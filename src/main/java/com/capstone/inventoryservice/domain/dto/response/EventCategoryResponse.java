package com.capstone.inventoryservice.domain.dto.response;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventCategoryResponse {
    private Long id;

    private String categoryName;

    private String description;

    private String iconUrl;

    private Integer eventCount;
}