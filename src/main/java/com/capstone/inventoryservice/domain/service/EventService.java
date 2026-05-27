package com.capstone.inventoryservice.domain.service;

import com.capstone.inventoryservice.domain.client.IAMFeignClient;
import com.capstone.inventoryservice.domain.client.OrgInternalResponse;
import com.capstone.inventoryservice.domain.client.BankInfoInternalResponse;
import com.capstone.inventoryservice.domain.client.OrderFeignClient;
import com.capstone.inventoryservice.domain.dto.BasePageResponse;
import com.capstone.inventoryservice.domain.dto.event.TicketCreatedEvent;
import com.capstone.inventoryservice.domain.dto.request.CreateEventRequest;
import com.capstone.inventoryservice.domain.dto.request.CreateShowtimeRequest;
import com.capstone.inventoryservice.domain.dto.request.CreateTicketTypeRequest;
import com.capstone.inventoryservice.domain.dto.request.EventFilterRequest;
import com.capstone.inventoryservice.domain.dto.request.UpdateEventRequest;
import com.capstone.inventoryservice.domain.dto.request.CreateDraftStep1Request;
import com.capstone.inventoryservice.domain.dto.request.UpdateDraftStep2Request;
import com.capstone.inventoryservice.domain.dto.request.UpdateDraftStep3Request;
import com.capstone.inventoryservice.domain.dto.request.UpdateDraftStep4Request;
import com.capstone.inventoryservice.domain.dto.response.*;
import com.capstone.inventoryservice.domain.mapper.ReviewMapper;
import com.capstone.inventoryservice.model.entity.Event;
import com.capstone.inventoryservice.model.entity.EventView;
import com.capstone.inventoryservice.model.entity.Showtime;
import com.capstone.inventoryservice.model.entity.TicketType;
import com.capstone.inventoryservice.exception.AppException;
import com.capstone.inventoryservice.exception.ErrorCode;
import com.capstone.inventoryservice.domain.mapper.TicketTypeMapper;
import com.capstone.inventoryservice.model.enums.*;
import com.capstone.inventoryservice.model.repository.EventRepository;
import com.capstone.inventoryservice.model.repository.EventViewRepository;
import com.capstone.inventoryservice.model.repository.UserFavoriteEventRepository;
import com.capstone.inventoryservice.security.JwtUtil;
import com.capstone.inventoryservice.domain.specification.EventSpecification;
import com.capstone.inventoryservice.domain.util.EventUtil;
import com.capstone.inventoryservice.domain.util.LocationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

    private final EventRepository eventRepository;
    private final EventViewRepository eventViewRepository;
    private final UserFavoriteEventRepository userFavoriteEventRepository;
    private final JwtUtil jwtUtil;
    private final IAMFeignClient iamFeignClient;
    private final OrderFeignClient orderFeignClient;
    private final LocationUtil locationUtil;
    private final EventUtil eventUtil;
    private final TicketTypeMapper ticketTypeMapper;
    private final UploadService uploadService;
    private final ApplicationEventPublisher eventPublisher;
    private final ReviewMapper reviewMapper;

    @Transactional(readOnly = true)
    public BasePageResponse<ListEventResponse> getEvents(EventFilterRequest filter) {
        filter.setApprovalStatuses(List.of(EventApprovalStatus.PUBLISHED));
        Specification<Event> spec = EventSpecification.withFilters(filter);
        Pageable pageable = buildPageable(filter);

        Page<Event> eventPage = eventRepository.findAll(spec, pageable);

        return buildEventPageResponse(eventPage, pageable);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "homepageEvents", key = "'default'")
    public HomepageResponse getHomepageEvents() {
        Pageable pageable = PageRequest.of(0, 4);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneMonthLater = now.plusMonths(1);

        Page<Event> upcomingEvents = eventRepository.findUpcomingEvents(now, oneMonthLater, pageable);
        List<ListEventResponse> upcomingResponses = mapToResponseList(upcomingEvents.getContent());

        Page<Event> livestageEvents = eventRepository.findAcceptedByCategory(EventCategory.LIVESTAGE, pageable);
        List<ListEventResponse> livestageResponses = mapToResponseList(livestageEvents.getContent());

        Page<Event> stageArtEvents = eventRepository.findAcceptedByCategory(EventCategory.STAGE_ART, pageable);
        List<ListEventResponse> stageArtResponses = mapToResponseList(stageArtEvents.getContent());

        Page<Event> workshopEvents = eventRepository.findAcceptedByCategory(EventCategory.WORKSHOP, pageable);
        List<ListEventResponse> workshopResponses = mapToResponseList(workshopEvents.getContent());

        return HomepageResponse.builder()
                .sections(List.of(
                        new HomepageSectionResponse("Sắp diễn ra", "UPCOMING", upcomingResponses),
                        new HomepageSectionResponse("Livestage", "LIVESTAGE", livestageResponses),
                        new HomepageSectionResponse("Sân khấu & Nghệ thuật", "STAGE_ART", stageArtResponses),
                        new HomepageSectionResponse("Hội thảo và Workshop", "WORKSHOP", workshopResponses)
                ))
                .build();
    }

    private List<ListEventResponse> mapToResponseList(List<Event> events) {
        if (events.isEmpty()) return Collections.emptyList();
        
        List<Long> eventIds = events.stream().map(Event::getId).toList();
        Map<Long, Long> favoriteCountMap = getFavoriteCountMap(eventIds);
        
        Long userId = null;
        try {
            var auth = jwtUtil.getDataFromAuth();
            if (auth != null) userId = auth.userId();
        } catch (Exception ignored) {}
        
        Set<Long> userFavoriteEventIds = userId != null ? getUserFavoriteEventIds(userId, eventIds) : Collections.emptySet();
        
        return events.stream()
                .map(e -> ListEventResponse.mapToResponse(e, favoriteCountMap, userFavoriteEventIds))
                .toList();
    }

    private static final double ALPHA = 3.0;
    private static final double BETA = 10.0;
    private static final double GROWTH_CAP = 3.0;
    private static final double BUY_WEIGHT = 0.65;
    private static final double HOT_WEIGHT = 0.35;

    @Transactional(readOnly = true)
    @Cacheable(value = "trendingEvents", key = "#limit")
    public BasePageResponse<TrendingEventResponse> getTrendingEvents(int limit) {
        LocalDateTime now = LocalDateTime.now();
        List<Event> candidateEvents = eventRepository.findCandidateEvents(now);

        if (candidateEvents.isEmpty()) {
            return BasePageResponse.fromPage(Page.empty());
        }

        List<Long> eventIds = candidateEvents.stream()
                .map(Event::getId)
                .toList();

        Map<Long, EventVolumeResponse> volumeMap;
        try {
            volumeMap = orderFeignClient.getVolumeForEvents(eventIds);
        } catch (Exception e) {
            log.error("Could not fetch volume data from order-service for trending events", e);
            volumeMap = Collections.emptyMap();
        }

        final Map<Long, EventVolumeResponse> finalVolumeMap = volumeMap;

        // Step 5: Compute buyPowerRaw for each candidate and find min/max
        Map<Long, Double> buyPowerRawMap = new HashMap<>();
        double minBuyPowerRaw = Double.MAX_VALUE;
        double maxBuyPowerRaw = -Double.MAX_VALUE;

        for (Event event : candidateEvents) {
            EventVolumeResponse volumeData = finalVolumeMap.get(event.getId());
            double revenueToday = 0.0;
            if (volumeData != null && volumeData.getRevenueToday() != null) {
                revenueToday = volumeData.getRevenueToday().doubleValue();
            }
            double buyPowerRaw = Math.log1p(revenueToday);
            buyPowerRawMap.put(event.getId(), buyPowerRaw);
            if (buyPowerRaw < minBuyPowerRaw) {
                minBuyPowerRaw = buyPowerRaw;
            }
            if (buyPowerRaw > maxBuyPowerRaw) {
                maxBuyPowerRaw = buyPowerRaw;
            }
        }

        // Step 12 & 13: Compute trending score for each event
        Map<Long, Double> trendingScoreMap = new HashMap<>();
        for (Event event : candidateEvents) {
            double buyPowerRaw = buyPowerRawMap.get(event.getId());
            double buyPowerScore = 0.5;
            if (maxBuyPowerRaw > minBuyPowerRaw) {
                buyPowerScore = (buyPowerRaw - minBuyPowerRaw) / (maxBuyPowerRaw - minBuyPowerRaw);
            }

            EventVolumeResponse volumeData = finalVolumeMap.get(event.getId());
            long ticketsToday = 0;
            long ticketsYesterday = 0;
            if (volumeData != null) {
                if (volumeData.getTicketsToday() != null) {
                    ticketsToday = volumeData.getTicketsToday();
                }
                if (volumeData.getTicketsYesterday() != null) {
                    ticketsYesterday = volumeData.getTicketsYesterday();
                }
            }

            double smoothedGrowth = (double) (ticketsToday + ALPHA) / (ticketsYesterday + ALPHA) - 1.0;
            double cappedGrowth = Math.max(0.0, Math.min(GROWTH_CAP, smoothedGrowth));
            double hotScoreBase = cappedGrowth / GROWTH_CAP;
            double confidence = (double) ticketsToday / (ticketsToday + BETA);
            double hotScore = hotScoreBase * confidence;

            double trendingScore = 100.0 * (BUY_WEIGHT * buyPowerScore + HOT_WEIGHT * hotScore);
            trendingScoreMap.put(event.getId(), trendingScore);
        }

        // Step 14: Sort candidateEvents
        // Sort theo trendingScore DESC. Nếu bằng nhau: revenueToday DESC, sau đó ticketsToday DESC, sau đó eventStartTime ASC.
        List<Event> sortedEvents = new ArrayList<>(candidateEvents);
        sortedEvents.sort((e1, e2) -> {
            double score1 = trendingScoreMap.get(e1.getId());
            double score2 = trendingScoreMap.get(e2.getId());
            int cmp = Double.compare(score2, score1); // DESC
            if (cmp != 0) return cmp;

            EventVolumeResponse v1 = finalVolumeMap.get(e1.getId());
            EventVolumeResponse v2 = finalVolumeMap.get(e2.getId());
            double rev1 = (v1 != null && v1.getRevenueToday() != null) ? v1.getRevenueToday().doubleValue() : 0.0;
            double rev2 = (v2 != null && v2.getRevenueToday() != null) ? v2.getRevenueToday().doubleValue() : 0.0;
            cmp = Double.compare(rev2, rev1); // DESC
            if (cmp != 0) return cmp;

            long tToday1 = (v1 != null && v1.getTicketsToday() != null) ? v1.getTicketsToday() : 0L;
            long tToday2 = (v2 != null && v2.getTicketsToday() != null) ? v2.getTicketsToday() : 0L;
            cmp = Long.compare(tToday2, tToday1); // DESC
            if (cmp != 0) return cmp;

            LocalDateTime t1 = e1.getEarliestStart();
            LocalDateTime t2 = e2.getEarliestStart();
            if (t1 == null && t2 == null) return 0;
            if (t1 == null) return 1;
            if (t2 == null) return -1;
            return t1.compareTo(t2); // ASC
        });

        int actualLimit = Math.min(limit, sortedEvents.size());
        List<Event> limitedEvents = sortedEvents.subList(0, actualLimit);

        List<TrendingEventResponse> dtoList = limitedEvents.stream().map(event -> {
            String organizerName = "Unknown";
            if (event.getOrganizerId() != null) {
                try {
                    OrgInternalResponse orgResponse = iamFeignClient.getOrganizationById(event.getOrganizerId());
                    if (orgResponse != null) {
                        organizerName = orgResponse.getOrganizationName();
                    }
                } catch (Exception e) {
                    log.error("Could not fetch organizer name for event: {}", event.getId(), e);
                }
            }

            EventVolumeResponse volumeData = finalVolumeMap.get(event.getId());
            BigDecimal volume24h = volumeData != null && volumeData.getVolume24h() != null
                    ? volumeData.getVolume24h()
                    : BigDecimal.ZERO;
            Double hotness = volumeData != null && volumeData.getHotness() != null
                    ? volumeData.getHotness()
                    : 0.0;

            return TrendingEventResponse.builder()
                    .id(event.getId())
                    .eventName(event.getEventName())
                    .thumbnailImage(event.getThumbnailImage())
                    .organizerName(organizerName)
                    .floorPrice(event.getFloorPrice())
                    .volume24h(volume24h)
                    .hotness(hotness)
                    .ticketAvailabilityStatus(event.getTicketAvailabilityStatus())
                    .build();
        }).toList();

        Pageable resultPageable = PageRequest.of(0, limit);
        Page<TrendingEventResponse> dtoPage = new PageImpl<>(dtoList, resultPageable, sortedEvents.size());

        return BasePageResponse.fromPage(dtoPage);
    }

    private Map<Long, Long> getFavoriteCountMap(List<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Object[]> results = eventRepository.countFavoritesByEventIds(eventIds);

        return results.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1],
                        (existing, replacement) -> existing
                ));
    }

    private Set<Long> getUserFavoriteEventIds(Long userId, List<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return Collections.emptySet();
        }

        List<Long> favoriteIds = eventRepository.findFavoriteEventIdsByUserId(userId, eventIds);
        return new HashSet<>(favoriteIds);
    }

    private Pageable buildPageable(EventFilterRequest filter) {
        int page = filter.getPage() != null && filter.getPage() >= 0 ? filter.getPage() : 0;
        int size = filter.getSize() != null && filter.getSize() > 0 && filter.getSize() <= 100
                ? filter.getSize() : 20;

        EventSortOption sortOption = filter.getSort() != null ? filter.getSort() : EventSortOption.NEWEST;
        Sort sort = sortOption.getSort();

        return PageRequest.of(page, size, sort);
    }

    @Transactional
    @Cacheable(value = "eventDetails", key = "#eventId")
    public EventResponse getEventById(Long eventId) {
        Event event = eventUtil.getEventOrElseThrow(eventId);
        if (event.getApprovalStatus() != EventApprovalStatus.PUBLISHED) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Event not found with id: " + eventId);
        }
        recordView(event);
        return convertToDTO(event);
    }

    @Transactional(readOnly = true)
    public EventResponse getEventByIdForAdmin(Long eventId) {
        Event event = eventUtil.getEventOrElseThrow(eventId);
        return convertToDTO(event);
    }

    private void recordView(Event event) {
        Long userId = null;
        try {
            var auth = jwtUtil.getDataFromAuth();
            if (auth != null) userId = auth.userId();
        } catch (Exception ignored) {}

        if (userId != null) {
            LocalDateTime tenMinsAgo = LocalDateTime.now().minusMinutes(10);
            long recentViews = eventViewRepository.countRecentViewsByUser(event.getId(), userId, tenMinsAgo);
            if (recentViews > 0) return;
        }

        EventView view = EventView.builder()
            .event(event)
            .userId(userId)
            .build();
        eventViewRepository.save(view);
    }

    @Caching(evict = {
        @CacheEvict(value = "homepageEvents", allEntries = true),
        @CacheEvict(value = "trendingEvents", allEntries = true)
    })
    public Boolean createEvent(CreateEventRequest request, MultipartFile bannerFile, MultipartFile thumbnailFile, MultipartFile seatMapFile) {

        Long orgId = jwtUtil.getDataFromAuth().organizationId();
        if(orgId == null) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Org id is null");
        }

        Event event = Event.builder()
                .eventName(request.getEventName())
                .description(request.getDescription())
                .venue(request.getVenue())
                .address(request.getAddress())
                .eventType(request.getEventType())
                .totalSeats(request.getTotalSeats())
                .introduction(request.getIntroduction())
                .organizerId(orgId)
                .bankInfoId(request.getBankInfoId())
                .isFeatured(request.getIsFeatured() != null && request.getIsFeatured())
                .approvalStatus(EventApprovalStatus.PENDING_REVIEW)
                .currentStep(5L)
                .category(request.getCategory())
                .province(locationUtil.getProvinceByCode(request.getProvinceCode()))
                .ward(locationUtil.getWardByCode(request.getWardCode()))
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .seatMapImage(null)
                .shortDescription(request.getShortDescription())
                .contactEmail(request.getContactEmail())
                .contactPhone(request.getContactPhone())
                .allowMultipleTicketTypesPerOrder(request.getAllowMultipleTicketTypesPerOrder() != null && request.getAllowMultipleTicketTypesPerOrder())
                .allowDiscountCode(request.getAllowDiscountCode() != null && request.getAllowDiscountCode())
                .allowResale(request.getAllowResale() != null && request.getAllowResale())
                .maxResalePricePercentage(request.getMaxResalePricePercentage() != null ? request.getMaxResalePricePercentage().divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP) : null)
                .organizerRoyaltyFeePercentage(request.getOrganizerRoyaltyFeePercentage() != null ? request.getOrganizerRoyaltyFeePercentage().divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP) : null)
                .postPurchaseInstruction(request.getPostPurchaseInstruction())
                .checkInInstruction(request.getCheckInInstruction())
                .entryGateInstruction(request.getEntryGateInstruction())
                .reconciliationNote(request.getReconciliationNote())
                .build();

        List<TicketType> allTicketTypes = new ArrayList<>();
        if (request.getShowtimes() != null && !request.getShowtimes().isEmpty()) {
            for (CreateShowtimeRequest showtimeRequest : request.getShowtimes()) {
                if (showtimeRequest.getEndDatetime().isBefore(showtimeRequest.getStartDatetime())) {
                    throw new AppException(ErrorCode.BAD_REQUEST, "Showtime end datetime must be after start datetime");
                }

                Showtime showtime = Showtime.builder()
                        .event(event)
                        .startDatetime(showtimeRequest.getStartDatetime())
                        .endDatetime(showtimeRequest.getEndDatetime())
                        .venue(showtimeRequest.getVenue())
                        .address(showtimeRequest.getAddress())
                        .build();

                if (showtimeRequest.getWardCode() != null) {
                    showtime.setWard(locationUtil.getWardByCode(showtimeRequest.getWardCode()));
                }
                if (showtimeRequest.getProvinceCode() != null) {
                    showtime.setProvince(locationUtil.getProvinceByCode(showtimeRequest.getProvinceCode()));
                }

                if (showtimeRequest.getTicketTypes() != null && !showtimeRequest.getTicketTypes().isEmpty()) {
                    for (CreateTicketTypeRequest ticketRequest : showtimeRequest.getTicketTypes()) {
                        TicketType ticketType = TicketType.builder()
                                .typeName(ticketRequest.getTypeName())
                                .description(ticketRequest.getDescription())
                                .price(ticketRequest.getPrice())
                                .quantityTotal(ticketRequest.getQuantityTotal())
                                .quantitySold(0)
                                .minPurchase(ticketRequest.getMinPurchase())
                                .maxPurchase(ticketRequest.getMaxPurchase())
                                .saleStartDate(ticketRequest.getSaleStartDate())
                                .saleEndDate(ticketRequest.getSaleEndDate())
                                .ticketTypeStatus(TicketTypeStatus.AVAILABLE)
                                .showtime(showtime)
                                .build();

                        showtime.getTicketTypes().add(ticketType);
                        allTicketTypes.add(ticketType);
                    }
                }

                event.getShowtimes().add(showtime);
            }
        }
        Event savedEvent = eventRepository.save(event);

        List<CompletableFuture<Void>> uploadTasks = new ArrayList<>();
        if (bannerFile != null && !bannerFile.isEmpty()) {
            try {
                uploadTasks.add(uploadService.uploadImageAsync(savedEvent, bannerFile.getBytes(), "banner"));
            } catch (IOException e) {
                throw new AppException(ErrorCode.IO_EXCEPTION, "Không thể đọc ảnh banner: " + e.getMessage());
            }
        }
        if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
            try {
                uploadTasks.add(uploadService.uploadImageAsync(savedEvent, thumbnailFile.getBytes(), "thumbnail"));
            } catch (IOException e) {
                throw new AppException(ErrorCode.IO_EXCEPTION, "Không thể đọc ảnh thumbnail: " + e.getMessage());
            }
        }
        if (seatMapFile != null && !seatMapFile.isEmpty()) {
            try {
                uploadTasks.add(uploadService.uploadImageAsync(savedEvent, seatMapFile.getBytes(), "seat_map"));
            } catch (IOException e) {
                throw new AppException(ErrorCode.IO_EXCEPTION, "Không thể đọc ảnh seat map: " + e.getMessage());
            }
        }

        CompletableFuture.allOf(uploadTasks.toArray(new CompletableFuture[0])).join();

        eventRepository.save(savedEvent);

        for (Showtime s : savedEvent.getShowtimes()) {
            for (TicketType t : s.getTicketTypes()) {
                eventPublisher.publishEvent(
                        new TicketCreatedEvent(
                                t.getId(),
                                t.getQuantityTotal()
                        )
                );
            }
        }

        return true;
    }

    @Transactional(readOnly = true)
    public BasePageResponse<ListEventResponse> getPendingEventsForAdmin(EventFilterRequest filter) {
        filter.setApprovalStatuses(List.of(EventApprovalStatus.PENDING_REVIEW));
        Specification<Event> spec = EventSpecification.withFilters(filter);
        Pageable pageable = buildPageable(filter);
        Page<Event> eventPage = eventRepository.findAll(spec, pageable);
        return buildEventPageResponse(eventPage, pageable);
    }

    @Transactional(readOnly = true)
    public BasePageResponse<ListEventResponse> getEventsForModeration(EventFilterRequest filter) {
        if (filter.getApprovalStatuses() == null || filter.getApprovalStatuses().isEmpty()) {
            filter.setApprovalStatuses(List.of(EventApprovalStatus.PENDING_REVIEW, EventApprovalStatus.REJECTED));
        }
        Specification<Event> spec = EventSpecification.withFilters(filter);
        Pageable pageable = buildPageable(filter);
        Page<Event> eventPage = eventRepository.findAll(spec, pageable);
        BasePageResponse<ListEventResponse> response = buildEventPageResponse(eventPage, pageable);

        if (response.getContent() != null) {
            for (ListEventResponse dto : response.getContent()) {
                if (dto.getOrganizerId() != null) {
                    try {
                        OrgInternalResponse org = iamFeignClient.getOrganizationById(dto.getOrganizerId());
                        if (org != null) {
                            dto.setOrganizerName(org.getOrganizationName());
                        }
                    } catch (Exception e) {
                        log.error("Error fetching organization name for orgId: {}", dto.getOrganizerId(), e);
                        dto.setOrganizerName("Unknown");
                    }
                }
            }
        }
        return response;
    }

    @Transactional(readOnly = true)
    public EventModerationSummaryResponse getModerationSummary() {
        long pending = eventRepository.countByApprovalStatus(EventApprovalStatus.PENDING_REVIEW);
        long rejected = eventRepository.countByApprovalStatus(EventApprovalStatus.REJECTED);
        return EventModerationSummaryResponse.builder()
                .pendingCount(pending)
                .rejectedCount(rejected)
                .build();
    }


    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "eventDetails", key = "#eventId"),
        @CacheEvict(value = "homepageEvents", allEntries = true),
        @CacheEvict(value = "trendingEvents", allEntries = true)
    })
    public EventResponse updateApprovalStatus(Long eventId, EventApprovalStatus approvalStatus) {
        if (approvalStatus == null || approvalStatus == EventApprovalStatus.PENDING_REVIEW) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Approval status must be PUBLISHED or REJECTED");
        }

        Event event = eventUtil.getEventOrElseThrow(eventId);
        event.setApprovalStatus(approvalStatus);
        Event savedEvent = eventRepository.save(event);
        return convertToDTO(savedEvent);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "eventDetails", key = "#eventId"),
        @CacheEvict(value = "homepageEvents", allEntries = true),
        @CacheEvict(value = "trendingEvents", allEntries = true)
    })
    public EventResponse updateEvent(Long eventId, UpdateEventRequest request) {
        Event event = eventUtil.getEventOrElseThrow(eventId);

        if (request.getEventName() != null) {
            event.setEventName(request.getEventName());
        }
        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }
        if (request.getVenue() != null) {
            event.setVenue(request.getVenue());
        }
        if (request.getAddress() != null) {
            event.setAddress(request.getAddress());
        }
        if (request.getIsCancelled() != null) {
            event.setIsCancelled(request.getIsCancelled());
        }
        if (request.getEventType() != null) {
            event.setEventType(request.getEventType());
        }
        if (request.getTotalSeats() != null) {
            event.setTotalSeats(request.getTotalSeats());
        }
        if (request.getIsFeatured() != null) {
            event.setIsFeatured(request.getIsFeatured());
        }
        if (request.getCategory() != null) {
            event.setCategory(request.getCategory());
        }
        if (request.getLatitude() != null) {
            event.setLatitude(request.getLatitude());
        }
        if (request.getLongitude() != null) {
            event.setLongitude(request.getLongitude());
        }
        if (request.getBankInfoId() != null) {
            event.setBankInfoId(request.getBankInfoId());
        }
        if (request.getShortDescription() != null) {
            event.setShortDescription(request.getShortDescription());
        }
        if (request.getContactEmail() != null) {
            event.setContactEmail(request.getContactEmail());
        }
        if (request.getContactPhone() != null) {
            event.setContactPhone(request.getContactPhone());
        }
        if (request.getAllowMultipleTicketTypesPerOrder() != null) {
            event.setAllowMultipleTicketTypesPerOrder(request.getAllowMultipleTicketTypesPerOrder());
        }
        if (request.getAllowDiscountCode() != null) {
            event.setAllowDiscountCode(request.getAllowDiscountCode());
        }
        if (request.getAllowResale() != null) {
            event.setAllowResale(request.getAllowResale());
        }
        if (request.getMaxResalePricePercentage() != null) {
            event.setMaxResalePricePercentage(request.getMaxResalePricePercentage().divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP));
        }
        if (request.getOrganizerRoyaltyFeePercentage() != null) {
            event.setOrganizerRoyaltyFeePercentage(request.getOrganizerRoyaltyFeePercentage().divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP));
        }
        if (request.getPostPurchaseInstruction() != null) {
            event.setPostPurchaseInstruction(request.getPostPurchaseInstruction());
        }
        if (request.getCheckInInstruction() != null) {
            event.setCheckInInstruction(request.getCheckInInstruction());
        }
        if (request.getEntryGateInstruction() != null) {
            event.setEntryGateInstruction(request.getEntryGateInstruction());
        }
        if (request.getReconciliationNote() != null) {
            event.setReconciliationNote(request.getReconciliationNote());
        }

        Event updatedEvent = eventRepository.save(event);
        log.info("Updated event with ID: {}", eventId);
        return convertToDTO(updatedEvent);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "eventDetails", key = "#eventId"),
        @CacheEvict(value = "homepageEvents", allEntries = true),
        @CacheEvict(value = "trendingEvents", allEntries = true)
    })
    public Boolean deleteEvent(Long eventId) {
        Event event = eventUtil.getEventOrElseThrow(eventId);

        eventRepository.delete(event);
        return true;
    }

    @Transactional(readOnly = true)
    public BasePageResponse<ListEventResponse> getEventsByOrganizer(
            EventStatus eventStatus,
            Pageable pageable
    ) {
        Long organizerId = jwtUtil.getDataFromAuth().organizationId();

        if (eventStatus == null) {
            Page<Event> eventPage = eventRepository.findByOrganizerId(organizerId, pageable);
            return buildEventPageResponse(eventPage, pageable);
        } else {
            List<Event> allEvents = eventRepository.findByOrganizerId(organizerId);
            List<Event> filtered = allEvents.stream()
                    .filter(e -> e.getEventStatus() == eventStatus)
                    .toList();

            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), filtered.size());
            List<Event> sublist;
            if (start > filtered.size()) {
                sublist = Collections.emptyList();
            } else {
                sublist = filtered.subList(start, end);
            }
            Page<Event> eventPage = new PageImpl<>(sublist, pageable, filtered.size());
            return buildEventPageResponse(eventPage, pageable);
        }

    }

    @Transactional(readOnly = true)
    public OrgEventDto getOrgEvents(EventFilterRequest filter) {
        if(!jwtUtil.getDataFromAuth().isOrganization()) {
            throw new AppException(ErrorCode.FORBIDDEN, "Only organization users can access their events");
        }
        Long orgId = jwtUtil.getDataFromAuth().organizationId();
        filter.setOrganizerId(orgId);

        Specification<Event> spec = EventSpecification.withFilters(filter);
        Pageable pageable = buildPageable(filter);

        Page<Event> eventPage = eventRepository.findAll(spec, pageable);

        List<Long> eventIds = eventPage.getContent()
                .stream()
                .map(Event::getId)
                .toList();

        Map<Long, BigDecimal> revenueMap =
                orderFeignClient.getRevenueForEvents(eventIds);

        Page<OrgEventDto.EventResponseDto> dtoPage = eventPage.map(event ->
                OrgEventDto.EventResponseDto.fromEntity(
                        event,
                        revenueMap.get(event.getId())
                )
        );

        long totalEvents = eventRepository.count(EventSpecification.withFilters(EventFilterRequest.builder().organizerId(orgId).build()));
        long totalOnSales = eventRepository.count(EventSpecification.withFilters(EventFilterRequest.builder().organizerId(orgId).eventStatuses(List.of(EventStatus.ON_SALE)).build()));
        long totalCompleted = eventRepository.count(EventSpecification.withFilters(EventFilterRequest.builder().organizerId(orgId).eventStatuses(List.of(EventStatus.COMPLETED)).build()));
        long totalPending = eventRepository.count(EventSpecification.withFilters(EventFilterRequest.builder().organizerId(orgId).approvalStatuses(List.of(EventApprovalStatus.PENDING_REVIEW)).build()));

        return OrgEventDto.builder()
                .totalEvents(totalEvents)
                .totalOnSales(totalOnSales)
                .totalPending(totalPending)
                .totalCompleted(totalCompleted)
                .events(BasePageResponse.fromPage(dtoPage))
                .build();
    }

    public EventResponse convertToDTO(Event event) {
        List<ShowtimeResponse> showtimeDTOs = null;
        if (event.getShowtimes() != null) {
            showtimeDTOs = event.getShowtimes().stream()
                    .map(this::convertShowtimeToDTO)
                    .sorted(Comparator.comparing(ShowtimeResponse::getStartDatetime))
                    .toList();
        }
        List<ReviewResponse> reviewDTOs = null;
        if (event.getReviews() != null) {
            reviewDTOs = event.getReviews().stream()
                    .map(reviewMapper::mapToResponse)
                    .toList();
        }

        if(event.getOrganizerId() == null) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "OrganizerId is null");
        }
        OrgInternalResponse orgInternalResponse = iamFeignClient.getOrganizationById(event.getOrganizerId());

        BankInfoInternalResponse bankInfo = null;
        if (event.getBankInfoId() != null) {
            try {
                bankInfo = iamFeignClient.getBankInfoById(event.getBankInfoId());
            } catch (Exception e) {
                log.error("Could not fetch bank info for event: {}", event.getId(), e);
            }
        }

        return EventResponse.builder()
                .eventId(event.getId())
                .eventName(event.getEventName())
                .orgInternalResponse(orgInternalResponse)
                .description(event.getDescription())
                .venue(event.getVenue())
                .address(event.getFullAddress())
                .detailAddress(event.getAddress())
                .wardCode(event.getWard() != null ? event.getWard().getCode() : null)
                .provinceCode(event.getProvince() != null ? event.getProvince().getCode() : null)
                .eventStatus(event.getEventStatus())
                .approvalStatus(event.getApprovalStatus())
                .currentStep(event.getCurrentStep())
                .eventType(event.getEventType())
                .bannerImage(event.getBannerImage())
                .thumbnailImage(event.getThumbnailImage())
                .introduction(event.getIntroduction())
                .seatMapImage(event.getSeatMapImage())
                .totalSeats(event.getTotalSeats())
                .organizerId(event.getOrganizerId())
                .bankInfoId(event.getBankInfoId())
                .bankInfo(bankInfo)
                .isFeatured(event.getIsFeatured())
                .category(event.getCategory())
                .latitude(event.getLatitude())
                .longitude(event.getLongitude())
                .shortDescription(event.getShortDescription())
                .contactEmail(event.getContactEmail())
                .contactPhone(event.getContactPhone())
                .allowMultipleTicketTypesPerOrder(event.getAllowMultipleTicketTypesPerOrder())
                .allowDiscountCode(event.getAllowDiscountCode())
                .allowResale(event.getAllowResale())
                .maxResalePricePercentage(event.getMaxResalePricePercentage() != null ? event.getMaxResalePricePercentage().multiply(BigDecimal.valueOf(100)) : null)
                .organizerRoyaltyFeePercentage(event.getOrganizerRoyaltyFeePercentage() != null ? event.getOrganizerRoyaltyFeePercentage().multiply(BigDecimal.valueOf(100)) : null)
                .postPurchaseInstruction(event.getPostPurchaseInstruction())
                .checkInInstruction(event.getCheckInInstruction())
                .entryGateInstruction(event.getEntryGateInstruction())
                .reconciliationNote(event.getReconciliationNote())
                .showtimes(showtimeDTOs)
                .reviews(reviewDTOs)
                .build();
    }

    public ShowtimeResponse convertShowtimeToDTO(Showtime showtime) {
        List<TicketTypeResponse> ticketTypeDTOs = null;
        if (showtime.getTicketTypes() != null) {
            ticketTypeDTOs = showtime.getTicketTypes().stream()
                    .map(ticketTypeMapper::convertToDTO)
                    .toList();
        }

        String provinceName = showtime.getProvince() != null ? showtime.getProvince().getName() : null;

        return ShowtimeResponse.builder()
                .showtimeId(showtime.getId())
                .startDatetime(showtime.getStartDatetime())
                .endDatetime(showtime.getEndDatetime())
                .venue(showtime.getVenue())
                .address(showtime.getAddress())
                .fullAddress(showtime.getFullAddress())
                .wardCode(showtime.getWard() != null ? showtime.getWard().getCode() : null)
                .provinceCode(showtime.getProvince() != null ? showtime.getProvince().getCode() : null)
                .provinceName(provinceName)
                .isCancelled(showtime.getIsCancelled())
                .ticketTypes(ticketTypeDTOs)
                .build();
    }

    private BasePageResponse<ListEventResponse> buildEventPageResponse(
            Page<Event> eventPage,
            Pageable pageable
    ) {
        if (eventPage.isEmpty()) {
            return BasePageResponse.fromPage(Page.empty(pageable));
        }

        List<Long> eventIds = eventPage.getContent().stream()
                .map(Event::getId)
                .toList();

        Map<Long, Long> favoriteCountMap = getFavoriteCountMap(eventIds);

        Long currentUserId = jwtUtil.getDataFromAuth().userId();
        Set<Long> userFavoriteEventIds = currentUserId != null
                ? getUserFavoriteEventIds(currentUserId, eventIds)
                : Collections.emptySet();

        Page<ListEventResponse> dtoPage = eventPage.map(
                event -> ListEventResponse.mapToResponse(event, favoriteCountMap, userFavoriteEventIds)
        );

        return BasePageResponse.fromPage(dtoPage);
    }

    @Transactional(readOnly = true)
    public List<ListEventResponse> getRecommendedEvents(int limit, Long excludedEventId) {
        Long userId = null;
        try {
            var auth = jwtUtil.getDataFromAuth();
            if (auth != null) userId = auth.userId();
        } catch (Exception ignored) {
            log.info("User not authenticated, returning fallback recommendations");
        }

        if (userId == null) {
            return getFallbackRecommendations(limit, excludedEventId);
        }

        List<Long> viewedEventIds = eventViewRepository.findViewedEventIdsByUserId(userId);
        List<Long> favoritedEventIds = userFavoriteEventRepository.findFavoritedEventIdsByUserId(userId);
        List<Long> purchasedEventIds;
        try {
            purchasedEventIds = orderFeignClient.getPurchasedEventIdsByUserId(userId);
        } catch (Exception e) {
            log.warn("Could not fetch purchased events from order-service", e);
            purchasedEventIds = Collections.emptyList();
        }

        Set<Long> knownEventIds = new HashSet<>();
        knownEventIds.addAll(viewedEventIds);
        knownEventIds.addAll(favoritedEventIds);
        knownEventIds.addAll(purchasedEventIds);
        if (excludedEventId != null) {
            knownEventIds.add(excludedEventId);
        }

        if (knownEventIds.isEmpty()) {
            return getFallbackRecommendations(limit, null);
        }

        Map<EventCategory, Integer> categoryScores = new EnumMap<>(EventCategory.class);
        Map<String, Integer> provinceScores = new HashMap<>();
        Map<Long, Integer> organizerScores = new HashMap<>();
        Map<EventType, Integer> eventTypeScores = new EnumMap<>(EventType.class);

        List<Event> interactedEvents = eventRepository.findAllById(knownEventIds);

        for (Event event : interactedEvents) {
            int weight = 1;
            if (favoritedEventIds.contains(event.getId())) weight += 2;
            if (purchasedEventIds.contains(event.getId())) weight += 3;

            if (event.getCategory() != null) {
                categoryScores.merge(event.getCategory(), weight, Integer::sum);
            }
            if (event.getProvince() != null) {
                provinceScores.merge(event.getProvince().getCode().toString(), weight, Integer::sum);
            }
            if (event.getOrganizerId() != null) {
                organizerScores.merge(event.getOrganizerId(), weight, Integer::sum);
            }
            if (event.getEventType() != null) {
                eventTypeScores.merge(event.getEventType(), weight, Integer::sum);
            }
        }

        if (categoryScores.isEmpty() && provinceScores.isEmpty() && organizerScores.isEmpty()) {
            return getFallbackRecommendations(limit, excludedEventId);
        }

        LocalDateTime now = LocalDateTime.now();
        List<Event> candidates = eventRepository.findAll(
                Specification.where(
                        EventSpecification.withFilters(
                                EventFilterRequest.builder()
                                        .approvalStatuses(List.of(EventApprovalStatus.PUBLISHED))
                                        .includeExpired(false)
                                        .ticketAvailabilityStatuses(List.of(TicketAvailabilityStatus.AVAILABLE))
                                        .build()
                        )
                ),
                PageRequest.of(0, 100)
        ).getContent();

        List<Event> scoredCandidates = candidates.stream()
                .filter(e -> !knownEventIds.contains(e.getId()))
                .filter(e -> e.getLatestEnd() == null || e.getLatestEnd().isAfter(now))
                .filter(e -> !Boolean.TRUE.equals(e.getIsCancelled()))
                .map(e -> {
                    int score = 0;
                    if (e.getCategory() != null && categoryScores.containsKey(e.getCategory())) {
                        score += categoryScores.get(e.getCategory()) * 3;
                    }
                    if (e.getProvince() != null && provinceScores.containsKey(e.getProvince().getCode().toString())) {
                        score += provinceScores.get(e.getProvince().getCode().toString()) * 2;
                    }
                    if (e.getOrganizerId() != null && organizerScores.containsKey(e.getOrganizerId())) {
                        score += organizerScores.get(e.getOrganizerId()) * 2;
                    }
                    if (e.getEventType() != null && eventTypeScores.containsKey(e.getEventType())) {
                        score += eventTypeScores.get(e.getEventType());
                    }
                    if (Boolean.TRUE.equals(e.getIsFeatured())) {
                        score += 1;
                    }
                    return new ScoredEvent(e, score);
                })
                .filter(se -> se.score > 0)
                .sorted((a, b) -> Integer.compare(b.score, a.score))
                .limit(limit)
                .map(se -> se.event)
                .toList();

        if (scoredCandidates.isEmpty()) {
            return getFallbackRecommendations(limit, excludedEventId);
        }

        List<ListEventResponse> recommendations = mapToResponseList(scoredCandidates);
        if(scoredCandidates.size() < limit){
            recommendations.addAll(getFallbackRecommendations(limit - scoredCandidates.size(), excludedEventId));
        }

        return recommendations;
    }

    private List<ListEventResponse> getFallbackRecommendations(int limit, Long excludedEventId) {
        Pageable pageable = PageRequest.of(0, limit + (excludedEventId != null ? 1 : 0));
        LocalDateTime now = LocalDateTime.now();
        Page<Event> upcomingEvents = eventRepository.findUpcomingEvents(now, now.plusMonths(3), pageable);
        
        List<Event> result = upcomingEvents.getContent().stream()
                .filter(e -> excludedEventId == null || !e.getId().equals(excludedEventId))
                .limit(limit)
                .toList();
                
        return mapToResponseList(result);
    }

    private record ScoredEvent(Event event, int score) { }

    @Transactional(readOnly = true)
    public CountDraftEventDto countCurrentDraftEvent() {
        Long orgId = Optional.ofNullable(jwtUtil.getDataFromAuth().organizationId())
                .orElseThrow(() -> new AppException(ErrorCode.BAD_REQUEST, "Org id is null"));

        long count = eventRepository.countByOrganizerIdAndApprovalStatus(orgId, EventApprovalStatus.DRAFT);

        return CountDraftEventDto.builder()
                .count(count)
                .build();
    }

    @Transactional
    public BasicEventInfoDto createDraftEvent() {
        Long orgId = jwtUtil.getDataFromAuth().organizationId();
        if(orgId == null) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Org id is null");
        }

        Event event = Event.builder()
                .organizerId(orgId)
                .approvalStatus(EventApprovalStatus.DRAFT)
                .currentStep(0L)
                .isFeatured(false)
                .isCancelled(false)
                .build();

        Event savedEvent = eventRepository.save(event);

        return BasicEventInfoDto.convertToDTO(savedEvent);
    }

    @Transactional
    @CacheEvict(value = "eventDetails", key = "#eventId")
    public EventResponse updateDraftStep1(Long eventId, CreateDraftStep1Request request, MultipartFile bannerFile, MultipartFile thumbnailFile) {
        Event event = eventUtil.getEventOrElseThrow(eventId);
        Long orgId = jwtUtil.getDataFromAuth().organizationId();
        if (!event.getOrganizerId().equals(orgId)) {
            throw new AppException(ErrorCode.FORBIDDEN, "Bạn không có quyền sửa bản nháp này");
        }
        if (event.getApprovalStatus() != EventApprovalStatus.DRAFT) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Sự kiện không còn ở trạng thái nháp");
        }

        event.setEventName(request.getEventName());
        event.setIntroduction(request.getIntroduction());
        event.setEventType(request.getEventType());
        event.setCategory(request.getCategory());
        event.setVenue(request.getVenue());
        event.setProvince(request.getProvinceCode() != null ? locationUtil.getProvinceByCode(request.getProvinceCode()) : null);
        event.setWard(request.getWardCode() != null ? locationUtil.getWardByCode(request.getWardCode()) : null);
        event.setAddress(request.getAddress());
        event.setLatitude(request.getLatitude());
        event.setLongitude(request.getLongitude());
        event.setShortDescription(request.getShortDescription());
        event.setDescription(request.getDescription());

        if (event.getCurrentStep() == null || event.getCurrentStep() < 1L) {
            event.setCurrentStep(1L);
        }

        Event savedEvent = eventRepository.save(event);

        List<CompletableFuture<Void>> uploadTasks = new ArrayList<>();
        if (bannerFile != null && !bannerFile.isEmpty()) {
            try {
                uploadTasks.add(uploadService.uploadImageAsync(savedEvent, bannerFile.getBytes(), "banner"));
            } catch (IOException e) {
                throw new AppException(ErrorCode.IO_EXCEPTION, "Không thể đọc ảnh banner: " + e.getMessage());
            }
        }
        if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
            try {
                uploadTasks.add(uploadService.uploadImageAsync(savedEvent, thumbnailFile.getBytes(), "thumbnail"));
            } catch (IOException e) {
                throw new AppException(ErrorCode.IO_EXCEPTION, "Không thể đọc ảnh thumbnail: " + e.getMessage());
            }
        }

        if (!uploadTasks.isEmpty()) {
            CompletableFuture.allOf(uploadTasks.toArray(new CompletableFuture[0])).join();
            savedEvent = eventRepository.save(savedEvent);
        }

        return convertToDTO(savedEvent);
    }

    @Transactional
    @CacheEvict(value = "eventDetails", key = "#eventId")
    public EventResponse updateDraftStep2(Long eventId, UpdateDraftStep2Request request, MultipartFile seatMapFile) {
        Event event = eventUtil.getEventOrElseThrow(eventId);
        Long orgId = jwtUtil.getDataFromAuth().organizationId();
        if (!event.getOrganizerId().equals(orgId)) {
            throw new AppException(ErrorCode.FORBIDDEN, "Bạn không có quyền sửa bản nháp này");
        }
        if (event.getApprovalStatus() != EventApprovalStatus.DRAFT) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Sự kiện không còn ở trạng thái nháp");
        }

        event.getShowtimes().clear();
        event.setTotalSeats(request.getTotalSeats());

        if (request.getShowtimes() != null && !request.getShowtimes().isEmpty()) {
            for (CreateShowtimeRequest showtimeRequest : request.getShowtimes()) {
                if (showtimeRequest.getEndDatetime().isBefore(showtimeRequest.getStartDatetime())) {
                    throw new AppException(ErrorCode.BAD_REQUEST, "Giờ kết thúc của suất diễn phải sau giờ bắt đầu");
                }

                Showtime showtime = Showtime.builder()
                        .event(event)
                        .startDatetime(showtimeRequest.getStartDatetime())
                        .endDatetime(showtimeRequest.getEndDatetime())
                        .venue(showtimeRequest.getVenue())
                        .address(showtimeRequest.getAddress())
                        .ticketTypes(new java.util.HashSet<>())
                        .build();

                if (showtimeRequest.getWardCode() != null) {
                    showtime.setWard(locationUtil.getWardByCode(showtimeRequest.getWardCode()));
                }
                if (showtimeRequest.getProvinceCode() != null) {
                    showtime.setProvince(locationUtil.getProvinceByCode(showtimeRequest.getProvinceCode()));
                }

                if (showtimeRequest.getTicketTypes() != null && !showtimeRequest.getTicketTypes().isEmpty()) {
                    for (CreateTicketTypeRequest ticketRequest : showtimeRequest.getTicketTypes()) {
                        TicketType ticketType = TicketType.builder()
                                .typeName(ticketRequest.getTypeName())
                                .description(ticketRequest.getDescription())
                                .price(ticketRequest.getPrice())
                                .quantityTotal(ticketRequest.getQuantityTotal())
                                .quantitySold(0)
                                .minPurchase(ticketRequest.getMinPurchase())
                                .maxPurchase(ticketRequest.getMaxPurchase())
                                .saleStartDate(ticketRequest.getSaleStartDate())
                                .saleEndDate(ticketRequest.getSaleEndDate())
                                .ticketTypeStatus(TicketTypeStatus.AVAILABLE)
                                .showtime(showtime)
                                .build();

                        showtime.getTicketTypes().add(ticketType);
                    }
                }

                event.getShowtimes().add(showtime);
            }
        }

        if (event.getCurrentStep() == null || event.getCurrentStep() < 2L) {
            event.setCurrentStep(2L);
        }

        Event savedEvent = eventRepository.save(event);

        if (seatMapFile != null && !seatMapFile.isEmpty()) {
            try {
                CompletableFuture<Void> uploadTask = uploadService.uploadImageAsync(savedEvent, seatMapFile.getBytes(), "seat_map");
                uploadTask.join();
                savedEvent = eventRepository.save(savedEvent);
            } catch (IOException e) {
                throw new AppException(ErrorCode.IO_EXCEPTION, "Không thể đọc ảnh sơ đồ ghế: " + e.getMessage());
            }
        }

        return convertToDTO(savedEvent);
    }

    @Transactional
    @CacheEvict(value = "eventDetails", key = "#eventId")
    public EventResponse updateDraftStep3(Long eventId, UpdateDraftStep3Request request) {
        Event event = eventUtil.getEventOrElseThrow(eventId);
        Long orgId = jwtUtil.getDataFromAuth().organizationId();
        if (!event.getOrganizerId().equals(orgId)) {
            throw new AppException(ErrorCode.FORBIDDEN, "Bạn không có quyền sửa bản nháp này");
        }
        if (event.getApprovalStatus() != EventApprovalStatus.DRAFT) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Sự kiện không còn ở trạng thái nháp");
        }

        event.setAllowMultipleTicketTypesPerOrder(request.getAllowMultipleTicketTypesPerOrder() != null && request.getAllowMultipleTicketTypesPerOrder());
        event.setAllowDiscountCode(request.getAllowDiscountCode() != null && request.getAllowDiscountCode());
        event.setAllowResale(request.getAllowResale() != null && request.getAllowResale());

        if (event.getAllowResale()) {
            if (request.getMaxResalePricePercentage() == null || request.getOrganizerRoyaltyFeePercentage() == null) {
                throw new AppException(ErrorCode.BAD_REQUEST, "Khi cho phép bán lại, phần trăm giá bán lại tối đa và phí tác quyền của ban tổ chức không được để trống.");
            }
            event.setMaxResalePricePercentage(request.getMaxResalePricePercentage().divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP));
            event.setOrganizerRoyaltyFeePercentage(request.getOrganizerRoyaltyFeePercentage().divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP));
        } else {
            if (request.getMaxResalePricePercentage() != null || request.getOrganizerRoyaltyFeePercentage() != null) {
                throw new AppException(ErrorCode.BAD_REQUEST, "Khi không cho phép bán lại, phần trăm giá bán lại tối đa và phí tác quyền của ban tổ chức phải để trống.");
            }
            event.setMaxResalePricePercentage(null);
            event.setOrganizerRoyaltyFeePercentage(null);
        }

        event.setPostPurchaseInstruction(request.getPostPurchaseInstruction());
        event.setCheckInInstruction(request.getCheckInInstruction());
        event.setEntryGateInstruction(request.getEntryGateInstruction());

        if (event.getCurrentStep() == null || event.getCurrentStep() < 3L) {
            event.setCurrentStep(3L);
        }

        Event savedEvent = eventRepository.save(event);
        return convertToDTO(savedEvent);
    }

    @Transactional
    @CacheEvict(value = "eventDetails", key = "#eventId")
    public EventResponse updateDraftStep4(Long eventId, UpdateDraftStep4Request request) {
        Event event = eventUtil.getEventOrElseThrow(eventId);
        Long orgId = jwtUtil.getDataFromAuth().organizationId();
        if (!event.getOrganizerId().equals(orgId)) {
            throw new AppException(ErrorCode.FORBIDDEN, "Bạn không có quyền sửa bản nháp này");
        }
        if (event.getApprovalStatus() != EventApprovalStatus.DRAFT) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Sự kiện không còn ở trạng thái nháp");
        }

        event.setBankInfoId(request.getBankInfoId());
        event.setReconciliationNote(request.getReconciliationNote());

        if (event.getCurrentStep() == null || event.getCurrentStep() < 4L) {
            event.setCurrentStep(4L);
        }

        Event savedEvent = eventRepository.save(event);
        return convertToDTO(savedEvent);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "eventDetails", key = "#eventId"),
        @CacheEvict(value = "homepageEvents", allEntries = true),
        @CacheEvict(value = "trendingEvents", allEntries = true)
    })
    public EventResponse publishEvent(Long eventId) {
        Event event = eventUtil.getEventOrElseThrow(eventId);
        Long orgId = jwtUtil.getDataFromAuth().organizationId();
        if (!event.getOrganizerId().equals(orgId)) {
            throw new AppException(ErrorCode.FORBIDDEN, "Bạn không có quyền xuất bản sự kiện này");
        }
        if (event.getApprovalStatus() != EventApprovalStatus.DRAFT) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Sự kiện không còn ở trạng thái nháp hoặc đã được xuất bản");
        }

        validateEventForPublish(event);

        event.setApprovalStatus(EventApprovalStatus.PENDING_REVIEW);
        event.setCurrentStep(5L);

        Event savedEvent = eventRepository.save(event);
        return convertToDTO(savedEvent);
    }

    private void validateEventForPublish(Event event) {
        if (event.getEventName() == null || event.getEventName().isBlank()) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Tên sự kiện không được để trống");
        }
        if (event.getDescription() == null || event.getDescription().isBlank()) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Mô tả sự kiện không được để trống");
        }
        if (event.getVenue() == null || event.getVenue().isBlank()) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Địa điểm không được để trống");
        }
        if (event.getEventType() == null) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Loại sự kiện không được để trống");
        }
        if (event.getCategory() == null) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Thể loại sự kiện không được để trống");
        }
        if (event.getProvince() == null) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Tỉnh/Thành phố không được để trống");
        }
        if (event.getWard() == null) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Phường/Xã không được để trống");
        }
        if (event.getShowtimes() == null || event.getShowtimes().isEmpty()) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Sự kiện phải có ít nhất một suất diễn");
        }
        for (Showtime showtime : event.getShowtimes()) {
            if (showtime.getTicketTypes() == null || showtime.getTicketTypes().isEmpty()) {
                throw new AppException(ErrorCode.BAD_REQUEST, "Mỗi suất diễn phải có ít nhất một loại vé");
            }
        }
        if (event.getAllowResale() != null && event.getAllowResale()) {
            if (event.getMaxResalePricePercentage() == null || event.getOrganizerRoyaltyFeePercentage() == null) {
                throw new AppException(ErrorCode.BAD_REQUEST, "Khi cho phép bán lại, phải điền đầy đủ phần trăm giá bán lại tối đa và phí tác quyền");
            }
        }
        if (event.getBankInfoId() == null) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Thông tin tài khoản ngân hàng nhận tiền không được để trống");
        }
    }

    @Transactional(readOnly = true)
    public EventResponse getEventDraft(Long eventId) {
        Event event = eventUtil.getEventOrElseThrow(eventId);
        Long orgId = jwtUtil.getDataFromAuth().organizationId();
        if (!event.getOrganizerId().equals(orgId)) {
            throw new AppException(ErrorCode.FORBIDDEN, "Bạn không có quyền truy cập sự kiện này");
        }
        return convertToDTO(event);
    }

    @Transactional
    public Boolean deleteDraftEvents(List<Long> eventIds) {
        Long orgId = jwtUtil.getDataFromAuth().organizationId();
        if (orgId == null) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Org id is null");
        }

        List<Event> drafts = eventRepository.findAllById(eventIds);
        for (Event event : drafts) {
            if (!event.getOrganizerId().equals(orgId)) {
                throw new AppException(ErrorCode.FORBIDDEN, "Bạn không có quyền xóa bản nháp này: " + event.getId());
            }
            if (event.getApprovalStatus() != EventApprovalStatus.DRAFT) {
                throw new AppException(ErrorCode.BAD_REQUEST, "Sự kiện không còn ở trạng thái nháp: " + event.getId());
            }
        }

        eventRepository.deleteAll(drafts);
        return true;
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "eventDetails", key = "#eventId"),
        @CacheEvict(value = "homepageEvents", allEntries = true),
        @CacheEvict(value = "trendingEvents", allEntries = true)
    })
    public EventResponse cancelEvent(Long eventId) {
        Event event = eventUtil.getEventOrElseThrow(eventId);
        Long orgId = jwtUtil.getDataFromAuth().organizationId();
        if (!event.getOrganizerId().equals(orgId)) {
            throw new AppException(ErrorCode.FORBIDDEN, "Bạn không có quyền hủy sự kiện này");
        }

        event.setIsCancelled(true);
        if (event.getShowtimes() != null) {
            for (Showtime showtime : event.getShowtimes()) {
                showtime.setIsCancelled(true);
            }
        }

        Event savedEvent = eventRepository.save(event);
        return convertToDTO(savedEvent);
    }
}
