package com.capstone.inventoryservice.config;

import com.capstone.inventoryservice.security.JwtUtil;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class FeignClientConfig {
    private final JwtUtil jwtUtil;

    public FeignClientConfig(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> requestTemplate.header("Authorization", "Bearer " + jwtUtil.getToken());
    }
}