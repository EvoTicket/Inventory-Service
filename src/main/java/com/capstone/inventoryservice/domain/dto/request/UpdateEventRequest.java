package com.capstone.inventoryservice.domain.dto.request;

import com.capstone.inventoryservice.model.enums.EventStatus;
import com.capstone.inventoryservice.model.enums.EventType;
import lombok.*;
import java.time.OffsetDateTime;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateEventRequest {
    private String eventName;
    private String description;
    private String venue;
    private String address;
    private OffsetDateTime startDatetime;
    private OffsetDateTime endDatetime;
    private EventStatus eventStatus;
    private EventType eventType;
    private String bannerImage;
    private String thumbnailImage;
    private Integer totalSeats;
    private Boolean isFeatured;
    private Long categoryId;
}

