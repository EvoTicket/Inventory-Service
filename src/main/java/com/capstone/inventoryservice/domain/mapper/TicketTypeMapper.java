package com.capstone.inventoryservice.domain.mapper;

import com.capstone.inventoryservice.domain.dto.response.TicketTypeResponse;
import com.capstone.inventoryservice.model.entity.TicketType;
import org.springframework.stereotype.Component;

@Component
public class TicketTypeMapper {

    public TicketTypeResponse convertToDTO(TicketType ticketType) {
        return TicketTypeResponse.builder()
                .ticketTypeId(ticketType.getId())
                .typeName(ticketType.getTypeName())
                .description(ticketType.getDescription())
                .price(ticketType.getPrice())
                .quantityAvailable(ticketType.getQuantityTotal())
                .quantitySold(ticketType.getQuantitySold())
                .minPurchase(ticketType.getMinPurchase())
                .maxPurchase(ticketType.getMaxPurchase())
                .saleStartDate(ticketType.getSaleStartDate())
                .saleEndDate(ticketType.getSaleEndDate())
                .ticketTypeStatus(ticketType.getTicketTypeStatus())
                .showtimeId(ticketType.getShowtime().getId())
                .eventId(ticketType.getShowtime().getEvent().getId())
                .eventName(ticketType.getShowtime().getEvent().getEventName())
                .build();
    }
}
