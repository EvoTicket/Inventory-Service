package com.capstone.inventoryservice.domain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import com.capstone.inventoryservice.model.enums.TicketAvailabilityStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrendingEventResponse {
    private Long id;
    private String eventName;
    private String thumbnailImage;
    private String organizerName;
    private BigDecimal floorPrice;
    private BigDecimal volume24h;
    private Double hotness;
    private TicketAvailabilityStatus ticketAvailabilityStatus;
}
