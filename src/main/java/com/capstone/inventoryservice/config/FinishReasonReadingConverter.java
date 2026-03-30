package com.capstone.inventoryservice.config;

import com.google.genai.types.FinishReason;
import org.jspecify.annotations.NonNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public class FinishReasonReadingConverter implements Converter<String, FinishReason> {
    @Override
    public FinishReason convert(@NonNull String source) {
        return new FinishReason(source);
    }
}