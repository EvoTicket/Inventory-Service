package com.capstone.inventoryservice.domain.client;

import com.capstone.inventoryservice.model.entity.Event;
import com.capstone.inventoryservice.model.entity.Showtime;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventDetailInternalResponse {
    String eventName;
    LocalDateTime eventStartTime;
    LocalDateTime eventEndTime;
    String venue;
    String address;
    String organizerName;
    ShowtimeDetail showtime;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ShowtimeDetail {
        Long showtimeId;
        LocalDateTime startDatetime;
        LocalDateTime endDatetime;
        String venue;
        String address;
        String fullAddress;

        public static ShowtimeDetail from(Showtime s) {
            return ShowtimeDetail.builder()
                    .showtimeId(s.getId())
                    .startDatetime(s.getStartDatetime())
                    .endDatetime(s.getEndDatetime())
                    .venue(s.getVenue())
                    .address(s.getAddress())
                    .fullAddress(s.getFullAddress())
                    .build();
        }
    }

    public static EventDetailInternalResponse toDto(Event event, Showtime showtime, String organizerName) {
        return EventDetailInternalResponse.builder()
                .eventName(event.getEventName())
                .eventStartTime(event.getEarliestStart())
                .eventEndTime(event.getLatestEnd())
                .venue(event.getVenue())
                .address(event.getFullAddress() == null ? "" : event.getAddress())
                .organizerName(organizerName)
                .showtime(ShowtimeDetail.from(showtime))
                .build();
    }
}
