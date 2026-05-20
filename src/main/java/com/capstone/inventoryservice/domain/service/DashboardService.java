package com.capstone.inventoryservice.domain.service;

import com.capstone.inventoryservice.domain.client.*;
import com.capstone.inventoryservice.domain.dto.response.PlatformDashboardResponse;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.math.BigDecimal;
import com.capstone.inventoryservice.domain.dto.response.OrganizerDashboardResponse;
import com.capstone.inventoryservice.model.repository.EventRepository;
import com.capstone.inventoryservice.security.JwtUtil;
import com.capstone.inventoryservice.model.entity.Event;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {
    private final OrderFeignClient orderFeignClient;
    private final IAMFeignClient iamFeignClient;
    private final EventRepository eventRepository;
    private final JwtUtil jwtUtil;
    private final CheckInFeignClient checkInFeignClient;

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

    @Transactional(readOnly = true)
    public OrganizerDashboardResponse getOrganizerDashboard(int days) {
        Long orgId = jwtUtil.getDataFromAuth().organizationId();
        List<Event> events = eventRepository.findByOrganizerId(orgId);

        List<Long> eventIds = events.stream().map(Event::getId).toList();
        Map<Long, BigDecimal> revenueMap = Collections.emptyMap();
        try {
            if (!eventIds.isEmpty()) {
                revenueMap = orderFeignClient.getRevenueForEvents(eventIds);
                if (revenueMap == null) {
                    revenueMap = Collections.emptyMap();
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch event revenues", e);
        }

        Map<Long, Long> resaleVolumeMap = Collections.emptyMap();
        Map<Long, BigDecimal> royaltyFeeMap = Collections.emptyMap();
        Map<String, BigDecimal> dailyRevenueMap = Collections.emptyMap();
        try {
            if (!eventIds.isEmpty()) {
                OrganizerOrdersStatsInternalResponse orderStats = orderFeignClient.getOrganizerStats(eventIds, days);
                if (orderStats != null) {
                    if (orderStats.getResaleVolumeMap() != null) resaleVolumeMap = orderStats.getResaleVolumeMap();
                    if (orderStats.getRoyaltyFeeMap() != null) royaltyFeeMap = orderStats.getRoyaltyFeeMap();
                    if (orderStats.getDailyRevenueMap() != null) dailyRevenueMap = orderStats.getDailyRevenueMap();
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch organizer orders stats", e);
        }

        Map<Long, Long> checkedInMap = Collections.emptyMap();
        Map<Long, Long> totalTicketsMap = Collections.emptyMap();
        try {
            if (!eventIds.isEmpty()) {
                OrganizerCheckInStatsInternalResponse checkinStats = checkInFeignClient.getOrganizerStats(eventIds);
                if (checkinStats != null) {
                    if (checkinStats.getCheckedInMap() != null) checkedInMap = checkinStats.getCheckedInMap();
                    if (checkinStats.getTotalTicketsMap() != null) totalTicketsMap = checkinStats.getTotalTicketsMap();
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch organizer check-in stats", e);
        }

        long totalSoldAll = 0;
        BigDecimal totalRevenueAll = BigDecimal.ZERO;
        long totalCapacityAll = 0;

        long totalResaleVolumeAll = 0;
        BigDecimal totalRoyaltyFeeAll = BigDecimal.ZERO;
        long totalCheckedInAll = 0;
        long totalAccessTicketsAll = 0;

        List<OrganizerDashboardResponse.EventPerformanceDto> performanceTable = new ArrayList<>();
        List<OrganizerDashboardResponse.TicketSalesByEventDto> ticketSalesByEvent = new ArrayList<>();

        for (Event event : events) {
            int sold = event.getTotalSold();
            int capacity = event.getTotalCapacity();
            totalSoldAll += sold;
            totalCapacityAll += capacity;

            long occ = capacity > 0 ? (sold * 100L / capacity) : 0;

            BigDecimal rev = revenueMap.getOrDefault(event.getId(), BigDecimal.ZERO);
            totalRevenueAll = totalRevenueAll.add(rev);

            long eventResale = resaleVolumeMap.getOrDefault(event.getId(), 0L);
            BigDecimal eventRoyalty = royaltyFeeMap.getOrDefault(event.getId(), BigDecimal.ZERO);
            long eventCheckedIn = checkedInMap.getOrDefault(event.getId(), 0L);
            long eventTotalTickets = totalTicketsMap.getOrDefault(event.getId(), 0L);

            totalResaleVolumeAll += eventResale;
            totalRoyaltyFeeAll = totalRoyaltyFeeAll.add(eventRoyalty);
            totalCheckedInAll += eventCheckedIn;
            totalAccessTicketsAll += eventTotalTickets;

            String checkinStr = "-";
            if (eventTotalTickets > 0) {
                double rate = (double) eventCheckedIn * 100 / eventTotalTickets;
                checkinStr = String.format("%,d / %,d (%.1f%%)", eventCheckedIn, eventTotalTickets, rate);
            }

            ticketSalesByEvent.add(OrganizerDashboardResponse.TicketSalesByEventDto.builder()
                    .name(event.getEventName())
                    .tickets(sold)
                    .build());

            performanceTable.add(OrganizerDashboardResponse.EventPerformanceDto.builder()
                    .name(event.getEventName())
                    .type(event.getCategory() != null ? event.getCategory().name() : "N/A")
                    .sold(String.format("%,d", sold))
                    .occupancy(occ + "%")
                    .revenue(formatCurrency(rev))
                    .checkin(checkinStr)
                    .resale(String.format("%,d", eventResale))
                    .royalty(formatCurrency(eventRoyalty))
                    .status(event.getEventStatus() != null ? event.getEventStatus().name() : "UNKNOWN")
                    .build());
        }

        List<OrganizerDashboardResponse.OccupancyByCategoryDto> occupancyByCategory = buildOccupancyByCategory(events);
        
        double avgOcc = totalCapacityAll > 0 ? (double) totalSoldAll * 100 / totalCapacityAll : 0.0;
        double avgCheckIn = totalAccessTicketsAll > 0 ? (double) totalCheckedInAll * 100 / totalAccessTicketsAll : 0.0;
        double absentRate = totalAccessTicketsAll > 0 ? (double) (totalAccessTicketsAll - totalCheckedInAll) * 100 / totalAccessTicketsAll : 0.0;

        List<OrganizerDashboardResponse.DailyRevenueDto> revenueTrend = new ArrayList<>();
        List<String> sortedDates = new ArrayList<>(dailyRevenueMap.keySet());
        Collections.sort(sortedDates);
        int dayIndex = 1;
        for (String dateStr : sortedDates) {
            revenueTrend.add(OrganizerDashboardResponse.DailyRevenueDto.builder()
                    .day(dayIndex++)
                    .revenue(dailyRevenueMap.get(dateStr))
                    .build());
        }

        return OrganizerDashboardResponse.builder()
                .totalRevenue(totalRevenueAll)
                .totalTicketsSold(totalSoldAll)
                .avgOccupancyRate(Math.round(avgOcc * 10.0) / 10.0)
                .avgCheckInRate(Math.round(avgCheckIn * 10.0) / 10.0)
                .resaleVolume(totalResaleVolumeAll)
                .royaltyFee(totalRoyaltyFeeAll)
                .revenueTrend(revenueTrend)
                .occupancyByCategory(occupancyByCategory)
                .ticketSalesByEvent(ticketSalesByEvent)
                .checkInStatus(OrganizerDashboardResponse.CheckInStatusDto.builder()
                        .checkedIn(totalCheckedInAll)
                        .notCheckedIn(Math.max(0, totalAccessTicketsAll - totalCheckedInAll))
                        .absentRate(Math.round(absentRate * 10.0) / 10.0)
                        .peakGateTime(null)
                        .build())
                .performanceTable(performanceTable)
                .build();
    }

    private List<OrganizerDashboardResponse.OccupancyByCategoryDto> buildOccupancyByCategory(List<Event> events) {
        Map<String, List<Event>> eventsByCategory = events.stream()
                .collect(Collectors.groupingBy(event -> event.getCategory() != null ? event.getCategory().name() : "N/A"));

        String[] colors = {"#8b5cf6", "#f59e0b", "#6366f1", "#10b981", "#ef4444", "#06b6d4"};
        List<OrganizerDashboardResponse.OccupancyByCategoryDto> result = new ArrayList<>();
        int index = 0;

        for (Map.Entry<String, List<Event>> entry : eventsByCategory.entrySet()) {
            long sold = entry.getValue().stream().mapToLong(Event::getTotalSold).sum();
            long capacity = entry.getValue().stream().mapToLong(Event::getTotalCapacity).sum();
            double occupancy = capacity > 0 ? (double) sold * 100 / capacity : 0.0;

            result.add(OrganizerDashboardResponse.OccupancyByCategoryDto.builder()
                    .name(entry.getKey())
                    .value(Math.round(occupancy * 10.0) / 10.0)
                    .color(colors[index % colors.length])
                    .build());
            index++;
        }

        return result;
    }

    private String formatCurrency(BigDecimal value) {
        if (value == null) {
            return "0 VNĐ";
        }
        if (value.compareTo(new BigDecimal("1000000000")) >= 0) {
            return String.format("%.1f tỷ", value.doubleValue() / 1000000000.0);
        }
        if (value.compareTo(new BigDecimal("1000000")) >= 0) {
            return String.format("%.1f triệu", value.doubleValue() / 1000000.0);
        }
        return String.format("%,d VNĐ", value.longValue());
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
