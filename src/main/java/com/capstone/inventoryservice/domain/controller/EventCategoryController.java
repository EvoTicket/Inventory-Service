package com.capstone.inventoryservice.domain.controller;

import com.capstone.inventoryservice.domain.dto.BaseResponse;
import com.capstone.inventoryservice.domain.dto.request.CreateCategoryRequest;
import com.capstone.inventoryservice.domain.dto.request.UpdateCategoryRequest;
import com.capstone.inventoryservice.domain.dto.response.EventCategoryResponse;
import com.capstone.inventoryservice.domain.service.CategoryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Validated
public class EventCategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<BaseResponse<List<EventCategoryResponse>>> getAllCategories() {
        List<EventCategoryResponse> categories = categoryService.getAllCategories();
        return ResponseEntity.ok(BaseResponse.ok("Lấy danh sách danh mục thành công", categories));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<EventCategoryResponse>> getCategoryById(@PathVariable Long id) {
        EventCategoryResponse category = categoryService.getCategoryById(id);
        return ResponseEntity.ok(BaseResponse.ok("Lấy thông tin danh mục thành công", category));
    }

    @PostMapping
    public ResponseEntity<BaseResponse<EventCategoryResponse>> createCategory(
            @Valid @RequestBody CreateCategoryRequest categoryDTO) {
        EventCategoryResponse createdCategory = categoryService.createCategory(categoryDTO);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(BaseResponse.created("Tạo danh mục thành công", createdCategory));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BaseResponse<EventCategoryResponse>> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCategoryRequest categoryDTO) {
        EventCategoryResponse updatedCategory = categoryService.updateCategory(id, categoryDTO);
        return ResponseEntity.ok(BaseResponse.ok("Cập nhật danh mục thành công", updatedCategory));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponse<Boolean>> deleteCategory(@PathVariable Long id) {
        return ResponseEntity.ok(BaseResponse.ok("Xóa danh mục thành công", categoryService.deleteCategory(id)));
    }
}