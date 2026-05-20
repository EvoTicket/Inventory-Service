package com.capstone.inventoryservice.domain.controller;

import com.capstone.inventoryservice.domain.dto.BasePageResponse;
import com.capstone.inventoryservice.domain.dto.BaseResponse;
import com.capstone.inventoryservice.domain.dto.request.*;
import com.capstone.inventoryservice.domain.dto.response.EventResponse;
import com.capstone.inventoryservice.domain.dto.response.HomepageResponse;
import com.capstone.inventoryservice.domain.dto.response.ListEventResponse;
import com.capstone.inventoryservice.domain.dto.response.OrgEventDto;
import com.capstone.inventoryservice.domain.dto.response.TrendingEventResponse;
import com.capstone.inventoryservice.model.enums.EventApprovalStatus;
import com.capstone.inventoryservice.model.enums.EventStatus;
import com.capstone.inventoryservice.model.enums.EventType;
import com.capstone.inventoryservice.model.enums.EventCategory;
import com.capstone.inventoryservice.model.enums.TicketAvailabilityStatus;
import com.capstone.inventoryservice.model.enums.EventSortOption;
import com.capstone.inventoryservice.domain.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class  EventController {

    private final EventService eventService;

    @GetMapping
    @Operation(summary = "Get all events with filters",
            description = "Get paginated list of events with search, filter and sort options")
    public ResponseEntity<BaseResponse<BasePageResponse<ListEventResponse>>> getEvents(
            @Parameter(description = "Search keyword (event name, description, venue)")
            @RequestParam(required = false) String keyword,

            @Parameter(description = "Filter by multiple categories")
            @RequestParam(required = false) List<EventCategory> categories,

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

            @Parameter(description = "Minimum price")
            @RequestParam(required = false) java.math.BigDecimal minPrice,

            @Parameter(description = "Maximum price")
            @RequestParam(required = false) java.math.BigDecimal maxPrice,

            @Parameter(description = "Filter by multiple ticket availability statuses (AVAILABLE, ALMOST_SOLD_OUT, SOLD_OUT)")
            @RequestParam(required = false) List<TicketAvailabilityStatus> ticketAvailabilityStatuses,

            @Parameter(description = "Include expired events (default: true)")
            @RequestParam(required = false, defaultValue = "true") Boolean includeExpired,

            @Parameter(description = "Page number (1-indexed)")
            @RequestParam(required = false, defaultValue = "1") Integer page,

            @Parameter(description = "Page size (max: 100)")
            @RequestParam(required = false, defaultValue = "20") Integer size,

            @Parameter(description = "Sort option (PRICE_ASC, PRICE_DESC, DATE_ASC, NEWEST, POPULAR)")
            @RequestParam(required = false, defaultValue = "NEWEST") EventSortOption sort
    ) {
        EventFilterRequest filter = EventFilterRequest.builder()
                .keyword(keyword)
                .categories(categories)
                .eventTypes(eventTypes)
                .eventStatuses(eventStatuses)
                .provinceCodes(provinceCodes)
                .isFeatured(isFeatured)
                .startDate(startDate)
                .endDate(endDate)
                .eventDate(eventDate)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .ticketAvailabilityStatuses(ticketAvailabilityStatuses)
                .includeExpired(includeExpired)
                .page(page - 1)
                .size(size)
                .sort(sort)
                .build();

        BasePageResponse<ListEventResponse> response = eventService.getEvents(filter);
        return ResponseEntity.ok(BaseResponse.ok("lấy danh sách thành công", response));
    }

    @GetMapping("/trending")
    @Operation(summary = "Get trending events", description = "Get list of trending events ordered by favorites and start time")
    public ResponseEntity<BaseResponse<BasePageResponse<TrendingEventResponse>>> getTrendingEvents(
            @Parameter(description = "Limit number of events returned")
            @RequestParam(required = false, defaultValue = "10") Integer limit
    ) {
        BasePageResponse<TrendingEventResponse> response = eventService.getTrendingEvents(limit);
        return ResponseEntity.ok(BaseResponse.ok("Lấy danh sách sự kiện nổi bật thành công", response));
    }

    @GetMapping("/homepage")
    @Operation(summary = "Get homepage events", description = "Get events grouped by sections for the homepage")
    public ResponseEntity<BaseResponse<HomepageResponse>> getHomepageEvents() {
        HomepageResponse response = eventService.getHomepageEvents();
        return ResponseEntity.ok(BaseResponse.ok("Lấy thông tin trang chủ thành công", response));
    }

    @GetMapping("/my")
    @Operation(summary = "Get events by organizer",
            description = "Get paginated list of events filtered by organizer ID and optional status")
    public ResponseEntity<BasePageResponse<ListEventResponse>> getEventsByOrganizer(

            @Parameter(description = "Event status filter (optional)")
            @RequestParam(required = false) EventStatus status,

            @Parameter(description = "Page number (1-indexed)")
            @RequestParam(defaultValue = "1") int page,

            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") int size,

            @Parameter(description = "Sort field")
            @RequestParam(defaultValue = "createdAt") String sortBy,

            @Parameter(description = "Sort direction (ASC or DESC)")
            @RequestParam(defaultValue = "DESC") String sortDirection
    ) {
        Sort.Direction direction = sortDirection.equalsIgnoreCase("ASC")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(direction, sortBy));

        BasePageResponse<ListEventResponse> response = eventService.getEventsByOrganizer(
                 status, pageable
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/organizer/dashboard")
    @Operation(summary = "Get events for organization",
            description = "Get paginated list of events with statistics and filters for an organization")
    public ResponseEntity<BaseResponse<OrgEventDto>> getEventsForOrg(
            @Parameter(description = "Search keyword (event name, description, venue)")
            @RequestParam(required = false) String keyword,

            @Parameter(description = "Filter by multiple categories")
            @RequestParam(required = false) List<EventCategory> categories,

            @Parameter(description = "Filter by multiple event types")
            @RequestParam(required = false) List<EventType> eventTypes,

            @Parameter(description = "Filter by multiple event statuses")
            @RequestParam(required = false) List<EventStatus> eventStatuses,

            @Parameter(description = "Filter by multiple approval statuses")
            @RequestParam(required = false) List<EventApprovalStatus> approvalStatuses,

            @Parameter(description = "Filter by start date (YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,

            @Parameter(description = "Filter by end date (YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,

            @Parameter(description = "Page number (1-indexed)")
            @RequestParam(required = false, defaultValue = "1") Integer page,

            @Parameter(description = "Page size (max: 100)")
            @RequestParam(required = false, defaultValue = "20") Integer size,

            @Parameter(description = "Sort option (PRICE_ASC, PRICE_DESC, DATE_ASC, NEWEST, POPULAR)")
            @RequestParam(required = false, defaultValue = "NEWEST") EventSortOption sort
    ) {
        EventFilterRequest filter = EventFilterRequest.builder()
                .keyword(keyword)
                .categories(categories)
                .eventTypes(eventTypes)
                .eventStatuses(eventStatuses)
                .approvalStatuses(approvalStatuses)
                .startDate(startDate)
                .endDate(endDate)
                .page(page - 1)
                .size(size)
                .sort(sort)
                .build();

        OrgEventDto response = eventService.getOrgEvents(filter);
        return ResponseEntity.ok(BaseResponse.ok("Lấy danh sách sự kiện thành công", response));
    }

    @GetMapping({"/recommend", "/recommended"})
    @Operation(summary = "Get recommended events for current user",
            description = "Get personalized event recommendations based on user's views, favorites, and purchase history")
    public ResponseEntity<BaseResponse<java.util.List<ListEventResponse>>> getRecommendedEvents(
            @Parameter(description = "Number of recommendations to return (default: 4)")
            @RequestParam(required = false, defaultValue = "4") Integer limit,
            @Parameter(description = "Event ID to exclude from recommendations")
            @RequestParam(required = false) Long eventId
    ) {
        java.util.List<ListEventResponse> recommendations = eventService.getRecommendedEvents(limit, eventId);
        return ResponseEntity.ok(BaseResponse.ok("Lấy gợi ý thành công", recommendations));
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<BaseResponse<EventResponse>> getEventById(@PathVariable Long eventId) {
        return ResponseEntity
                .ok(BaseResponse.ok("Lấy thông tin thành công" , eventService.getEventById(eventId)));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BaseResponse<Boolean>> createEvent(
            @Valid
            @RequestPart("event")
            @Parameter(
                    description = "Event JSON",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CreateEventRequest.class)
                    )
            )
            CreateEventRequest request,

            @RequestPart(value = "bannerImage", required = false)
            @Parameter(description = "Banner image", content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    schema = @Schema(type = "string", format = "binary")))
            MultipartFile bannerImage,

            @RequestPart(value = "thumbnailImage", required = false)
            @Parameter(description = "Thumbnail image", content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    schema = @Schema(type = "string", format = "binary")))
            MultipartFile thumbnailImage,

            @RequestPart(value = "seatMapImage", required = false)
            @Parameter(description = "Seat map image", content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    schema = @Schema(type = "string", format = "binary")))
            MultipartFile seatMapImage
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.ok("tạo event thành công" ,eventService.createEvent(request, bannerImage, thumbnailImage, seatMapImage)));
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

    @PostMapping(value = "/draft", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BaseResponse<EventResponse>> createDraftStep1(
            @Valid
            @RequestPart("event")
            @Parameter(
                    description = "Event Step 1 JSON",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CreateDraftStep1Request.class)
                    )
            )
            CreateDraftStep1Request request,

            @RequestPart(value = "bannerImage", required = false)
            @Parameter(description = "Banner image", content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    schema = @Schema(type = "string", format = "binary")))
            MultipartFile bannerImage,

            @RequestPart(value = "thumbnailImage", required = false)
            @Parameter(description = "Thumbnail image", content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    schema = @Schema(type = "string", format = "binary")))
            MultipartFile thumbnailImage
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.ok("Tạo bản nháp thành công", eventService.createDraftStep1(request, bannerImage, thumbnailImage)));
    }

    @PutMapping(value = "/{eventId}/draft/step-1", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BaseResponse<EventResponse>> updateDraftStep1(
            @PathVariable Long eventId,
            @Valid
            @RequestPart("event")
            @Parameter(
                    description = "Event Step 1 JSON",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CreateDraftStep1Request.class)
                    )
            )
            CreateDraftStep1Request request,

            @RequestPart(value = "bannerImage", required = false)
            @Parameter(description = "Banner image", content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    schema = @Schema(type = "string", format = "binary")))
            MultipartFile bannerImage,

            @RequestPart(value = "thumbnailImage", required = false)
            @Parameter(description = "Thumbnail image", content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    schema = @Schema(type = "string", format = "binary")))
            MultipartFile thumbnailImage
    ) {
        return ResponseEntity.ok(BaseResponse.ok("Cập nhật bước 1 thành công", eventService.updateDraftStep1(eventId, request, bannerImage, thumbnailImage)));
    }

    @PutMapping(value = "/{eventId}/draft/step-2", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BaseResponse<EventResponse>> updateDraftStep2(
            @PathVariable Long eventId,
            @Valid
            @RequestPart("event")
            @Parameter(
                    description = "Event Step 2 JSON",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UpdateDraftStep2Request.class)
                    )
            )
            UpdateDraftStep2Request request,

            @RequestPart(value = "seatMapImage", required = false)
            @Parameter(description = "Seat map image", content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    schema = @Schema(type = "string", format = "binary")))
            MultipartFile seatMapImage
    ) {
        return ResponseEntity.ok(BaseResponse.ok("Cập nhật bước 2 thành công", eventService.updateDraftStep2(eventId, request, seatMapImage)));
    }

    @PutMapping("/{eventId}/draft/step-3")
    public ResponseEntity<BaseResponse<EventResponse>> updateDraftStep3(
            @PathVariable Long eventId,
            @Valid @RequestBody UpdateDraftStep3Request request
    ) {
        return ResponseEntity.ok(BaseResponse.ok("Cập nhật bước 3 thành công", eventService.updateDraftStep3(eventId, request)));
    }

    @PutMapping("/{eventId}/draft/step-4")
    public ResponseEntity<BaseResponse<EventResponse>> updateDraftStep4(
            @PathVariable Long eventId,
            @Valid @RequestBody UpdateDraftStep4Request request
    ) {
        return ResponseEntity.ok(BaseResponse.ok("Cập nhật bước 4 thành công", eventService.updateDraftStep4(eventId, request)));
    }

    @PostMapping("/{eventId}/publish")
    public ResponseEntity<BaseResponse<EventResponse>> publishEvent(
            @PathVariable Long eventId
    ) {
        return ResponseEntity.ok(BaseResponse.ok("Xuất bản sự kiện thành công", eventService.publishEvent(eventId)));
    }

    @GetMapping("/{eventId}/draft")
    public ResponseEntity<BaseResponse<EventResponse>> getEventDraft(
            @PathVariable Long eventId
    ) {
        return ResponseEntity.ok(BaseResponse.ok("Lấy thông tin bản nháp thành công", eventService.getEventDraft(eventId)));
    }

}