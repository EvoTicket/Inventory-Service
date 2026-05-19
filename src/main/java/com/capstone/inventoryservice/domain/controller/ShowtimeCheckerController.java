package com.capstone.inventoryservice.domain.controller;

import com.capstone.inventoryservice.domain.dto.BaseResponse;
import com.capstone.inventoryservice.domain.dto.request.AssignCheckerRequest;
import com.capstone.inventoryservice.domain.dto.request.ApproveCheckerRequest;
import com.capstone.inventoryservice.domain.dto.response.EventResponse;
import com.capstone.inventoryservice.domain.dto.response.ShowtimeCheckerResponse;
import com.capstone.inventoryservice.domain.dto.response.CheckerEventResponse;
import com.capstone.inventoryservice.domain.service.ShowtimeCheckerService;
import com.capstone.inventoryservice.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ShowtimeCheckerController {

    private final ShowtimeCheckerService showtimeCheckerService;
    private final JwtUtil jwtUtil;

    // Admin gán trực tiếp checker vào showtime
    @PostMapping("/showtimes/{showtimeId}/checkers/assign")
    public ResponseEntity<BaseResponse<ShowtimeCheckerResponse>> assignChecker(
            @PathVariable Long showtimeId,
            @RequestBody AssignCheckerRequest request
    ) {
        ShowtimeCheckerResponse response = showtimeCheckerService.assignChecker(showtimeId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.ok(response));
    }

    // Checker đăng ký tham gia showtime
    @PostMapping("/showtimes/{showtimeId}/checkers/register")
    public ResponseEntity<BaseResponse<ShowtimeCheckerResponse>> registerChecker(
            @PathVariable Long showtimeId
    ) {
        ShowtimeCheckerResponse response = showtimeCheckerService.registerChecker(showtimeId);
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.ok(response));
    }

    // Admin duyệt/từ chối yêu cầu tham gia của checker
    @PutMapping("/showtimes/{showtimeId}/checkers/{checkerId}/approve")
    public ResponseEntity<BaseResponse<ShowtimeCheckerResponse>> approveChecker(
            @PathVariable Long showtimeId,
            @PathVariable Long checkerId,
            @RequestBody ApproveCheckerRequest request
    ) {
        ShowtimeCheckerResponse response = showtimeCheckerService.approveChecker(showtimeId, checkerId, request);
        return ResponseEntity.ok(BaseResponse.ok(response));
    }

    // Xem danh sách event mà checker tham gia vào (đã xác nhận)
    @GetMapping("/checkers/approved-events")
    public ResponseEntity<BaseResponse<List<CheckerEventResponse>>> getApprovedEventsForChecker(
    ) {
        Long checkerId = jwtUtil.getDataFromAuth().userId();
        List<CheckerEventResponse> response = showtimeCheckerService.getApprovedEventsForChecker(checkerId);
        return ResponseEntity.ok(BaseResponse.ok(response));
    }
}
