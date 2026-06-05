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

    // Finance & Settlement
    private BigDecimal payoutPendingVolume;
    private int payoutPendingOrgs;
    private String payoutPendingBatch;
    private BigDecimal payoutSettledVolume;
    private int payoutSettledBatches;
    private int disputesCount;
    private String disputesMessage;

    // Governance & Review
    private long organizationsPendingApproval;
    private String organizationsPendingDetails;
    private long eventsPendingApproval;
    private String eventsPendingDetails;
    private long restrictedAccounts;
    private String restrictedAccountsDetails;
    private int highRiskEventsCount;

    @Data
    @Builder
    public static class DailyTrendDto {
        private String date;
        private BigDecimal gmv;
        private long ticketsSold;
    }
}
