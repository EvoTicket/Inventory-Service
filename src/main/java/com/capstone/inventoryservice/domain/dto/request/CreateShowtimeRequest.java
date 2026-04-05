package com.capstone.inventoryservice.domain.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateShowtimeRequest {

    @NotNull(message = "Start datetime is required")
    private LocalDateTime startDatetime;

    @NotNull(message = "End datetime is required")
    private LocalDateTime endDatetime;

    private String venue;

    private String address;

    private Integer wardCode;

    private Integer provinceCode;

    @Valid
    private List<CreateTicketTypeRequest> ticketTypes;
}
