package com.capstone.inventoryservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;

import java.time.Duration;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericToStringSerializer<>(Object.class));
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericToStringSerializer<>(Object.class));
        return template;
    }

    @Bean
    public DefaultRedisScript<Long> reserveScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("scripts/reserve.lua"));
        script.setResultType(Long.class);
        return script;
    }

    @Bean
    public StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamMessageListenerContainer(
            RedisConnectionFactory factory) {

        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions
                        .builder()
                        .pollTimeout(Duration.ofMillis(100))
                        .build();

        return StreamMessageListenerContainer.create(factory, options);
    }

    @Bean
    public org.springframework.data.redis.listener.RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory) {
        org.springframework.data.redis.listener.RedisMessageListenerContainer container = new org.springframework.data.redis.listener.RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        return container;
    }

    @Value("${spring.data.redis.url:redis://localhost:6379}")
    private String redisUrl;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        String host = "localhost";
        int port = 6379;
        String username = null;
        String password = null;
        boolean useSsl = false;

        if (redisUrl != null) {
            try {
                if (redisUrl.startsWith("rediss://")) {
                    useSsl = true;
                    redisUrl = redisUrl.substring(9);
                } else if (redisUrl.startsWith("redis://")) {
                    redisUrl = redisUrl.substring(8);
                }

                String[] authAndHost = redisUrl.split("@");

                if (authAndHost.length == 2) {
                    String[] credentials = authAndHost[0].split(":");
                    username = credentials[0];
                    password = credentials.length > 1 ? credentials[1] : null;

                    String[] hostPort = authAndHost[1].split(":");
                    host = hostPort[0];
                    if (hostPort.length > 1) {
                        port = Integer.parseInt(hostPort[1]);
                    }
                } else {
                    String[] hostPort = authAndHost[0].split(":");
                    host = hostPort[0];
                    if (hostPort.length > 1) {
                        port = Integer.parseInt(hostPort[1]);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Invalid Redis URL format: " + redisUrl, e);
            }
        }

        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(host, port);

        if (username != null) {
            config.setUsername(username);
        }
        if (password != null) {
            config.setPassword(password);
        }

        LettuceClientConfiguration.LettuceClientConfigurationBuilder builder =
                LettuceClientConfiguration.builder();

        if (useSsl) {
            builder.useSsl();
        }

        return new LettuceConnectionFactory(config, builder.build());
    }
}
