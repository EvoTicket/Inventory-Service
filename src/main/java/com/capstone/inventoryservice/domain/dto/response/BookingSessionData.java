package com.capstone.inventoryservice.domain.dto.response;

import com.capstone.inventoryservice.domain.dto.request.ReserveRequest;
import com.capstone.inventoryservice.model.entity.TicketType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class BookingSessionData {
    private Long userId;
    private List<BookingItem> items;
    private String eventName;
    private LocalDateTime time;
    private String venue;
    private BigDecimal totalAmount;

    @Data
    public static class BookingItem {
        private Long ticketTypeId;
        private Integer qty;
        private BigDecimal price;
    }

    public static BookingSessionData fromReserveRequest(ReserveRequest request, Long userId, List<TicketType> ticketTypes) {
        BookingSessionData data = new BookingSessionData();

        data.setUserId(userId);

        data.setItems(request.getItems().stream().map(item -> {
            BookingItem bookingItem = new BookingItem();
            bookingItem.setTicketTypeId(item.getTicketTypeId());
            bookingItem.setQty(item.getQty());

            ticketTypes.stream()
                    .filter(tt -> tt.getId().equals(item.getTicketTypeId()))
                    .findFirst()
                    .ifPresent(tt -> bookingItem.setPrice(tt.getPrice()));
            
            return bookingItem;
        }).toList());

        if (!ticketTypes.isEmpty()) {
            TicketType firstTicketType = ticketTypes.getFirst();
            data.setEventName(firstTicketType.getShowtime().getEvent().getEventName());
            data.setTime(firstTicketType.getShowtime().getStartDatetime());
            data.setVenue(firstTicketType.getShowtime().getFullAddress());
        }

        data.setTotalAmount(data.getItems().stream()
                .filter(item -> item.getPrice() != null && item.getQty() != null)
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQty())))
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        return data;
    }
}
