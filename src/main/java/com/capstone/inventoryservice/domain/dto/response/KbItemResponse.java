package com.capstone.inventoryservice.domain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KbItemResponse {
    private Long id;
    private String source;
    private String title;
    private String category;
    private String filename;
    private Integer chunkCount;
    private String status;
    private String updatedBy;
    private LocalDateTime updatedAt;
    private LocalDateTime createdAt;
}
