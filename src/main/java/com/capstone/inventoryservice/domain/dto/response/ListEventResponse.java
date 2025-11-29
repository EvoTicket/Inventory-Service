package com.capstone.inventoryservice.domain.dto.response;

import com.capstone.inventoryservice.model.entity.Event;
import com.capstone.inventoryservice.model.enums.EventStatus;
import com.capstone.inventoryservice.model.enums.EventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

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

    private OffsetDateTime startDatetime;
    private OffsetDateTime endDatetime;
    private EventStatus eventStatus;
    private EventType eventType;

    private String bannerImage;
    private String thumbnailImage;

    private Integer totalSeats;
    private Long organizerId;
    private Boolean isFeatured;

    private Long categoryId;
    private String categoryName;
    private String categoryIconUrl;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    private Boolean isExpired;

    private boolean isFavorite;
    private Long favoriteCount;

    public static ListEventResponse fromEntity(Event event) {
        OffsetDateTime now = OffsetDateTime.now();
        boolean expired = event.getEndDatetime() != null && event.getEndDatetime().isBefore(now);

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
                .categoryId(event.getCategory() != null ? event.getCategory().getId() : null)
                .categoryName(event.getCategory() != null ? event.getCategory().getCategoryName() : null)
                .categoryIconUrl(event.getCategory() != null ? event.getCategory().getIconUrl() : null)
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .isExpired(expired)
                .isFavorite(false)
                .favoriteCount(0L)
                .build();
    }
}