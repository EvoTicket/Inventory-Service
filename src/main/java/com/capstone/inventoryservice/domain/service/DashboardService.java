package com.capstone.inventoryservice.domain.service;

import com.capstone.inventoryservice.domain.client.IAMFeignClient;
import com.capstone.inventoryservice.domain.client.OrderFeignClient;
import com.capstone.inventoryservice.domain.client.PlatformStatsInternalResponse;
import com.capstone.inventoryservice.domain.dto.response.PlatformDashboardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final OrderFeignClient orderFeignClient;
    private final IAMFeignClient iamFeignClient;

    public PlatformDashboardResponse getPlatformDashboard(int days) {
        PlatformStatsInternalResponse stats = orderFeignClient.getPlatformStats(days);
        
        // Tính từ 00:00:00 của 'days' ngày trước
        LocalDateTime since = LocalDateTime.now().minusDays(days).withHour(0).withMinute(0).withSecond(0);
        long newUsers = iamFeignClient.getNewUsersCount(since.format(DateTimeFormatter.ISO_DATE_TIME));

        return PlatformDashboardResponse.builder()
                .totalGmv(stats.getTotalGmv())
                .totalRevenue(stats.getTotalRevenue())
                .totalTicketsSold(stats.getTotalTicketsSold())
                .newUsersCount(newUsers)
                .trend(stats.getTrend().stream()
                        .map(d -> PlatformDashboardResponse.DailyTrendDto.builder()
                                .date(d.getDate().toString())
                                .gmv(d.getGmv())
                                .ticketsSold(d.getTicketsSold())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}
