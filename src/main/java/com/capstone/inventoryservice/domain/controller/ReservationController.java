package com.capstone.inventoryservice.domain.controller;

import com.capstone.inventoryservice.domain.dto.BaseResponse;
import com.capstone.inventoryservice.domain.dto.request.ReserveRequest;
import com.capstone.inventoryservice.domain.dto.response.ReserveResponse;
import com.capstone.inventoryservice.domain.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping("/reserve")
    public ResponseEntity<BaseResponse<ReserveResponse>> reserveTickets(@Valid @RequestBody ReserveRequest request) {
        ReserveResponse response = reservationService.reserveTickets(request);
        return ResponseEntity.ok(BaseResponse.ok("Giữ vé thành công", response));
    }
}
