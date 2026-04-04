package com.capstone.inventoryservice.domain.controller;

import com.capstone.inventoryservice.domain.dto.BaseResponse;
import com.capstone.inventoryservice.model.enums.EventCategory;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/categories")
public class EventCategoryController {

    @GetMapping
    public ResponseEntity<BaseResponse<List<EventCategory>>> getAllCategories() {
        List<EventCategory> categories = Arrays.asList(EventCategory.values());
        return ResponseEntity.ok(BaseResponse.ok("Lấy danh sách danh mục thành công", categories));
    }
}