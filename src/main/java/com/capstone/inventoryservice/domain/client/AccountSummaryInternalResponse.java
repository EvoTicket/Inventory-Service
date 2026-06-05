package com.capstone.inventoryservice.domain.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountSummaryInternalResponse {
    private long totalAccounts;
    private long activeOrganizers;
    private long pendingApprovals;
    private long restrictedAccounts;
}
