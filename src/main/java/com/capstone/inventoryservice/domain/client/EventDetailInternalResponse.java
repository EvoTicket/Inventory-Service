package com.capstone.inventoryservice.domain.client;

import com.capstone.inventoryservice.model.entity.Event;
import com.capstone.inventoryservice.model.entity.Showtime;
import lombok.*;

import java.math.BigDecimal;
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
    String category;
    Integer provinceCode;
    ShowtimeDetail showtime;
    BigDecimal maxResalePricePercentage;
    BigDecimal organizerRoyaltyFeePercentage;

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
        Integer provinceCode;

        public static ShowtimeDetail from(Showtime s) {
            return ShowtimeDetail.builder()
                    .showtimeId(s.getId())
                    .startDatetime(s.getStartDatetime())
                    .endDatetime(s.getEndDatetime())
                    .venue(s.getVenue())
                    .address(s.getAddress())
                    .fullAddress(s.getFullAddress())
                    .provinceCode(s.getProvince() != null ? s.getProvince().getCode() : null)
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
                .category(event.getCategory() != null ? event.getCategory().name() : null)
                .provinceCode(event.getProvince() != null ? event.getProvince().getCode() : null)
                .showtime(ShowtimeDetail.from(showtime))
                .maxResalePricePercentage(event.getMaxResalePricePercentage())
                .organizerRoyaltyFeePercentage(event.getOrganizerRoyaltyFeePercentage())
                .build();
    }
}
