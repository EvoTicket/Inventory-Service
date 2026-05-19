package com.capstone.inventoryservice.domain.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckerEventResponse {
    private Long eventId;
    private String eventName;
    private String description;
    private String venue;
    private String address;
    private Long organizerId;
    private String bannerImage;
    private String thumbnailImage;
    private List<CheckerShowtimeResponse> showtimes;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CheckerShowtimeResponse {
        private Long showtimeId;
        private LocalDateTime startDatetime;
        private LocalDateTime endDatetime;
        private String venue;
        private String address;
        private String fullAddress;
        private String provinceName;
        private Boolean isCancelled;
    }
}
