package com.capstone.inventoryservice.domain.controller;

import com.capstone.inventoryservice.domain.dto.BaseResponse;
import com.capstone.inventoryservice.domain.dto.response.OrganizerDashboardResponse;
import com.capstone.inventoryservice.domain.dto.response.PlatformDashboardResponse;
import com.capstone.inventoryservice.domain.service.DashboardService;
import com.capstone.inventoryservice.domain.dto.request.DashboardExportRequest;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService dashboardService;

    @GetMapping("/platform")
    @Operation(summary = "Lấy dữ liệu dashboard cho platform", description = "Lấy dữ liệu thống kê tổng quát về doanh thu, số vé và người dùng mới cho platform")
    public ResponseEntity<BaseResponse<PlatformDashboardResponse>> getPlatformDashboard(
            @RequestParam(defaultValue = "7") int days) {
        PlatformDashboardResponse response = dashboardService.getPlatformDashboard(days);
        return ResponseEntity.ok(BaseResponse.ok("Lấy dữ liệu dashboard thành công", response));
    }

    @GetMapping("/organizer")
    @Operation(summary = "Lấy dữ liệu dashboard cho organizer", description = "Lấy thông tin tổng hợp về doanh thu, tỷ lệ lấp đầy, check-in, resale của một organizer")
    public ResponseEntity<BaseResponse<OrganizerDashboardResponse>> getOrganizerDashboard(
            @RequestParam(defaultValue = "30") int days) {
        OrganizerDashboardResponse response = dashboardService.getOrganizerDashboard(days);
        return ResponseEntity.ok(BaseResponse.ok("Lấy dữ liệu dashboard organizer thành công", response));
    }

    @GetMapping("/organizer/export")
    @Operation(summary = "Xuất báo cáo dashboard organizer dạng GET (Legacy)", description = "Xuất báo cáo phẳng với phạm vi cụ thể bằng phương thức GET")
    public ResponseEntity<byte[]> exportOrganizerDashboard(
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam(defaultValue = "overview") String scope,
            @RequestParam(defaultValue = "30") int days) {
        byte[] data = dashboardService.exportOrganizerDashboard(format, scope, days);

        String filename = "organizer_report_" + scope + "."
                + (format.equalsIgnoreCase("pdf") || format.equalsIgnoreCase("xlsx") ? "csv" : format);

        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, "text/csv; charset=utf-8")
                .body(data);
    }

    @PostMapping("/organizer/export")
    @Operation(summary = "Xuất báo cáo dashboard organizer nâng cao (POST)", description = "Xuất báo cáo động dạng CSV, XLSX hoặc PDF theo các phần tùy chọn chọn lọc")
    public ResponseEntity<byte[]> exportOrganizerDashboardPost(
            @RequestBody DashboardExportRequest request) {
        byte[] data = dashboardService.exportOrganizerDashboardAdvanced(request);
        
        String ext = request.getFormat() != null ? request.getFormat().toLowerCase() : "csv";
        String contentType = switch (ext) {
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "pdf" -> "application/pdf";
            default -> "text/csv; charset=utf-8";
        };
        
        String filename = "organizer_dashboard_export_" + System.currentTimeMillis() + "." + ext;
        
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, contentType)
                .body(data);
    }
}
