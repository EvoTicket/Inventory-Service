package com.capstone.inventoryservice.domain.dto.request;

import com.capstone.inventoryservice.model.enums.EventStatus;
import com.capstone.inventoryservice.model.enums.EventType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateEventRequest {

    @NotBlank(message = "Event name is required")
    @Size(max = 255, message = "Event name must not exceed 255 characters")
    private String eventName;

    @NotBlank(message = "Description is required")
    private String description;

    @NotBlank(message = "Venue is required")
    private String venue;

    private Integer wardCode;

    private Integer provinceCode;

    private String address;

    @NotNull(message = "Start datetime is required")
    @Future(message = "Start datetime must be in the future")
    private OffsetDateTime startDatetime;

    @NotNull(message = "End datetime is required")
    private OffsetDateTime endDatetime;

    @NotNull(message = "Event status is required")
    private EventStatus eventStatus;

    @NotNull(message = "Event type is required")
    private EventType eventType;

    @NotNull(message = "Total seats is required")
    @Min(value = 1, message = "Total seats must be at least 1")
    private Integer totalSeats;

    private Boolean isFeatured;

    private BigDecimal latitude;

    private BigDecimal longitude;

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    private List<CreateTicketTypeRequest> ticketTypes;
}