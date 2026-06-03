package com.capstone.inventoryservice.domain.service;

import com.capstone.inventoryservice.domain.client.*;
import com.capstone.inventoryservice.domain.dto.response.PlatformDashboardResponse;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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

import com.capstone.inventoryservice.domain.dto.request.DashboardExportRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

// Apache POI for Excel export
import org.apache.poi.xssf.usermodel.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;

// OpenPDF for PDF export
import com.lowagie.text.Document;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Phrase;
import com.lowagie.text.Element;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPCell;
import java.awt.Color;

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

        Map<LocalDate, DailyStatsInternalDto> trendMap = stats.getTrend() != null ?
                stats.getTrend().stream().collect(Collectors.toMap(
                        DailyStatsInternalDto::getDate,
                        d -> d,
                        (existing, replacement) -> existing
                )) : Collections.emptyMap();

        List<PlatformDashboardResponse.DailyTrendDto> paddedTrend = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            DailyStatsInternalDto dayStats = trendMap.get(date);
            
            BigDecimal gmv = dayStats != null ? dayStats.getGmv() : BigDecimal.ZERO;
            long tickets = dayStats != null ? dayStats.getTicketsSold() : 0L;

            paddedTrend.add(PlatformDashboardResponse.DailyTrendDto.builder()
                    .date(date.toString())
                    .gmv(gmv)
                    .ticketsSold(tickets)
                    .build());
        }

        return PlatformDashboardResponse.builder()
                .totalGmv(stats.getTotalGmv())
                .totalRevenue(stats.getTotalRevenue())
                .totalTicketsSold(stats.getTotalTicketsSold())
                .newUsersCount(newUsers)
                .trend(paddedTrend)
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
                    .type(event.getCategory() != null ? event.getCategory().name() : "Draft")
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
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.format.DateTimeFormatter keyFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
        java.time.format.DateTimeFormatter displayFormatter = java.time.format.DateTimeFormatter.ofPattern("d/M");
        
        for (int i = days - 1; i >= 0; i--) {
            java.time.LocalDate date = today.minusDays(i);
            String key = date.format(keyFormatter);
            String display = date.format(displayFormatter);
            BigDecimal revenue = dailyRevenueMap.getOrDefault(key, BigDecimal.ZERO);
            
            revenueTrend.add(OrganizerDashboardResponse.DailyRevenueDto.builder()
                    .date(display)
                    .revenue(revenue)
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
                csv.append(String.format("\"%s\",\"%s\"\n", row.getDate(), row.getRevenue().toString()));
            }
        } else {
            csv.append("Data for scope: ").append(scope).append("\n");
            csv.append("Feature is currently mocked. Please request specific scope implementation if needed.\n");
        }
        
        return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    public byte[] exportOrganizerDashboardAdvanced(DashboardExportRequest request) {
        int days = request.getDays() != null ? request.getDays() : 30;
        OrganizerDashboardResponse dashboard = getOrganizerDashboard(days);
        
        List<String> sections = request.getSections();
        if (sections == null || sections.isEmpty()) {
            sections = List.of("summary", "revenue", "tickets", "checkin", "resale");
        }
        
        String format = request.getFormat() != null ? request.getFormat().toUpperCase() : "CSV";
        
        switch (format) {
            case "XLSX":
                return generateXlsxReport(dashboard, sections);
            case "PDF":
                return generatePdfReport(dashboard, sections);
            case "CSV":
            default:
                return generateCsvReport(dashboard, sections, request.getSeparator(), request.getIncludeHeaders());
        }
    }

    private byte[] generateCsvReport(OrganizerDashboardResponse dashboard, List<String> sections, String separator, Boolean includeHeaders) {
        String sep = (separator != null && !separator.isEmpty()) ? separator : ",";
        boolean headers = includeHeaders == null || includeHeaders;
        
        StringBuilder sb = new StringBuilder();
        sb.append('\uFEFF'); // UTF-8 BOM
        
        for (int i = 0; i < sections.size(); i++) {
            String section = sections.get(i).toLowerCase();
            if (i > 0) {
                sb.append("\n\n"); // space between sections
            }
            
            switch (section) {
                case "summary":
                    if (headers) {
                        sb.append("--- OVERVIEW STATS ---\n");
                        sb.append("Metric").append(sep).append("Value\n");
                    }
                    sb.append("Total Revenue").append(sep).append(dashboard.getTotalRevenue()).append("\n");
                    sb.append("Total Tickets Sold").append(sep).append(dashboard.getTotalTicketsSold()).append("\n");
                    sb.append("Average Occupancy Rate").append(sep).append(dashboard.getAvgOccupancyRate()).append("%\n");
                    sb.append("Average Check-in Rate").append(sep).append(dashboard.getAvgCheckInRate()).append("%\n");
                    sb.append("Resale Volume").append(sep).append(dashboard.getResaleVolume()).append("\n");
                    sb.append("Royalty Fee").append(sep).append(dashboard.getRoyaltyFee()).append("\n");
                    break;
                    
                case "revenue":
                    if (headers) {
                        sb.append("--- REVENUE TREND ---\n");
                        sb.append("Date").append(sep).append("Revenue\n");
                    }
                    for (OrganizerDashboardResponse.DailyRevenueDto row : dashboard.getRevenueTrend()) {
                        sb.append(row.getDate()).append(sep).append(row.getRevenue()).append("\n");
                    }
                    break;
                    
                case "tickets":
                    if (headers) {
                        sb.append("--- TICKET SALES & PERFORMANCE ---\n");
                        sb.append("Event Name").append(sep).append("Type").append(sep).append("Tickets Sold").append(sep).append("Occupancy").append(sep).append("Revenue").append(sep).append("Status\n");
                    }
                    for (OrganizerDashboardResponse.EventPerformanceDto row : dashboard.getPerformanceTable()) {
                        sb.append(String.format("\"%s\"%s\"%s\"%s\"%s\"%s\"%s\"%s\"%s\"%s\"%s\"\n",
                                escapeCsv(row.getName()), sep,
                                escapeCsv(row.getType()), sep,
                                escapeCsv(row.getSold()), sep,
                                escapeCsv(row.getOccupancy()), sep,
                                escapeCsv(row.getRevenue()), sep,
                                escapeCsv(row.getStatus())
                        ));
                    }
                    break;
                    
                case "checkin":
                    if (headers) {
                        sb.append("--- CHECK-IN STATUS ---\n");
                        sb.append("Metric").append(sep).append("Value\n");
                    }
                    if (dashboard.getCheckInStatus() != null) {
                        sb.append("Checked In").append(sep).append(dashboard.getCheckInStatus().getCheckedIn()).append("\n");
                        sb.append("Not Checked In").append(sep).append(dashboard.getCheckInStatus().getNotCheckedIn()).append("\n");
                        sb.append("Absent Rate").append(sep).append(dashboard.getCheckInStatus().getAbsentRate()).append("%\n");
                    }
                    
                    if (headers) {
                        sb.append("\nEvent Name").append(sep).append("Check-in Progress\n");
                    }
                    for (OrganizerDashboardResponse.EventPerformanceDto row : dashboard.getPerformanceTable()) {
                        sb.append(String.format("\"%s\"%s\"%s\"\n",
                                escapeCsv(row.getName()), sep,
                                escapeCsv(row.getCheckin())
                        ));
                    }
                    break;
                    
                case "resale":
                    if (headers) {
                        sb.append("--- RESALE & ROYALTY ---\n");
                        sb.append("Event Name").append(sep).append("Resale Volume").append(sep).append("Royalty Fee\n");
                    }
                    for (OrganizerDashboardResponse.EventPerformanceDto row : dashboard.getPerformanceTable()) {
                        sb.append(String.format("\"%s\"%s\"%s\"%s\"%s\"\n",
                                escapeCsv(row.getName()), sep,
                                escapeCsv(row.getResale()), sep,
                                escapeCsv(row.getRoyalty())
                        ));
                    }
                    break;
            }
        }
        
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private byte[] generateXlsxReport(OrganizerDashboardResponse dashboard, List<String> sections) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            
            // Cell Styles
            XSSFCellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(new org.apache.poi.xssf.usermodel.XSSFColor(new Color(139, 92, 246), null));
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            XSSFFont headerFont = workbook.createFont();
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setBold(true);
            headerFont.setFontName("Segoe UI");
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            
            XSSFCellStyle titleStyle = workbook.createCellStyle();
            XSSFFont titleFont = workbook.createFont();
            titleFont.setFontHeightInPoints((short) 16);
            titleFont.setBold(true);
            titleFont.setColor(new org.apache.poi.xssf.usermodel.XSSFColor(new Color(139, 92, 246), null).getIndex());
            titleFont.setFontName("Segoe UI");
            titleStyle.setFont(titleFont);
            
            XSSFCellStyle sectionTitleStyle = workbook.createCellStyle();
            XSSFFont secTitleFont = workbook.createFont();
            secTitleFont.setFontHeightInPoints((short) 12);
            secTitleFont.setBold(true);
            secTitleFont.setFontName("Segoe UI");
            sectionTitleStyle.setFont(secTitleFont);
            
            XSSFCellStyle dataStyle = workbook.createCellStyle();
            XSSFFont dataFont = workbook.createFont();
            dataFont.setFontName("Segoe UI");
            dataStyle.setFont(dataFont);
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setTopBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setLeftBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
            dataStyle.setBorderRight(BorderStyle.THIN);
            dataStyle.setRightBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
            
            XSSFCellStyle boldDataStyle = workbook.createCellStyle();
            XSSFFont boldDataFont = workbook.createFont();
            boldDataFont.setFontName("Segoe UI");
            boldDataFont.setBold(true);
            boldDataStyle.setFont(boldDataFont);
            boldDataStyle.cloneStyleFrom(dataStyle);
            
            for (String sec : sections) {
                String section = sec.toLowerCase();
                switch (section) {
                    case "summary":
                        createSummarySheet(workbook, dashboard, titleStyle, sectionTitleStyle, dataStyle, boldDataStyle, headerStyle);
                        break;
                    case "revenue":
                        createRevenueSheet(workbook, dashboard, titleStyle, headerStyle, dataStyle);
                        break;
                    case "tickets":
                        createTicketsSheet(workbook, dashboard, titleStyle, sectionTitleStyle, headerStyle, dataStyle);
                        break;
                    case "checkin":
                        createCheckInSheet(workbook, dashboard, titleStyle, sectionTitleStyle, headerStyle, dataStyle, boldDataStyle);
                        break;
                    case "resale":
                        createResaleSheet(workbook, dashboard, titleStyle, headerStyle, dataStyle);
                        break;
                }
            }
            
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            return bos.toByteArray();
        } catch (IOException e) {
            log.error("Failed to generate XLSX report", e);
            return new byte[0];
        }
    }

    private void createSummarySheet(XSSFWorkbook workbook, OrganizerDashboardResponse dashboard,
                                    XSSFCellStyle titleStyle, XSSFCellStyle sectionTitleStyle,
                                    XSSFCellStyle dataStyle, XSSFCellStyle boldDataStyle, XSSFCellStyle headerStyle) {
        XSSFSheet sheet = workbook.createSheet("Tổng quan");
        sheet.setDisplayGridlines(true);
        
        // Title
        XSSFRow row0 = sheet.createRow(0);
        XSSFCell cellTitle = row0.createCell(0);
        cellTitle.setCellValue("BÁO CÁO TỔNG QUAN");
        cellTitle.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));
        
        // Metrics Table
        int rowIdx = 2;
        XSSFRow secRow = sheet.createRow(rowIdx++);
        XSSFCell secCell = secRow.createCell(0);
        secCell.setCellValue("Chỉ số hiệu năng chính (KPIs)");
        secCell.setCellStyle(sectionTitleStyle);
        
        String[][] kpis = {
            {"Tổng doanh thu", formatCurrency(dashboard.getTotalRevenue())},
            {"Tổng số vé bán ra", String.format("%,d vé", dashboard.getTotalTicketsSold())},
            {"Tỷ lệ lấp đầy trung bình", dashboard.getAvgOccupancyRate() + "%"},
            {"Tỷ lệ check-in trung bình", dashboard.getAvgCheckInRate() + "%"},
            {"Khối lượng giao dịch Resale", String.format("%,d vé", dashboard.getResaleVolume())},
            {"Tổng phí bản quyền nhận được", formatCurrency(dashboard.getRoyaltyFee())}
        };
        
        for (String[] kpi : kpis) {
            XSSFRow row = sheet.createRow(rowIdx++);
            XSSFCell c0 = row.createCell(0);
            c0.setCellValue(kpi[0]);
            c0.setCellStyle(dataStyle);
            
            XSSFCell c1 = row.createCell(1);
            c1.setCellValue(kpi[1]);
            c1.setCellStyle(boldDataStyle);
        }
        
        // Occupancy By Category
        rowIdx += 2;
        XSSFRow catSecRow = sheet.createRow(rowIdx++);
        XSSFCell catSecCell = catSecRow.createCell(0);
        catSecCell.setCellValue("Tỷ lệ lấp đầy theo danh mục");
        catSecCell.setCellStyle(sectionTitleStyle);
        
        XSSFRow headerRow = sheet.createRow(rowIdx++);
        String[] headers = {"Danh mục sự kiện", "Tỷ lệ lấp đầy"};
        for (int i = 0; i < headers.length; i++) {
            XSSFCell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        if (dashboard.getOccupancyByCategory() != null) {
            for (OrganizerDashboardResponse.OccupancyByCategoryDto cat : dashboard.getOccupancyByCategory()) {
                XSSFRow row = sheet.createRow(rowIdx++);
                
                XSSFCell c0 = row.createCell(0);
                c0.setCellValue(cat.getName());
                c0.setCellStyle(dataStyle);
                
                XSSFCell c1 = row.createCell(1);
                c1.setCellValue(cat.getValue() + "%");
                c1.setCellStyle(dataStyle);
            }
        }
        
        for (int i = 0; i < 4; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createRevenueSheet(XSSFWorkbook workbook, OrganizerDashboardResponse dashboard,
                                    XSSFCellStyle titleStyle, XSSFCellStyle headerStyle, XSSFCellStyle dataStyle) {
        XSSFSheet sheet = workbook.createSheet("Doanh thu");
        sheet.setDisplayGridlines(true);
        
        XSSFRow row0 = sheet.createRow(0);
        XSSFCell cellTitle = row0.createCell(0);
        cellTitle.setCellValue("XU THẾ DOANH THU HÀNG NGÀY");
        cellTitle.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 2));
        
        int rowIdx = 2;
        XSSFRow headerRow = sheet.createRow(rowIdx++);
        String[] headers = {"Ngày", "Doanh thu"};
        for (int i = 0; i < headers.length; i++) {
            XSSFCell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        if (dashboard.getRevenueTrend() != null) {
            for (OrganizerDashboardResponse.DailyRevenueDto trend : dashboard.getRevenueTrend()) {
                XSSFRow row = sheet.createRow(rowIdx++);
                
                XSSFCell c0 = row.createCell(0);
                c0.setCellValue(trend.getDate());
                c0.setCellStyle(dataStyle);
                
                XSSFCell c1 = row.createCell(1);
                c1.setCellValue(trend.getRevenue().doubleValue());
                c1.setCellStyle(dataStyle);
            }
        }
        
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private void createTicketsSheet(XSSFWorkbook workbook, OrganizerDashboardResponse dashboard,
                                    XSSFCellStyle titleStyle, XSSFCellStyle sectionTitleStyle,
                                    XSSFCellStyle headerStyle, XSSFCellStyle dataStyle) {
        XSSFSheet sheet = workbook.createSheet("Vé & Sự kiện");
        sheet.setDisplayGridlines(true);
        
        XSSFRow row0 = sheet.createRow(0);
        XSSFCell cellTitle = row0.createCell(0);
        cellTitle.setCellValue("BÁO CÁO CHI TIẾT BÁN VÉ");
        cellTitle.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));
        
        int rowIdx = 2;
        XSSFRow secRow = sheet.createRow(rowIdx++);
        XSSFCell secCell = secRow.createCell(0);
        secCell.setCellValue("Doanh số vé theo Sự kiện");
        secCell.setCellStyle(sectionTitleStyle);
        
        XSSFRow headerRow = sheet.createRow(rowIdx++);
        String[] headers = {"Tên Sự kiện", "Vé đã bán"};
        for (int i = 0; i < headers.length; i++) {
            XSSFCell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        if (dashboard.getTicketSalesByEvent() != null) {
            for (OrganizerDashboardResponse.TicketSalesByEventDto sale : dashboard.getTicketSalesByEvent()) {
                XSSFRow row = sheet.createRow(rowIdx++);
                
                XSSFCell c0 = row.createCell(0);
                c0.setCellValue(sale.getName());
                c0.setCellStyle(dataStyle);
                
                XSSFCell c1 = row.createCell(1);
                c1.setCellValue(sale.getTickets());
                c1.setCellStyle(dataStyle);
            }
        }
        
        rowIdx += 2;
        XSSFRow secRow2 = sheet.createRow(rowIdx++);
        XSSFCell secCell2 = secRow2.createCell(0);
        secCell2.setCellValue("Hiệu năng Chi tiết các Sự kiện");
        secCell2.setCellStyle(sectionTitleStyle);
        
        XSSFRow headerRow2 = sheet.createRow(rowIdx++);
        String[] headers2 = {"Tên Sự kiện", "Danh mục", "Vé bán", "Lấp đầy", "Doanh thu", "Trạng thái"};
        for (int i = 0; i < headers2.length; i++) {
            XSSFCell cell = headerRow2.createCell(i);
            cell.setCellValue(headers2[i]);
            cell.setCellStyle(headerStyle);
        }
        
        if (dashboard.getPerformanceTable() != null) {
            for (OrganizerDashboardResponse.EventPerformanceDto row : dashboard.getPerformanceTable()) {
                XSSFRow r = sheet.createRow(rowIdx++);
                
                XSSFCell c0 = r.createCell(0); c0.setCellValue(row.getName()); c0.setCellStyle(dataStyle);
                XSSFCell c1 = r.createCell(1); c1.setCellValue(row.getType()); c1.setCellStyle(dataStyle);
                XSSFCell c2 = r.createCell(2); c2.setCellValue(row.getSold()); c2.setCellStyle(dataStyle);
                XSSFCell c3 = r.createCell(3); c3.setCellValue(row.getOccupancy()); c3.setCellStyle(dataStyle);
                XSSFCell c4 = r.createCell(4); c4.setCellValue(row.getRevenue()); c4.setCellStyle(dataStyle);
                XSSFCell c5 = r.createCell(5); c5.setCellValue(row.getStatus()); c5.setCellStyle(dataStyle);
            }
        }
        
        for (int i = 0; i < 6; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createCheckInSheet(XSSFWorkbook workbook, OrganizerDashboardResponse dashboard,
                                    XSSFCellStyle titleStyle, XSSFCellStyle sectionTitleStyle,
                                    XSSFCellStyle headerStyle, XSSFCellStyle dataStyle, XSSFCellStyle boldDataStyle) {
        XSSFSheet sheet = workbook.createSheet("Check-in");
        sheet.setDisplayGridlines(true);
        
        XSSFRow row0 = sheet.createRow(0);
        XSSFCell cellTitle = row0.createCell(0);
        cellTitle.setCellValue("PHÂN TÍCH LƯỢT CHECK-IN");
        cellTitle.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));
        
        int rowIdx = 2;
        if (dashboard.getCheckInStatus() != null) {
            XSSFRow secRow = sheet.createRow(rowIdx++);
            XSSFCell secCell = secRow.createCell(0);
            secCell.setCellValue("Tổng quan Check-in");
            secCell.setCellStyle(sectionTitleStyle);
            
            String[][] stats = {
                {"Số lượt đã Check-in", String.format("%,d vé", dashboard.getCheckInStatus().getCheckedIn())},
                {"Chưa Check-in", String.format("%,d vé", dashboard.getCheckInStatus().getNotCheckedIn())},
                {"Tỷ lệ vắng mặt (Absent)", dashboard.getCheckInStatus().getAbsentRate() + "%"}
            };
            
            for (String[] stat : stats) {
                XSSFRow row = sheet.createRow(rowIdx++);
                XSSFCell c0 = row.createCell(0); c0.setCellValue(stat[0]); c0.setCellStyle(dataStyle);
                XSSFCell c1 = row.createCell(1); c1.setCellValue(stat[1]); c1.setCellStyle(boldDataStyle);
            }
        }
        
        rowIdx += 2;
        XSSFRow secRow2 = sheet.createRow(rowIdx++);
        XSSFCell secCell2 = secRow2.createCell(0);
        secCell2.setCellValue("Tiến độ Check-in từng Sự kiện");
        secCell2.setCellStyle(sectionTitleStyle);
        
        XSSFRow headerRow = sheet.createRow(rowIdx++);
        String[] headers = {"Tên Sự kiện", "Tiến độ Check-in"};
        for (int i = 0; i < headers.length; i++) {
            XSSFCell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        if (dashboard.getPerformanceTable() != null) {
            for (OrganizerDashboardResponse.EventPerformanceDto row : dashboard.getPerformanceTable()) {
                XSSFRow r = sheet.createRow(rowIdx++);
                
                XSSFCell c0 = r.createCell(0);
                c0.setCellValue(row.getName());
                c0.setCellStyle(dataStyle);
                
                XSSFCell c1 = r.createCell(1);
                c1.setCellValue(row.getCheckin());
                c1.setCellStyle(dataStyle);
            }
        }
        
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private void createResaleSheet(XSSFWorkbook workbook, OrganizerDashboardResponse dashboard,
                                   XSSFCellStyle titleStyle, XSSFCellStyle headerStyle, XSSFCellStyle dataStyle) {
        XSSFSheet sheet = workbook.createSheet("Resale");
        sheet.setDisplayGridlines(true);
        
        XSSFRow row0 = sheet.createRow(0);
        XSSFCell cellTitle = row0.createCell(0);
        cellTitle.setCellValue("GIAO DỊCH RESALE & PHÍ BẢN QUYỀN");
        cellTitle.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));
        
        int rowIdx = 2;
        XSSFRow headerRow = sheet.createRow(rowIdx++);
        String[] headers = {"Tên Sự kiện", "Lượng giao dịch Resale", "Phí bản quyền (Royalty Fee)"};
        for (int i = 0; i < headers.length; i++) {
            XSSFCell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        if (dashboard.getPerformanceTable() != null) {
            for (OrganizerDashboardResponse.EventPerformanceDto row : dashboard.getPerformanceTable()) {
                XSSFRow r = sheet.createRow(rowIdx++);
                
                XSSFCell c0 = r.createCell(0);
                c0.setCellValue(row.getName());
                c0.setCellStyle(dataStyle);
                
                XSSFCell c1 = r.createCell(1);
                c1.setCellValue(row.getResale());
                c1.setCellStyle(dataStyle);
                
                XSSFCell c2 = r.createCell(2);
                c2.setCellValue(row.getRoyalty());
                c2.setCellStyle(dataStyle);
            }
        }
        
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(2);
    }

    private byte[] generatePdfReport(OrganizerDashboardResponse dashboard, List<String> sections) {
        Document document = new Document(PageSize.A4, 36f, 36f, 36f, 36f); // 0.5 inch margins
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            PdfWriter.getInstance(document, bos);
            document.open();
            
            // Fonts
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(139, 92, 246));
            Font secFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, new Color(139, 92, 246));
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
            Font textFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
            
            Color themeColor = new Color(139, 92, 246);
            Color lightGray = new Color(243, 244, 246);
            Color altColor = new Color(249, 250, 251);
            
            // Main Header
            Paragraph docTitle = new Paragraph("EVOTICKET - BÁO CÁO DASHBOARD ORGANIZER", titleFont);
            docTitle.setAlignment(Element.ALIGN_CENTER);
            docTitle.setSpacingAfter(5f);
            document.add(docTitle);
            
            Paragraph subText = new Paragraph("Thời gian xuất báo cáo: " + java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").format(LocalDateTime.now()), textFont);
            subText.setAlignment(Element.ALIGN_CENTER);
            subText.setSpacingAfter(20f);
            document.add(subText);
            
            for (String sec : sections) {
                String section = sec.toLowerCase();
                
                switch (section) {
                    case "summary":
                        document.add(new Paragraph("1. CHỈ SỐ TỔNG QUAN (KPIS)", secFont));
                        Paragraph space = new Paragraph(" ");
                        space.setSpacingAfter(5f);
                        document.add(space);
                        
                        PdfPTable kpiTable = new PdfPTable(2);
                        kpiTable.setWidthPercentage(100f);
                        kpiTable.setWidths(new float[]{60f, 40f});
                        kpiTable.setSpacingAfter(15f);
                        
                        kpiTable.addCell(createPdfCell("Chỉ số", boldFont, themeColor, Element.ALIGN_LEFT));
                        kpiTable.addCell(createPdfCell("Giá trị thực tế", boldFont, themeColor, Element.ALIGN_RIGHT));
                        
                        kpiTable.addCell(createPdfCell("Tổng doanh thu", textFont, Color.WHITE, Element.ALIGN_LEFT));
                        kpiTable.addCell(createPdfCell(formatCurrency(dashboard.getTotalRevenue()), boldFont, Color.WHITE, Element.ALIGN_RIGHT));
                        
                        kpiTable.addCell(createPdfCell("Tổng số vé bán ra", textFont, altColor, Element.ALIGN_LEFT));
                        kpiTable.addCell(createPdfCell(String.format("%,d vé", dashboard.getTotalTicketsSold()), boldFont, altColor, Element.ALIGN_RIGHT));
                        
                        kpiTable.addCell(createPdfCell("Tỷ lệ lấp đầy trung bình", textFont, Color.WHITE, Element.ALIGN_LEFT));
                        kpiTable.addCell(createPdfCell(dashboard.getAvgOccupancyRate() + "%", boldFont, Color.WHITE, Element.ALIGN_RIGHT));
                        
                        kpiTable.addCell(createPdfCell("Tỷ lệ check-in trung bình", textFont, altColor, Element.ALIGN_LEFT));
                        kpiTable.addCell(createPdfCell(dashboard.getAvgCheckInRate() + "%", boldFont, altColor, Element.ALIGN_RIGHT));
                        
                        kpiTable.addCell(createPdfCell("Lượng vé giao dịch Resale", textFont, Color.WHITE, Element.ALIGN_LEFT));
                        kpiTable.addCell(createPdfCell(String.format("%,d vé", dashboard.getResaleVolume()), boldFont, Color.WHITE, Element.ALIGN_RIGHT));
                        
                        kpiTable.addCell(createPdfCell("Tổng phí bản quyền nhận được", textFont, altColor, Element.ALIGN_LEFT));
                        kpiTable.addCell(createPdfCell(formatCurrency(dashboard.getRoyaltyFee()), boldFont, altColor, Element.ALIGN_RIGHT));
                        
                        document.add(kpiTable);
                        break;
                        
                    case "revenue":
                        document.add(new Paragraph("2. XU HƯỚNG DOANH THU", secFont));
                        Paragraph spaceRev = new Paragraph(" ");
                        spaceRev.setSpacingAfter(5f);
                        document.add(spaceRev);
                        
                        PdfPTable revTable = new PdfPTable(2);
                        revTable.setWidthPercentage(100f);
                        revTable.setWidths(new float[]{50f, 50f});
                        revTable.setSpacingAfter(15f);
                        
                        revTable.addCell(createPdfCell("Ngày", headerFont, themeColor, Element.ALIGN_CENTER));
                        revTable.addCell(createPdfCell("Doanh thu phát sinh", headerFont, themeColor, Element.ALIGN_RIGHT));
                        
                        boolean alt = false;
                        if (dashboard.getRevenueTrend() != null) {
                            for (OrganizerDashboardResponse.DailyRevenueDto row : dashboard.getRevenueTrend()) {
                                Color bg = alt ? altColor : Color.WHITE;
                                revTable.addCell(createPdfCell(row.getDate(), textFont, bg, Element.ALIGN_CENTER));
                                revTable.addCell(createPdfCell(formatCurrency(row.getRevenue()), textFont, bg, Element.ALIGN_RIGHT));
                                alt = !alt;
                            }
                        }
                        document.add(revTable);
                        break;
                        
                    case "tickets":
                        document.add(new Paragraph("3. CHI TIẾT BÁN VÉ & HIỆU NĂNG SỰ KIỆN", secFont));
                        Paragraph spaceTix = new Paragraph(" ");
                        spaceTix.setSpacingAfter(5f);
                        document.add(spaceTix);
                        
                        PdfPTable tixTable = new PdfPTable(5);
                        tixTable.setWidthPercentage(100f);
                        tixTable.setWidths(new float[]{40f, 15f, 12f, 15f, 18f});
                        tixTable.setSpacingAfter(15f);
                        
                        tixTable.addCell(createPdfCell("Sự kiện", headerFont, themeColor, Element.ALIGN_LEFT));
                        tixTable.addCell(createPdfCell("Thể loại", headerFont, themeColor, Element.ALIGN_CENTER));
                        tixTable.addCell(createPdfCell("Vé bán", headerFont, themeColor, Element.ALIGN_CENTER));
                        tixTable.addCell(createPdfCell("Lấp đầy", headerFont, themeColor, Element.ALIGN_CENTER));
                        tixTable.addCell(createPdfCell("Doanh thu", headerFont, themeColor, Element.ALIGN_RIGHT));
                        
                        boolean altT = false;
                        if (dashboard.getPerformanceTable() != null) {
                            for (OrganizerDashboardResponse.EventPerformanceDto row : dashboard.getPerformanceTable()) {
                                Color bg = altT ? altColor : Color.WHITE;
                                tixTable.addCell(createPdfCell(row.getName(), textFont, bg, Element.ALIGN_LEFT));
                                tixTable.addCell(createPdfCell(row.getType(), textFont, bg, Element.ALIGN_CENTER));
                                tixTable.addCell(createPdfCell(row.getSold(), textFont, bg, Element.ALIGN_CENTER));
                                tixTable.addCell(createPdfCell(row.getOccupancy(), textFont, bg, Element.ALIGN_CENTER));
                                tixTable.addCell(createPdfCell(row.getRevenue(), textFont, bg, Element.ALIGN_RIGHT));
                                altT = !altT;
                            }
                        }
                        document.add(tixTable);
                        break;
                        
                    case "checkin":
                        document.add(new Paragraph("4. PHÂN TÍCH CHECK-IN SỰ KIỆN", secFont));
                        Paragraph spaceCi = new Paragraph(" ");
                        spaceCi.setSpacingAfter(5f);
                        document.add(spaceCi);
                        
                        if (dashboard.getCheckInStatus() != null) {
                            PdfPTable ciStatsTable = new PdfPTable(2);
                            ciStatsTable.setWidthPercentage(100f);
                            ciStatsTable.setSpacingAfter(10f);
                            ciStatsTable.setWidths(new float[]{70f, 30f});
                            
                            ciStatsTable.addCell(createPdfCell("Chỉ số Check-in", boldFont, lightGray, Element.ALIGN_LEFT));
                            ciStatsTable.addCell(createPdfCell("Giá trị", boldFont, lightGray, Element.ALIGN_RIGHT));
                            
                            ciStatsTable.addCell(createPdfCell("Số lượt vé đã Check-in", textFont, Color.WHITE, Element.ALIGN_LEFT));
                            ciStatsTable.addCell(createPdfCell(String.format("%,d vé", dashboard.getCheckInStatus().getCheckedIn()), boldFont, Color.WHITE, Element.ALIGN_RIGHT));
                            
                            ciStatsTable.addCell(createPdfCell("Chưa Check-in", textFont, altColor, Element.ALIGN_LEFT));
                            ciStatsTable.addCell(createPdfCell(String.format("%,d vé", dashboard.getCheckInStatus().getNotCheckedIn()), boldFont, altColor, Element.ALIGN_RIGHT));
                            
                            ciStatsTable.addCell(createPdfCell("Tỷ lệ vắng mặt (Absent)", textFont, Color.WHITE, Element.ALIGN_LEFT));
                            ciStatsTable.addCell(createPdfCell(dashboard.getCheckInStatus().getAbsentRate() + "%", boldFont, Color.WHITE, Element.ALIGN_RIGHT));
                            
                            document.add(ciStatsTable);
                        }
                        
                        PdfPTable ciTable = new PdfPTable(2);
                        ciTable.setWidthPercentage(100f);
                        ciTable.setWidths(new float[]{70f, 30f});
                        ciTable.setSpacingAfter(15f);
                        
                        ciTable.addCell(createPdfCell("Sự kiện", headerFont, themeColor, Element.ALIGN_LEFT));
                        ciTable.addCell(createPdfCell("Tiến độ Check-in", headerFont, themeColor, Element.ALIGN_CENTER));
                        
                        boolean altCi = false;
                        if (dashboard.getPerformanceTable() != null) {
                            for (OrganizerDashboardResponse.EventPerformanceDto row : dashboard.getPerformanceTable()) {
                                Color bg = altCi ? altColor : Color.WHITE;
                                ciTable.addCell(createPdfCell(row.getName(), textFont, bg, Element.ALIGN_LEFT));
                                ciTable.addCell(createPdfCell(row.getCheckin(), textFont, bg, Element.ALIGN_CENTER));
                                altCi = !altCi;
                            }
                        }
                        document.add(ciTable);
                        break;
                        
                    case "resale":
                        document.add(new Paragraph("5. BÁO CÁO PHÂN PHỐI LẠI (RESALE)", secFont));
                        Paragraph spaceRe = new Paragraph(" ");
                        spaceRe.setSpacingAfter(5f);
                        document.add(spaceRe);
                        
                        PdfPTable reTable = new PdfPTable(3);
                        reTable.setWidthPercentage(100f);
                        reTable.setWidths(new float[]{50f, 25f, 25f});
                        reTable.setSpacingAfter(15f);
                        
                        reTable.addCell(createPdfCell("Sự kiện", headerFont, themeColor, Element.ALIGN_LEFT));
                        reTable.addCell(createPdfCell("Vé bán lại (Resale)", headerFont, themeColor, Element.ALIGN_CENTER));
                        reTable.addCell(createPdfCell("Phí bản quyền nhận được", headerFont, themeColor, Element.ALIGN_RIGHT));
                        
                        boolean altRe = false;
                        if (dashboard.getPerformanceTable() != null) {
                            for (OrganizerDashboardResponse.EventPerformanceDto row : dashboard.getPerformanceTable()) {
                                Color bg = altRe ? altColor : Color.WHITE;
                                reTable.addCell(createPdfCell(row.getName(), textFont, bg, Element.ALIGN_LEFT));
                                reTable.addCell(createPdfCell(row.getResale(), textFont, bg, Element.ALIGN_CENTER));
                                reTable.addCell(createPdfCell(row.getRoyalty(), textFont, bg, Element.ALIGN_RIGHT));
                                altRe = !altRe;
                            }
                        }
                        document.add(reTable);
                        break;
                }
            }
            
            document.close();
            return bos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate PDF report", e);
            return new byte[0];
        }
    }

    private PdfPCell createPdfCell(String text, Font font, Color bgColor, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bgColor);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(6f);
        cell.setBorderColor(new Color(209, 213, 219));
        return cell;
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }
}
