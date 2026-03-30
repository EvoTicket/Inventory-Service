package com.capstone.inventoryservice.config;

import com.google.genai.types.FinishReason;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
public class FinishReasonWritingConverter implements Converter<FinishReason, String> {
    @Override
    public String convert(FinishReason source) {
        return source.toString();
    }
}