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
            @RequestParam(defaultValue = "7") int days
    ) {
        PlatformDashboardResponse response = dashboardService.getPlatformDashboard(days);
        return ResponseEntity.ok(BaseResponse.ok("Lấy dữ liệu dashboard thành công", response));
    }
}
