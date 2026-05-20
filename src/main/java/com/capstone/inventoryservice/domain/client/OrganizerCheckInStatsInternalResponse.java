package com.capstone.inventoryservice.domain.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizerCheckInStatsInternalResponse {
    private Map<Long, Long> checkedInMap;
    private Map<Long, Long> totalTicketsMap;
}
