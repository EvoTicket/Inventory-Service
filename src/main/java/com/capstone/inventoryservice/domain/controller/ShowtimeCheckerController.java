package com.capstone.inventoryservice.domain.controller;

import com.capstone.inventoryservice.domain.dto.BaseResponse;
import com.capstone.inventoryservice.domain.dto.request.AssignCheckerRequest;
import com.capstone.inventoryservice.domain.dto.request.ApproveCheckerRequest;
import com.capstone.inventoryservice.domain.dto.response.EventResponse;
import com.capstone.inventoryservice.domain.dto.response.ShowtimeResponse;
import com.capstone.inventoryservice.domain.dto.response.ShowtimeCheckerResponse;
import com.capstone.inventoryservice.domain.service.ShowtimeCheckerService;
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

    // Xem danh sách checker theo showtime
    @GetMapping("/showtimes/{showtimeId}/checkers")
    public ResponseEntity<BaseResponse<List<ShowtimeCheckerResponse>>> getCheckersByShowtime(
            @PathVariable Long showtimeId
    ) {
        List<ShowtimeCheckerResponse> response = showtimeCheckerService.getCheckersByShowtime(showtimeId);
        return ResponseEntity.ok(BaseResponse.ok(response));
    }

    // Xem danh sách showtime của một checker
    @GetMapping("/checkers/{checkerId}/showtimes")
    public ResponseEntity<BaseResponse<List<ShowtimeCheckerResponse>>> getShowtimesForChecker(
            @PathVariable Long checkerId
    ) {
        List<ShowtimeCheckerResponse> response = showtimeCheckerService.getShowtimesForChecker(checkerId);
        return ResponseEntity.ok(BaseResponse.ok(response));
    }

    // Xem danh sách event mà checker tham gia vào (đã xác nhận)
    @GetMapping("/checkers/{checkerId}/approved-events")
    public ResponseEntity<BaseResponse<List<EventResponse>>> getApprovedEventsForChecker(
            @PathVariable Long checkerId
    ) {
        List<EventResponse> response = showtimeCheckerService.getApprovedEventsForChecker(checkerId);
        return ResponseEntity.ok(BaseResponse.ok(response));
    }

    // Xem danh sách showtime mà checker tham gia vào (đã xác nhận)
    @GetMapping("/checkers/{checkerId}/approved-showtimes")
    public ResponseEntity<BaseResponse<List<ShowtimeResponse>>> getApprovedShowtimesForChecker(
            @PathVariable Long checkerId
    ) {
        List<ShowtimeResponse> response = showtimeCheckerService.getApprovedShowtimesForChecker(checkerId);
        return ResponseEntity.ok(BaseResponse.ok(response));
    }
}
