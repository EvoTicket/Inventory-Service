package com.capstone.inventoryservice.domain.dto.response;

import com.capstone.inventoryservice.domain.dto.request.ReserveRequest;
import com.capstone.inventoryservice.model.entity.TicketType;
import lombok.Data;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Data
public class BookingSessionData {
    private Long userId;
    private List<BookingItem> items;
    private String eventName;
    private String time;
    private String venue;

    @Data
    public static class BookingItem {
        private Long ticketTypeId;
        private Integer qty;
    }

    public static BookingSessionData fromReserveRequest(ReserveRequest request, Long userId, TicketType ticketType) {
        BookingSessionData data = new BookingSessionData();

        data.setUserId(userId);

        data.setItems(request.getItems().stream().map(item -> {
            BookingItem bookingItem = new BookingItem();
            bookingItem.setTicketTypeId(item.getTicketTypeId());
            bookingItem.setQty(item.getQty());
            return bookingItem;
        }).toList());

        data.setEventName(ticketType.getShowtime().getEvent().getEventName());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(
                "HH:mm - EEEE, dd/MM/yyyy",
                Locale.forLanguageTag("vi-VN")
        );
        data.setTime(ticketType.getShowtime().getStartDatetime().format(formatter));

        data.setVenue(ticketType.getShowtime().getFullAddress());

        return data;
    }
}
