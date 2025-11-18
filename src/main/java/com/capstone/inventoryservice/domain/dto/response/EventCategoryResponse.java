package com.capstone.inventoryservice.domain.dto.response;

import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventCategoryResponse {
    private Long categoryId;
    private String categoryName;
    private String description;
    private String iconUrl;
}