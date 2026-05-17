package com.capstone.inventoryservice.domain.controller;

import com.capstone.inventoryservice.domain.dto.BasePageResponse;
import com.capstone.inventoryservice.domain.dto.BaseResponse;
import com.capstone.inventoryservice.domain.dto.request.EventApprovalRequest;
import com.capstone.inventoryservice.domain.dto.request.EventFilterRequest;
import com.capstone.inventoryservice.domain.dto.response.EventResponse;
import com.capstone.inventoryservice.domain.dto.response.ListEventResponse;
import com.capstone.inventoryservice.domain.service.EventService;
import com.capstone.inventoryservice.model.enums.EventSortOption;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/events")
@RequiredArgsConstructor
public class AdminEventController {

    private final EventService eventService;

    @GetMapping("/pending")
    public ResponseEntity<BaseResponse<BasePageResponse<ListEventResponse>>> getPendingEvents(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size,
            @RequestParam(required = false, defaultValue = "NEWEST") EventSortOption sort
    ) {
        EventFilterRequest filter = EventFilterRequest.builder()
                .page(page - 1)
                .size(size)
                .sort(sort)
                .build();

        BasePageResponse<ListEventResponse> response = eventService.getPendingEventsForAdmin(filter);
        return ResponseEntity.ok(BaseResponse.ok("Lấy danh sách sự kiện chờ duyệt thành công", response));
    }

    @PatchMapping("/{eventId}/approval")
    public ResponseEntity<BaseResponse<EventResponse>> updateApproval(
            @PathVariable Long eventId,
            @Valid @RequestBody EventApprovalRequest request
    ) {
        EventResponse response = eventService.updateApprovalStatus(eventId, request.getApprovalStatus());
        return ResponseEntity.ok(BaseResponse.ok("Cập nhật trạng thái duyệt sự kiện thành công", response));
    }
}
