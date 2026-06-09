package com.capstone.inventoryservice.domain.controller;

import com.capstone.inventoryservice.domain.dto.BasePageResponse;
import com.capstone.inventoryservice.domain.dto.BaseResponse;
import com.capstone.inventoryservice.domain.dto.request.EventApprovalRequest;
import com.capstone.inventoryservice.domain.dto.request.EventFilterRequest;
import com.capstone.inventoryservice.domain.dto.response.EventResponse;
import com.capstone.inventoryservice.domain.dto.response.ListEventResponse;
import com.capstone.inventoryservice.domain.service.EventService;
import com.capstone.inventoryservice.domain.dto.response.EventModerationSummaryResponse;
import com.capstone.inventoryservice.model.enums.EventApprovalStatus;
import com.capstone.inventoryservice.model.enums.EventCategory;
import com.capstone.inventoryservice.model.enums.EventSortOption;
import java.util.List;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PutMapping("/{eventId}/approval")
    @com.capstone.inventoryservice.config.audit.AuditAction(
            action = "Duyệt sự kiện",
            module = "Event Moderation",
            severity = "High",
            sensitive = true,
            targetType = "Event"
    )
    public ResponseEntity<BaseResponse<EventResponse>> updateApproval(
            @PathVariable Long eventId,
            @Valid @RequestBody EventApprovalRequest request
    ) {
        EventResponse response = eventService.updateApprovalStatus(eventId, request.getApprovalStatus());
        return ResponseEntity.ok(BaseResponse.ok("Cập nhật trạng thái duyệt sự kiện thành công", response));
    }

    @GetMapping("/moderation")
    public ResponseEntity<BaseResponse<BasePageResponse<ListEventResponse>>> getEventsForModeration(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) List<EventApprovalStatus> approvalStatuses,
            @RequestParam(required = false) List<EventCategory> categories,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size,
            @RequestParam(required = false, defaultValue = "NEWEST") EventSortOption sort
    ) {
        EventFilterRequest filter = EventFilterRequest.builder()
                .keyword(keyword)
                .approvalStatuses(approvalStatuses)
                .categories(categories)
                .page(page - 1)
                .size(size)
                .sort(sort)
                .build();
        BasePageResponse<ListEventResponse> response = eventService.getEventsForModeration(filter);
        return ResponseEntity.ok(BaseResponse.ok("Lấy danh sách sự kiện kiểm duyệt thành công", response));
    }

    @GetMapping("/moderation/summary")
    public ResponseEntity<BaseResponse<EventModerationSummaryResponse>> getModerationSummary() {
        EventModerationSummaryResponse response = eventService.getModerationSummary();
        return ResponseEntity.ok(BaseResponse.ok("Lấy thông tin tổng quan duyệt sự kiện thành công", response));
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<BaseResponse<EventResponse>> getEventDetail(
            @PathVariable Long eventId
    ) {
        EventResponse response = eventService.getEventByIdForAdmin(eventId);
        return ResponseEntity.ok(BaseResponse.ok("Lấy chi tiết sự kiện thành công", response));
    }
}
