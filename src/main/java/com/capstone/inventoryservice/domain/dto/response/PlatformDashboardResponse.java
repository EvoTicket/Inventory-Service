package com.capstone.inventoryservice.domain.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class PlatformDashboardResponse {
    private BigDecimal totalGmv;
    private BigDecimal totalRevenue;
    private long totalTicketsSold;
    private long newUsersCount;
    private List<DailyTrendDto> trend;

    @Data
    @Builder
    public static class DailyTrendDto {
        private String date;
        private BigDecimal gmv;
        private long ticketsSold;
    }
}
