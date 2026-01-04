package com.capstone.inventoryservice.domain.dto.request;

import com.capstone.inventoryservice.model.enums.EventStatus;
import com.capstone.inventoryservice.model.enums.EventType;
import lombok.*;

import java.math.BigDecimal;
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
    private Integer totalSeats;
    private Boolean isFeatured;
    private Long categoryId;
    private BigDecimal latitude;
    private BigDecimal longitude;
}

