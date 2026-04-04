package com.capstone.inventoryservice.domain.dto.response;

import com.capstone.inventoryservice.model.entity.Event;
import com.capstone.inventoryservice.model.enums.EventStatus;
import com.capstone.inventoryservice.model.enums.EventType;
import com.capstone.inventoryservice.model.enums.EventCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.math.BigDecimal;
import com.capstone.inventoryservice.model.entity.TicketType;
import com.capstone.inventoryservice.model.enums.TicketAvailabilityStatus;
import java.util.Objects;

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
        boolean expired = event.getEndDatetime() != null && event.getEndDatetime().isBefore(now);

        BigDecimal floorPriceCalc = event.getTicketTypes() != null 
                ? event.getTicketTypes().stream()
                    .map(TicketType::getPrice)
                    .filter(Objects::nonNull)
                    .min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO)
                : BigDecimal.ZERO;
        
        String provinceNameCalc = event.getProvince() != null ? event.getProvince().getName() : null;

        TicketAvailabilityStatus status = null;
        if (event.getTicketTypes() != null && !event.getTicketTypes().isEmpty()) {
            int totalSold = event.getTicketTypes().stream().mapToInt(t -> t.getQuantitySold() != null ? t.getQuantitySold() : 0).sum();
            int totalCapacity = event.getTicketTypes().stream().mapToInt(t -> t.getQuantityTotal() != null ? t.getQuantityTotal() : 0).sum();
            
            if (totalCapacity > 0) {
                if (totalSold >= totalCapacity) {
                    status = TicketAvailabilityStatus.SOLD_OUT;
                } else if (totalSold * 10 >= totalCapacity * 9) {
                    status = TicketAvailabilityStatus.ALMOST_SOLD_OUT;
                }
            }
        }

        return ListEventResponse.builder()
                .id(event.getId())
                .eventName(event.getEventName())
                .description(event.getDescription())
                .venue(event.getVenue())
                .fullAddress(event.getFullAddress())
                .startDatetime(event.getStartDatetime())
                .endDatetime(event.getEndDatetime())
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