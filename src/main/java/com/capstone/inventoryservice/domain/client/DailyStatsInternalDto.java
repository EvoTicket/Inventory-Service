package com.capstone.inventoryservice.domain.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyStatsInternalDto {
    private LocalDate date;
    private BigDecimal gmv;
    private long ticketsSold;
}
