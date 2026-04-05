package com.capstone.inventoryservice.domain.dto.response;

import com.capstone.inventoryservice.model.entity.Event;
import com.capstone.inventoryservice.model.entity.Showtime;
import com.capstone.inventoryservice.model.entity.TicketType;
import com.capstone.inventoryservice.model.enums.EventStatus;
import com.capstone.inventoryservice.model.enums.EventType;
import com.capstone.inventoryservice.model.enums.EventCategory;
import com.capstone.inventoryservice.model.enums.TicketAvailabilityStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ListEventResponse {
    private Long id;
    private String eventName;
    private String description;
    private String venue;

    private String fullAddress;

    private LocalDateTime startDatetime;
    private LocalDateTime endDatetime;
    private EventStatus eventStatus;
    private EventType eventType;

    private String bannerImage;
    private String thumbnailImage;

    private Integer totalSeats;
    private Long organizerId;
    private Boolean isFeatured;

    private EventCategory category;

    private BigDecimal floorPrice;
    private String provinceName;
    private TicketAvailabilityStatus ticketAvailabilityStatus;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Boolean isExpired;

    private boolean isFavorite;
    private Long favoriteCount;

    public static ListEventResponse mapToResponse(Event event, Map<Long, Long> favoriteCountMap,  Set<Long> userFavoriteEventIds) {
        LocalDateTime now = LocalDateTime.now();

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

        boolean expired = latestEnd != null && latestEnd.isBefore(now);

        BigDecimal floorPriceCalc = BigDecimal.ZERO;
        if (event.getShowtimes() != null) {
            for (Showtime s : event.getShowtimes()) {
                if (Boolean.TRUE.equals(s.getIsCancelled())) continue;
                if (s.getTicketTypes() != null) {
                    for (TicketType t : s.getTicketTypes()) {
                        if (t.getPrice() != null && (floorPriceCalc.compareTo(BigDecimal.ZERO) == 0 || t.getPrice().compareTo(floorPriceCalc) < 0)) {
                            floorPriceCalc = t.getPrice();
                        }
                    }
                }
            }
        }
        
        String provinceNameCalc = event.getProvince() != null ? event.getProvince().getName() : null;

        int totalSold = 0;
        int totalCapacity = 0;
        if (event.getShowtimes() != null) {
            for (Showtime s : event.getShowtimes()) {
                if (Boolean.TRUE.equals(s.getIsCancelled())) continue;
                if (s.getTicketTypes() != null) {
                    for (TicketType t : s.getTicketTypes()) {
                        totalSold += t.getQuantitySold() != null ? t.getQuantitySold() : 0;
                        totalCapacity += t.getQuantityTotal() != null ? t.getQuantityTotal() : 0;
                    }
                }
            }
        }

        TicketAvailabilityStatus status = null;
        if (totalCapacity > 0) {
            if (totalSold >= totalCapacity) {
                status = TicketAvailabilityStatus.SOLD_OUT;
            } else if (totalSold * 10 >= totalCapacity * 9) {
                status = TicketAvailabilityStatus.ALMOST_SOLD_OUT;
            }
        }

        return ListEventResponse.builder()
                .id(event.getId())
                .eventName(event.getEventName())
                .description(event.getDescription())
                .venue(event.getVenue())
                .fullAddress(event.getFullAddress())
                .startDatetime(earliestStart)
                .endDatetime(latestEnd)
                .eventStatus(event.getEventStatus())
                .eventType(event.getEventType())
                .bannerImage(event.getBannerImage())
                .thumbnailImage(event.getThumbnailImage())
                .totalSeats(event.getTotalSeats())
                .organizerId(event.getOrganizerId())
                .isFeatured(event.getIsFeatured())
                .category(event.getCategory() != null ? event.getCategory() : null)
                .floorPrice(floorPriceCalc)
                .provinceName(provinceNameCalc)
                .ticketAvailabilityStatus(status)
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .isExpired(expired)
                .isFavorite(userFavoriteEventIds.contains(event.getId()))
                .favoriteCount(favoriteCountMap.getOrDefault(event.getId(), 0L))
                .build();
    }
}
