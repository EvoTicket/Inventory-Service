package com.capstone.inventoryservice.domain.dto.request;

import jakarta.validation.Valid;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateShowtimeRequest {
    private Long id;
    private LocalDateTime startDatetime;
    private LocalDateTime endDatetime;
    private String venue;
    private String address;
    private Integer wardCode;
    private Integer provinceCode;
    private Boolean isCancelled;

    @Valid
    private List<CreateTicketTypeRequest> ticketTypes;
}
