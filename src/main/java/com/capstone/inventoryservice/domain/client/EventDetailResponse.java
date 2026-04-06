package com.capstone.inventoryservice.domain.client;

import com.capstone.inventoryservice.model.entity.Event;
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

    public static EventDetailResponse toDto (Event event, String organizerName){
        return EventDetailResponse.builder()
                .eventName(event.getEventName())
                .eventStartTime(event.getEarliestStart())
                .eventEndTime(event.getLatestEnd())
                .venue(event.getVenue())
                .address(event.getFullAddress() == null ? "" : event.getAddress())
                .organizerName(organizerName)
                .build();
    }
}
