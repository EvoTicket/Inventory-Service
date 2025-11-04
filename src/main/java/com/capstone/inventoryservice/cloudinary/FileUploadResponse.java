package com.capstone.inventoryservice.cloudinary;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponse {
    private String publicId;
    private String url;
    private String secureUrl;
    private String format;
    private Long size;
    private Integer width;
    private Integer height;
    private String resourceType;
}
