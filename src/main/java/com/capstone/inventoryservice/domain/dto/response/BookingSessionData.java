package com.capstone.inventoryservice.domain.dto.response;

import com.capstone.inventoryservice.domain.dto.request.ReserveRequest;
import lombok.Data;

import java.util.List;

@Data
public class BookingSessionData {
    private Long userId;
    private List<BookingItem> items;

    @Data
    public static class BookingItem {
        private Long ticketTypeId;
        private Integer qty;
    }

    public static BookingSessionData fromReserveRequest(ReserveRequest request, Long userId) {
        BookingSessionData data = new BookingSessionData();

        data.setUserId(userId);

        data.setItems(request.getItems().stream().map(item -> {
            BookingItem bookingItem = new BookingItem();
            bookingItem.setTicketTypeId(item.getTicketTypeId());
            bookingItem.setQty(item.getQty());
            return bookingItem;
        }).toList());

        return data;
    }
}
