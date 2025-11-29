package com.capstone.inventoryservice.domain.dto.response;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponse {
    private Long id;
    private Long eventId;
    private Long userId;
    private Integer rating;
    private String comment;
    private List<String> images;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
