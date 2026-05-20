package com.capstone.inventoryservice.domain.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizerOrdersStatsInternalResponse {
    private Map<Long, Long> resaleVolumeMap;
    private Map<Long, BigDecimal> royaltyFeeMap;
    private Map<String, BigDecimal> dailyRevenueMap;
}
