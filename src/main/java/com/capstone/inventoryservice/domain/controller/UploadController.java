package com.capstone.inventoryservice.domain.controller;

import com.capstone.inventoryservice.domain.dto.BaseResponse;
import com.capstone.inventoryservice.domain.service.CategoryService;
import com.capstone.inventoryservice.domain.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/upload")
public class UploadController {
    private final EventService eventService;
    private final CategoryService categoryService;

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload avatar")
    public ResponseEntity<BaseResponse<String>> uploadUserAvatar(
            @Parameter(
                    description = "File ảnh",
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)
            )
            @RequestPart("file") MultipartFile file,

            @RequestParam(required = false)
            @Parameter(description = "ID của event, bắt buộc khi type = 'thumbnail' hoặc 'banner'")
            Long eventId,

            @RequestParam(required = false)
            @Parameter(description = "ID của category, bắt buộc khi type = 'category'")
            Long categoryId,

            @RequestParam
            @Parameter(
                    description = "Loại upload, có thể là: 'thumbnail', 'banner', 'category'",
                    example = "thumbnail",
                    required = true,
                    schema = @Schema(type = "string", allowableValues = {"thumbnail", "banner", "category"})
            )
            String type
    ) {
        String result;
        switch (type) {
            case "thumbnail", "banner" -> {
                if (eventId == null) {
                    return ResponseEntity.badRequest()
                            .body(BaseResponse.badRequest("eventId phải khác null với type " + type));
                }
                result = eventService.uploadEventImage(file, eventId, type);
            }

            case "category" -> {
                if (categoryId == null) {
                    return ResponseEntity.badRequest()
                            .body(BaseResponse.badRequest("categoryId phải khác null với type " + type));
                }
                result = categoryService.uploadIconUrl(file, categoryId);
            }

            default -> {
                return ResponseEntity.badRequest()
                        .body(BaseResponse.badRequest("type: " + type + " không đúng"));
            }
        }
        return ResponseEntity.ok(BaseResponse.ok(result));
    }
}
