package com.capstone.inventoryservice.domain.controller;

import com.capstone.inventoryservice.domain.dto.BaseResponse;
import com.capstone.inventoryservice.domain.dto.response.AdminAuditResponse;
import com.capstone.inventoryservice.domain.service.AdminAuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/audit")
@RequiredArgsConstructor
@Tag(name = "Admin Audit Logs", description = "Các endpoint để truy xuất nhật ký hệ thống của Admin")
public class AdminAuditController {

    private final AdminAuditService adminAuditService;

    @Operation(summary = "Lấy danh sách nhật ký hệ thống", description = "Trả về thống kê và danh sách phân trang nhật ký hành vi người dùng, admin của hệ thống")
    @GetMapping
    public ResponseEntity<BaseResponse<AdminAuditResponse>> getAuditDashboard(
            @RequestParam(defaultValue = "all") String tab,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        AdminAuditResponse response = adminAuditService.getAuditDashboard(tab, search, page, size);
        return ResponseEntity.ok(BaseResponse.ok("Lấy danh sách audit logs thành công", response));
    }
}
