package com.capstone.inventoryservice.domain.dto.response;

import lombok.*;

import java.time.LocalDateTime;

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
    private LocalDateTime eventStartDate;
    private LocalDateTime eventEndDate;
    private LocalDateTime likedAt;
    private String eventBannerImage;
    private String eventVenue;
    private String eventAddress;
    private java.math.BigDecimal minPrice;
}
