package com.capstone.inventoryservice.domain.dto.response;

import com.capstone.inventoryservice.model.entity.Event;
import com.capstone.inventoryservice.model.enums.EventStatus;
import com.capstone.inventoryservice.model.enums.EventType;
import com.capstone.inventoryservice.model.enums.EventApprovalStatus;
import com.capstone.inventoryservice.model.enums.EventCategory;
import com.capstone.inventoryservice.model.enums.TicketAvailabilityStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
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
    private EventApprovalStatus approvalStatus;
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

        boolean expired = event.getLatestEnd() != null && event.getLatestEnd().isBefore(now);

        String provinceName = event.getProvince() != null ? event.getProvince().getName() : null;

        return ListEventResponse.builder()
                .id(event.getId())
                .eventName(event.getEventName())
                .description(event.getDescription())
                .venue(event.getVenue())
                .fullAddress(event.getFullAddress())
                .startDatetime(event.getEarliestStart())
                .endDatetime(event.getLatestEnd())
                .eventStatus(event.getEventStatus())
                .approvalStatus(event.getApprovalStatus())
                .eventType(event.getEventType())
                .bannerImage(event.getBannerImage())
                .thumbnailImage(event.getThumbnailImage())
                .totalSeats(event.getTotalSeats())
                .organizerId(event.getOrganizerId())
                .isFeatured(event.getIsFeatured())
                .category(event.getCategory() != null ? event.getCategory() : null)
                .floorPrice(event.getFloorPrice())
                .provinceName(provinceName)
                .ticketAvailabilityStatus(event.getTicketAvailabilityStatus())
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .isExpired(expired)
                .isFavorite(userFavoriteEventIds.contains(event.getId()))
                .favoriteCount(favoriteCountMap.getOrDefault(event.getId(), 0L))
                .build();
    }
}
