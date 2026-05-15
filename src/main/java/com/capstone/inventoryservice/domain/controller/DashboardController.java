package com.capstone.inventoryservice.domain.controller;

import com.capstone.inventoryservice.domain.dto.BaseResponse;
import com.capstone.inventoryservice.domain.dto.response.PlatformDashboardResponse;
import com.capstone.inventoryservice.domain.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService dashboardService;

    @GetMapping("/platform")
    public ResponseEntity<BaseResponse<PlatformDashboardResponse>> getPlatformDashboard(
            @RequestParam(defaultValue = "7") int days) {
        PlatformDashboardResponse response = dashboardService.getPlatformDashboard(days);
        return ResponseEntity.ok(BaseResponse.ok("Lấy dữ liệu dashboard thành công", response));
    }

    @GetMapping("/organizer")
    public ResponseEntity<BaseResponse<com.capstone.inventoryservice.domain.dto.response.OrganizerDashboardResponse>> getOrganizerDashboard(
            @RequestParam(defaultValue = "30") int days) {
        com.capstone.inventoryservice.domain.dto.response.OrganizerDashboardResponse response = dashboardService
                .getOrganizerDashboard(days);
        return ResponseEntity.ok(BaseResponse.ok("Lấy dữ liệu dashboard organizer thành công", response));
    }

    @GetMapping("/organizer/export")
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
}
