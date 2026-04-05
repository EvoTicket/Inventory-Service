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
public class EventDetailResponse {
    String eventName;
    LocalDateTime eventStartTime;
    LocalDateTime eventEndTime;
    String venue;
    String address;
    String organizerName;

    static public EventDetailResponse toDto (Event event, String organizerName){
        LocalDateTime earliestStart = null;
        LocalDateTime latestEnd = null;
        if (event.getShowtimes() != null) {
            for (Showtime s : event.getShowtimes()) {
                if (Boolean.TRUE.equals(s.getIsCancelled())) continue;
                if (s.getStartDatetime() != null && (earliestStart == null || s.getStartDatetime().isBefore(earliestStart))) {
                    earliestStart = s.getStartDatetime();
                }
                if (s.getEndDatetime() != null && (latestEnd == null || s.getEndDatetime().isAfter(latestEnd))) {
                    latestEnd = s.getEndDatetime();
                }
            }
        }

        return EventDetailResponse.builder()
                .eventName(event.getEventName())
                .eventStartTime(earliestStart)
                .eventEndTime(latestEnd)
                .venue(event.getVenue())
                .address(event.getFullAddress() == null ? "" : event.getAddress())
                .organizerName(organizerName)
                .build();
    }
}
