package com.capstone.inventoryservice.domain.dto.response;

import lombok.*;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserFavoriteEventResponse {
    private Long id;
    private Long userId;
    private Long eventId;
    private String eventName;
    private String eventDescription;
    private OffsetDateTime eventStartDate;
    private OffsetDateTime eventEndDate;
    private OffsetDateTime likedAt;
}
