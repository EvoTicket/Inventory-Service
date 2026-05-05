package com.capstone.inventoryservice.domain.dto.response;

import com.capstone.inventoryservice.domain.dto.BasePageResponse;
import com.capstone.inventoryservice.model.enums.EventApprovalStatus;
import com.capstone.inventoryservice.model.enums.EventCategory;
import com.capstone.inventoryservice.model.enums.EventStatus;
import com.capstone.inventoryservice.model.enums.EventType;
import lombok.*;
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
    }
}
