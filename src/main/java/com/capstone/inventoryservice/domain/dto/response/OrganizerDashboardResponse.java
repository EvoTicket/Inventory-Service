package com.capstone.inventoryservice.domain.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class OrganizerDashboardResponse {
    private BigDecimal totalRevenue;
    private long totalTicketsSold;
    private double avgOccupancyRate;
    private double avgCheckInRate;
    private long resaleVolume;
    private BigDecimal royaltyFee;

    private List<DailyRevenueDto> revenueTrend;
    private List<OccupancyByCategoryDto> occupancyByCategory;
    private List<TicketSalesByEventDto> ticketSalesByEvent;
    private CheckInStatusDto checkInStatus;
    private List<EventPerformanceDto> performanceTable;

    @Data
    @Builder
    public static class DailyRevenueDto {
        private int day;
        private BigDecimal revenue;
    }

    @Data
    @Builder
    public static class OccupancyByCategoryDto {
        private String name;
        private double value;
        private String color;
    }

    @Data
    @Builder
    public static class TicketSalesByEventDto {
        private String name;
        private long tickets;
    }

    @Data
    @Builder
    public static class CheckInStatusDto {
        private long checkedIn;
        private long notCheckedIn;
        private double absentRate;
        private String peakGateTime;
    }

    @Data
    @Builder
    public static class EventPerformanceDto {
        private String name;
        private String type;
        private String sold;
        private String occupancy;
        private String revenue;
        private String checkin;
        private String resale;
        private String royalty;
        private String status;
    }
}
