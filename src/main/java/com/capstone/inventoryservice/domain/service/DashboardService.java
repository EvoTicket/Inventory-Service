package com.capstone.inventoryservice.domain.service;

import com.capstone.inventoryservice.domain.client.IAMFeignClient;
import com.capstone.inventoryservice.domain.client.OrderFeignClient;
import com.capstone.inventoryservice.domain.client.PlatformStatsInternalResponse;
import com.capstone.inventoryservice.domain.dto.response.PlatformDashboardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;
import java.util.List;
import java.util.ArrayList;
import java.math.BigDecimal;
import com.capstone.inventoryservice.domain.dto.response.OrganizerDashboardResponse;
import com.capstone.inventoryservice.model.repository.EventRepository;
import com.capstone.inventoryservice.security.JwtUtil;
import com.capstone.inventoryservice.model.entity.Event;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {
    private final OrderFeignClient orderFeignClient;
    private final IAMFeignClient iamFeignClient;
    private final EventRepository eventRepository;
    private final JwtUtil jwtUtil;

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

    public OrganizerDashboardResponse getOrganizerDashboard(int days) {
        Long orgId = jwtUtil.getDataFromAuth().organizationId();
        List<Event> events = eventRepository.findByOrganizerId(orgId);

        // Mocking Data for Organizer Dashboard since real data requires complex aggregations across multiple microservices
        List<OrganizerDashboardResponse.DailyRevenueDto> revenueTrend = new ArrayList<>();
        for (int i = 1; i <= days; i++) {
            revenueTrend.add(OrganizerDashboardResponse.DailyRevenueDto.builder()
                    .day(i)
                    .revenue(BigDecimal.valueOf(Math.floor(Math.random() * 200) + 100 + (i * 5) + (Math.sin(i / 3.0) * 50)))
                    .build());
        }

        List<OrganizerDashboardResponse.OccupancyByCategoryDto> occupancyByCategory = List.of(
                OrganizerDashboardResponse.OccupancyByCategoryDto.builder().name("Livestage").value(82).color("#8b5cf6").build(),
                OrganizerDashboardResponse.OccupancyByCategoryDto.builder().name("Hội thảo").value(50).color("#f59e0b").build(),
                OrganizerDashboardResponse.OccupancyByCategoryDto.builder().name("Triển lãm").value(71).color("#6366f1").build(),
                OrganizerDashboardResponse.OccupancyByCategoryDto.builder().name("Online").value(47).color("#10b981").build()
        );

        List<OrganizerDashboardResponse.TicketSalesByEventDto> ticketSalesByEvent = List.of(
                OrganizerDashboardResponse.TicketSalesByEventDto.builder().name("Anh Trai Say Hi").tickets(4280).build(),
                OrganizerDashboardResponse.TicketSalesByEventDto.builder().name("Tech Summit VN").tickets(3020).build(),
                OrganizerDashboardResponse.TicketSalesByEventDto.builder().name("Indie Night").tickets(2140).build(),
                OrganizerDashboardResponse.TicketSalesByEventDto.builder().name("Creative Expo").tickets(1320).build(),
                OrganizerDashboardResponse.TicketSalesByEventDto.builder().name("Startup Finance").tickets(620).build()
        );

        List<Long> eventIds = events.stream().map(Event::getId).collect(Collectors.toList());
        java.util.Map<Long, BigDecimal> revenueMap = java.util.Collections.emptyMap();
        try {
            if (!eventIds.isEmpty()) {
                revenueMap = orderFeignClient.getRevenueForEvents(eventIds);
            }
        } catch (Exception e) {
            // Ignore error from order-service
        }

        long totalSoldAll = 0;
        BigDecimal totalRevenueAll = BigDecimal.ZERO;
        long totalCapacityAll = 0;

        List<OrganizerDashboardResponse.EventPerformanceDto> performanceTable = new ArrayList<>();
        if (events.isEmpty()) {
            performanceTable = List.of(
                    OrganizerDashboardResponse.EventPerformanceDto.builder()
                            .name("Anh Trai Say Hi Concert 2026").type("Livestage").sold("4,280").occupancy("95%").revenue("5.4 tỷ").checkin("-").resale("214").royalty("18.2 triệu").status("On sale").build(),
                    OrganizerDashboardResponse.EventPerformanceDto.builder()
                            .name("Tech Summit VN 2026").type("Hội thảo").sold("3,020").occupancy("82%").revenue("1.9 tỷ").checkin("76%").resale("64").royalty("3.4 triệu").status("On sale").build()
            );
        } else {
            for (Event event : events) {
                int sold = event.getTotalSold();
                int capacity = event.getTotalCapacity();
                totalSoldAll += sold;
                totalCapacityAll += capacity;

                long occ = capacity > 0 ? (sold * 100L / capacity) : 0;
                
                BigDecimal rev = revenueMap.getOrDefault(event.getId(), BigDecimal.ZERO);
                totalRevenueAll = totalRevenueAll.add(rev);

                String revStr;
                if (rev.compareTo(new BigDecimal("1000000000")) >= 0) {
                    revStr = String.format("%.1f tỷ", rev.doubleValue() / 1000000000.0);
                } else if (rev.compareTo(new BigDecimal("1000000")) >= 0) {
                    revStr = String.format("%.1f triệu", rev.doubleValue() / 1000000.0);
                } else {
                    revStr = String.format("%,d VNĐ", rev.longValue());
                }

                performanceTable.add(OrganizerDashboardResponse.EventPerformanceDto.builder()
                        .name(event.getEventName())
                        .type(event.getCategory() != null ? event.getCategory().name() : "N/A")
                        .sold(String.format("%,d", sold))
                        .occupancy(occ + "%")
                        .revenue(revStr)
                        .checkin("-") // Mock
                        .resale("-") // Mock
                        .royalty("-") // Mock
                        .status(event.getEventStatus() != null ? event.getEventStatus().name() : "UNKNOWN")
                        .build());
            }
        }
        
        double avgOcc = totalCapacityAll > 0 ? (double) totalSoldAll * 100 / totalCapacityAll : 0.0;

        return OrganizerDashboardResponse.builder()
                .totalRevenue(events.isEmpty() ? new BigDecimal("8760000000") : totalRevenueAll)
                .totalTicketsSold(events.isEmpty() ? 12480 : totalSoldAll)
                .avgOccupancyRate(events.isEmpty() ? 71.0 : Math.round(avgOcc * 10.0) / 10.0)
                .avgCheckInRate(64.0) // Mock
                .resaleVolume(428) // Mock
                .royaltyFee(new BigDecimal("42500000")) // Mock
                .revenueTrend(revenueTrend)
                .occupancyByCategory(occupancyByCategory)
                .ticketSalesByEvent(ticketSalesByEvent)
                .checkInStatus(OrganizerDashboardResponse.CheckInStatusDto.builder()
                        .checkedIn(7986)
                        .notCheckedIn(4494)
                        .absentRate(36.0)
                        .peakGateTime("19:20")
                        .build())
                .performanceTable(performanceTable)
                .build();
    }

    public byte[] exportOrganizerDashboard(String format, String scope, int days) {
        // Since we don't have Apache POI/iText dependencies, we will just return a CSV string as bytes for all formats right now for demonstration.
        // In a real application, you would use a library to generate the XLSX or PDF.
        
        OrganizerDashboardResponse dashboard = getOrganizerDashboard(days);
        
        StringBuilder csv = new StringBuilder();
        csv.append('\uFEFF'); // BOM for UTF-8 Excel support
        
        if ("overview".equalsIgnoreCase(scope) || "Tổng hợp".equalsIgnoreCase(scope)) {
            csv.append("Sự kiện,Loại,Vé đã bán,Lấp đầy,Doanh thu,Check-in,Resale,Royalty Fee,Trạng thái\n");
            for (OrganizerDashboardResponse.EventPerformanceDto row : dashboard.getPerformanceTable()) {
                csv.append(String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"\n",
                        escapeCsv(row.getName()),
                        escapeCsv(row.getType()),
                        escapeCsv(row.getSold()),
                        escapeCsv(row.getOccupancy()),
                        escapeCsv(row.getRevenue()),
                        escapeCsv(row.getCheckin()),
                        escapeCsv(row.getResale()),
                        escapeCsv(row.getRoyalty()),
                        escapeCsv(row.getStatus())
                ));
            }
        } else if ("revenue".equalsIgnoreCase(scope) || "Doanh thu".equalsIgnoreCase(scope)) {
            csv.append("Ngày,Doanh thu\n");
            for (OrganizerDashboardResponse.DailyRevenueDto row : dashboard.getRevenueTrend()) {
                csv.append(String.format("%d,\"%s\"\n", row.getDay(), row.getRevenue().toString()));
            }
        } else {
            // Mock for other scopes
            csv.append("Data for scope: ").append(scope).append("\n");
            csv.append("Feature is currently mocked. Please request specific scope implementation if needed.\n");
        }
        
        return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }
}
