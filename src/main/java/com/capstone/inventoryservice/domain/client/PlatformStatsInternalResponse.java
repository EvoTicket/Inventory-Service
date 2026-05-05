package com.capstone.inventoryservice.domain.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformStatsInternalResponse {
    private BigDecimal totalGmv;
    private BigDecimal totalRevenue;
    private long totalTicketsSold;
    private List<DailyStatsInternalDto> trend;
}
