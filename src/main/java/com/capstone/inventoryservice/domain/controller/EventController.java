package com.capstone.inventoryservice.domain.controller;

import com.capstone.inventoryservice.domain.dto.BasePageResponse;
import com.capstone.inventoryservice.domain.dto.BaseResponse;
import com.capstone.inventoryservice.domain.dto.request.CreateEventRequest;
import com.capstone.inventoryservice.domain.dto.request.EventFilterRequest;
import com.capstone.inventoryservice.domain.dto.request.UpdateEventRequest;
import com.capstone.inventoryservice.domain.dto.response.EventResponse;
import com.capstone.inventoryservice.domain.dto.response.ListEventResponse;
import com.capstone.inventoryservice.model.enums.EventStatus;
import com.capstone.inventoryservice.model.enums.EventType;
import com.capstone.inventoryservice.domain.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @GetMapping
    @Operation(summary = "Get all events with filters",
            description = "Get paginated list of events with search, filter and sort options")
    public ResponseEntity<BaseResponse<BasePageResponse<ListEventResponse>>> getEvents(
            @Parameter(description = "Search keyword (event name, description, venue)")
            @RequestParam(required = false) String keyword,

            @Parameter(description = "Filter by multiple category IDs")
            @RequestParam(required = false) List<Long> categoryIds,

            @Parameter(description = "Filter by multiple event types")
            @RequestParam(required = false) List<EventType> eventTypes,

            @Parameter(description = "Filter by multiple event statuses")
            @RequestParam(required = false) List<EventStatus> eventStatuses,

            @Parameter(description = "Filter by multiple province codes")
            @RequestParam(required = false) List<Integer> provinceCodes,

            @Parameter(description = "Filter featured events only")
            @RequestParam(required = false) Boolean isFeatured,

            @Parameter(description = "Filter by start date (YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,

            @Parameter(description = "Filter by end date (YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,

            @Parameter(description = "Filter events happening on specific date (YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate eventDate,

            @Parameter(description = "Minimum number of seats")
            @RequestParam(required = false) Integer minSeats,

            @Parameter(description = "Maximum number of seats")
            @RequestParam(required = false) Integer maxSeats,

            @Parameter(description = "Include expired events (default: true)")
            @RequestParam(required = false, defaultValue = "true") Boolean includeExpired,

            @Parameter(description = "Page number (1-indexed)")
            @RequestParam(required = false, defaultValue = "1") Integer page,

            @Parameter(description = "Page size (max: 100)")
            @RequestParam(required = false, defaultValue = "20") Integer size,

            @Parameter(description = "Sort by field (createdAt, startDatetime, totalSeats, eventName)")
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,

            @Parameter(description = "Sort direction (ASC, DESC)")
            @RequestParam(required = false, defaultValue = "DESC") String sortDirection
    ) {
        EventFilterRequest filter = EventFilterRequest.builder()
                .keyword(keyword)
                .categoryIds(categoryIds)
                .eventTypes(eventTypes)
                .eventStatuses(eventStatuses)
                .provinceCodes(provinceCodes)
                .isFeatured(isFeatured)
                .startDate(startDate)
                .endDate(endDate)
                .eventDate(eventDate)
                .minSeats(minSeats)
                .maxSeats(maxSeats)
                .includeExpired(includeExpired)
                .page(page - 1)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        BasePageResponse<ListEventResponse> response = eventService.getEvents(filter);
        return ResponseEntity.ok(BaseResponse.ok("lấy danh sách thành công", response));
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<BaseResponse<EventResponse>> getEventById(@PathVariable Long eventId) {
        return ResponseEntity
                .ok(BaseResponse.ok("Lấy thông tin thành công" , eventService.getEventById(eventId)));
    }

    @PostMapping
    public ResponseEntity<BaseResponse<EventResponse>> createEvent(@Valid @RequestBody CreateEventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.ok("tạo event thành công" ,eventService.createEvent(request)));
    }

    @PutMapping("/{eventId}")
    public ResponseEntity<BaseResponse<EventResponse>> updateEvent(
            @PathVariable Long eventId,
            @Valid @RequestBody UpdateEventRequest request) {
        return ResponseEntity
                .ok(BaseResponse.ok("Cập nhật event thành công", eventService.updateEvent(eventId, request)));
    }

    @DeleteMapping("/{eventId}")
    public ResponseEntity<BaseResponse<Boolean>> deleteEvent(@PathVariable Long eventId) {
        return ResponseEntity
                .ok(BaseResponse.ok("success", eventService.deleteEvent(eventId)));
    }
}