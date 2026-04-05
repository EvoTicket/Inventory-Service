package com.capstone.inventoryservice.domain.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShowtimeResponse {
    private Long showtimeId;
    private LocalDateTime startDatetime;
    private LocalDateTime endDatetime;
    private String venue;
    private String address;
    private String fullAddress;
    private String provinceName;
    private Boolean isCancelled;
    private List<TicketTypeResponse> ticketTypes;
}
