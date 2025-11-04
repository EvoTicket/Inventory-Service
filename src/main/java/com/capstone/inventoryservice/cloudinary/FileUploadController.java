package com.capstone.inventoryservice.cloudinary;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class FileUploadController {

    private final CloudinaryService cloudinaryService;

    @PostMapping(value = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileUploadResponse> uploadFile(
            @Parameter(
                    description = "File ảnh đại diện",
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)
            )
            @RequestPart("file") MultipartFile file,

            @RequestParam(value = "folder", required = false) String folder) {
        try {
            Map<String, Object> uploadResult = cloudinaryService.uploadFile(file, folder);

            FileUploadResponse response = FileUploadResponse.builder()
                    .publicId(uploadResult.get("public_id").toString())
                    .url(uploadResult.get("url").toString())
                    .secureUrl(uploadResult.get("secure_url").toString())
                    .format(uploadResult.get("format").toString())
                    .size(Long.parseLong(uploadResult.get("bytes").toString()))
                    .resourceType(uploadResult.get("resource_type").toString())
                    .build();

            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadImage(
            @Parameter(
                    description = "File ảnh đại diện",
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)
            )
            @RequestPart("file") MultipartFile file,

            @RequestParam(value = "folder", required = false) String folder) {
        try {
            Map<String, Object> uploadResult = cloudinaryService.uploadImage(file, folder);

            FileUploadResponse response = FileUploadResponse.builder()
                    .publicId(uploadResult.get("public_id").toString())
                    .url(uploadResult.get("url").toString())
                    .secureUrl(uploadResult.get("secure_url").toString())
                    .format(uploadResult.get("format").toString())
                    .size(Long.parseLong(uploadResult.get("bytes").toString()))
                    .width((Integer) uploadResult.get("width"))
                    .height((Integer) uploadResult.get("height"))
                    .resourceType(uploadResult.get("resource_type").toString())
                    .build();

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{publicId}")
    public ResponseEntity<String> deleteFile(@PathVariable String publicId) {
        try {
            String formattedPublicId = publicId.replace(":", "/");
            String result = cloudinaryService.deleteFile(formattedPublicId);
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Không thể xóa file");
        }
    }
}
