package com.capstone.inventoryservice.domain.controller;

import com.capstone.inventoryservice.domain.dto.BaseResponse;
import com.capstone.inventoryservice.domain.service.BankSyncService;
import com.capstone.inventoryservice.model.entity.Bank;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/banks")
@RequiredArgsConstructor
@Tag(name = "Bank Management", description = "API quản lý danh sách ngân hàng")
public class BankController {

    private final BankSyncService bankSyncService;

    @PostMapping("/sync")
    @Operation(summary = "Đồng bộ danh sách ngân hàng từ VietQR")
    public ResponseEntity<BaseResponse<List<Bank>>> syncBanks() {
        List<Bank> banks = bankSyncService.syncBanks();
        return ResponseEntity.ok(BaseResponse.ok("Đồng bộ danh sách ngân hàng thành công", banks));
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách ngân hàng đã đồng bộ")
    public ResponseEntity<BaseResponse<List<Bank>>> getAllBanks() {
        List<Bank> banks = bankSyncService.getAllBanks();
        return ResponseEntity.ok(BaseResponse.ok("Lấy danh sách ngân hàng thành công", banks));
    }
}
