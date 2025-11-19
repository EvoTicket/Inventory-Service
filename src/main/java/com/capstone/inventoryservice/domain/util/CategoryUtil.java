package com.capstone.inventoryservice.domain.util;

import com.capstone.inventoryservice.exception.AppException;
import com.capstone.inventoryservice.exception.ErrorCode;
import com.capstone.inventoryservice.model.entity.EventCategory;
import com.capstone.inventoryservice.model.repository.EventCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CategoryUtil {
    private final EventCategoryRepository eventCategoryRepository;

    public EventCategory getCategoryOrElseThrow(Long id) {
        return eventCategoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy danh mục với ID: " + id));
    }
}
