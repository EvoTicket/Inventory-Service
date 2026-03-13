package com.capstone.inventoryservice.domain.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TicketCreatedEvent {
    private Long ticketTypeId;
    private long quantity;
}