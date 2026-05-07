package com.capstone.inventoryservice.domain.dto.response;

import com.capstone.inventoryservice.domain.dto.BasePageResponse;
import com.capstone.inventoryservice.model.entity.Event;
import com.capstone.inventoryservice.model.enums.EventApprovalStatus;
import com.capstone.inventoryservice.model.enums.EventCategory;
import com.capstone.inventoryservice.model.enums.EventStatus;
import com.capstone.inventoryservice.model.enums.EventType;
import lombok.*;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrgEventDto {
    private long totalEvents;
    private long totalOnSales;
    private long totalPending;
    private long totalCompleted;
    private BasePageResponse<EventResponseDto> events;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventResponseDto {
        private Long id;

        // Basic info
        private String name;
        private String thumbnailUrl;
        private EventCategory category;
        private EventType type;

        // Time & location
        private LocalDateTime startTime;
        private String venue;

        // Status
        private EventStatus status;
        private EventApprovalStatus approvalStatus;

        // Ticket info
        private Long soldTickets;
        private Long totalTickets;

        // Revenue
        private BigDecimal revenue;

        // Checker
        private Integer totalCheckers;

        // Metadata
        private LocalDateTime updatedAt;

        public static EventResponseDto fromEntity(Event event, BigDecimal revenue) {
            return EventResponseDto.builder()
                    .id(event.getId())
                    .name(event.getEventName())
                    .thumbnailUrl(event.getThumbnailImage())
                    .category(event.getCategory())
                    .type(event.getEventType())
                    .startTime(event.getEarliestStart())
                    .venue(event.getFullAddress())
                    .status(event.getEventStatus())
                    .approvalStatus(event.getApprovalStatus())
                    .soldTickets(event.getTotalQuantitySold() != null ? event.getTotalQuantitySold().longValue() : 0L)
                    .totalTickets(event.getTotalQuantityTotal() != null ? event.getTotalQuantityTotal().longValue() : 0L)
                    .revenue(revenue != null ? revenue : BigDecimal.ZERO)
                    .totalCheckers(event.getCheckers() != null ? event.getCheckers().intValue() : 0)
                    .updatedAt(event.getUpdatedAt())
                    .build();
        }
    }
}
