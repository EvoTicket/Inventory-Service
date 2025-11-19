package com.capstone.inventoryservice.domain.service;

import com.capstone.inventoryservice.domain.dto.request.CreateCategoryRequest;
import com.capstone.inventoryservice.domain.dto.request.UpdateCategoryRequest;
import com.capstone.inventoryservice.domain.dto.response.EventCategoryResponse;
import com.capstone.inventoryservice.domain.util.CategoryUtil;
import com.capstone.inventoryservice.exception.AppException;
import com.capstone.inventoryservice.exception.ErrorCode;
import com.capstone.inventoryservice.model.entity.EventCategory;
import com.capstone.inventoryservice.model.repository.EventCategoryRepository;
import com.cloudinary.Cloudinary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final EventCategoryRepository categoryRepository;
    private final CategoryUtil categoryUtil;
    private final Cloudinary cloudinary;
    public List<EventCategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::convertToDTO)
                .toList();
    }

    public EventCategoryResponse getCategoryById(Long id) {
        EventCategory category = categoryUtil.getCategoryOrElseThrow(id);
        return convertToDTO(category);
    }

    @Transactional
    public EventCategoryResponse createCategory(CreateCategoryRequest dto) {
        if (categoryRepository.existsByCategoryName(dto.getCategoryName())) {
            throw new AppException(ErrorCode.CONFLICT, "Tên danh mục '" + dto.getCategoryName() + "' đã tồn tại");
        }

        EventCategory category = EventCategory.builder()
                .categoryName(dto.getCategoryName())
                .description(dto.getDescription())
                .build();

        EventCategory savedCategory = categoryRepository.save(category);
        return convertToDTO(savedCategory);
    }

    @Transactional
    public EventCategoryResponse updateCategory(Long id, UpdateCategoryRequest dto) {
        EventCategory category = categoryUtil.getCategoryOrElseThrow(id);

        if (categoryRepository.existsByCategoryNameAndIdNot(dto.getCategoryName(), id)) {
            throw new AppException(ErrorCode.CONFLICT, "Tên danh mục '" + dto.getCategoryName() + "' đã tồn tại");
        }

        if(dto.getCategoryName() != null) {
            category.setCategoryName(dto.getCategoryName());
        }
        if(dto.getDescription() != null) {
            category.setDescription(dto.getDescription());
        }

        EventCategory updatedCategory = categoryRepository.save(category);
        return convertToDTO(updatedCategory);
    }

    @Transactional
    public boolean deleteCategory(Long id) {
        EventCategory category = categoryUtil.getCategoryOrElseThrow(id);


        long eventCount = categoryRepository.countEventsByCategoryId(id);
        if (eventCount > 0) {
            throw new IllegalStateException("Không thể xóa danh mục vì còn " + eventCount + " sự kiện đang sử dụng");
        }

        categoryRepository.delete(category);
        return true;
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public String uploadIconUrl(MultipartFile file, Long categoryId) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("File phải là ảnh");
        }

        String folder = "category/" + categoryId + "/";

        String publicId = UUID.randomUUID().toString();

        Map<String, Object> options = new HashMap<>();
        options.put("resource_type", "image");
        options.put("folder",  folder);
        options.put("public_id", publicId);
        options.put("overwrite", true);

        try {
            Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(), options);
            EventCategory category = categoryUtil.getCategoryOrElseThrow(categoryId);
            category.setIconUrl(uploadResult.get("url").toString());
            return uploadResult.get("url").toString();
        } catch (IOException e) {
            throw new AppException(ErrorCode.IO_EXCEPTION, "Không thể tải ảnh lên Cloudinary: " + e.getMessage());
        }
    }

    private EventCategoryResponse convertToDTO(EventCategory category) {
        int eventCount = category.getEvents() != null ? category.getEvents().size() : 0;

        return EventCategoryResponse.builder()
                .id(category.getId())
                .categoryName(category.getCategoryName())
                .description(category.getDescription())
                .iconUrl(category.getIconUrl())
                .eventCount(eventCount)
                .build();
    }
}
