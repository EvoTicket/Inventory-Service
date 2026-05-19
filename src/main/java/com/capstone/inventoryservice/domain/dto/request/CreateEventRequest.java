package com.capstone.inventoryservice.domain.dto.request;

import com.capstone.inventoryservice.model.enums.EventType;
import com.capstone.inventoryservice.model.enums.EventCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
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

    @NotNull(message = "Event type is required")
    private EventType eventType;

    @NotNull(message = "Total seats is required")
    @Min(value = 1, message = "Total seats must be at least 1")
    private Integer totalSeats;

    private Boolean isFeatured;

    private BigDecimal latitude;

    private BigDecimal longitude;

    private String introduction;

    @NotNull(message = "Bank info is required")
    private Long bankInfoId;

    @NotNull(message = "Category is required")
    private EventCategory category;

    @Valid
    @NotEmpty(message = "At least one showtime is required")
    private List<CreateShowtimeRequest> showtimes;
}
