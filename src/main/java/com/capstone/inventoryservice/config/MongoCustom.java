package com.capstone.inventoryservice.config;

import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.util.MimeType;

@Configuration
public class MongoCustom {

    @Bean
    @Primary
    public MongoCustomConversions customConversions() {
        return MongoCustomConversions.create(adapter -> {
            adapter.registerConverter(new FinishReasonReadingConverter());
            adapter.registerConverter(new FinishReasonWritingConverter());
            adapter.registerConverter(new Converter<MimeType, String>() {
                @Override
                public String convert(@NonNull MimeType source) {
                    return source.toString();
                }
            });
            adapter.registerConverter(new Converter<String, MimeType>() {
                @Override
                public MimeType convert(@NonNull String source) {
                    return MimeType.valueOf(source);
                }
            });
        });
    }
}
