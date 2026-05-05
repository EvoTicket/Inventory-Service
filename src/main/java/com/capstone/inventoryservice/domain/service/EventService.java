package com.capstone.inventoryservice.domain.service;

import com.capstone.inventoryservice.domain.client.IAMFeignClient;
import com.capstone.inventoryservice.domain.client.OrgInternalResponse;
import com.capstone.inventoryservice.domain.client.OrderFeignClient;
import com.capstone.inventoryservice.domain.dto.BasePageResponse;
import com.capstone.inventoryservice.domain.dto.event.TicketCreatedEvent;
import com.capstone.inventoryservice.domain.dto.request.CreateEventRequest;
import com.capstone.inventoryservice.domain.dto.request.CreateShowtimeRequest;
import com.capstone.inventoryservice.domain.dto.request.CreateTicketTypeRequest;
import com.capstone.inventoryservice.domain.dto.request.EventFilterRequest;
import com.capstone.inventoryservice.domain.dto.request.UpdateEventRequest;
import com.capstone.inventoryservice.domain.dto.response.EventResponse;
import com.capstone.inventoryservice.domain.dto.response.HomepageResponse;
import com.capstone.inventoryservice.domain.dto.response.HomepageSectionResponse;
import com.capstone.inventoryservice.domain.dto.response.ListEventResponse;
import com.capstone.inventoryservice.domain.dto.response.ReviewResponse;
import com.capstone.inventoryservice.domain.dto.response.ShowtimeResponse;
import com.capstone.inventoryservice.domain.dto.response.TicketTypeResponse;
import com.capstone.inventoryservice.domain.dto.response.TrendingEventResponse;
import com.capstone.inventoryservice.domain.dto.response.EventVolumeResponse;
import com.capstone.inventoryservice.domain.dto.response.OrgEventDto;
import com.capstone.inventoryservice.domain.mapper.ReviewMapper;
import com.capstone.inventoryservice.model.entity.Event;
import com.capstone.inventoryservice.model.entity.EventView;
import com.capstone.inventoryservice.model.entity.Showtime;
import com.capstone.inventoryservice.model.entity.TicketType;
import com.capstone.inventoryservice.exception.AppException;
import com.capstone.inventoryservice.exception.ErrorCode;
import com.capstone.inventoryservice.domain.mapper.TicketTypeMapper;
import com.capstone.inventoryservice.model.enums.EventCategory;
import com.capstone.inventoryservice.model.enums.EventType;
import com.capstone.inventoryservice.model.enums.TicketAvailabilityStatus;
import com.capstone.inventoryservice.model.enums.EventStatus;
import com.capstone.inventoryservice.model.enums.EventApprovalStatus;
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
        Specification<Event> spec = EventSpecification.withFilters(filter);
        Pageable pageable = buildPageable(filter);

        Page<Event> eventPage = eventRepository.findAll(spec, pageable);

        return buildEventPageResponse(eventPage, pageable);
    }

    @Transactional(readOnly = true)
    public HomepageResponse getHomepageEvents() {
        Pageable pageable = PageRequest.of(0, 4);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneMonthLater = now.plusMonths(1);

        Page<Event> upcomingEvents = eventRepository.findUpcomingEvents(now, oneMonthLater, pageable);
        List<ListEventResponse> upcomingResponses = mapToResponseList(upcomingEvents.getContent());

        Page<Event> livestageEvents = eventRepository.findByCategory(EventCategory.LIVESTAGE, pageable);
        List<ListEventResponse> livestageResponses = mapToResponseList(livestageEvents.getContent());

        Page<Event> stageArtEvents = eventRepository.findByCategory(EventCategory.STAGE_ART, pageable);
        List<ListEventResponse> stageArtResponses = mapToResponseList(stageArtEvents.getContent());

        Page<Event> workshopEvents = eventRepository.findByCategory(EventCategory.WORKSHOP, pageable);
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

    @Transactional(readOnly = true)
    public BasePageResponse<TrendingEventResponse> getTrendingEvents(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        Page<Event> eventPage = eventRepository.findTrendingEvents(LocalDateTime.now(), pageable);

        List<Long> eventIds = eventPage.getContent().stream()
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

        Page<TrendingEventResponse> dtoPage = eventPage.map(event -> {
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
        });

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

        String sortBy = filter.getSortBy() != null ? filter.getSortBy() : "createdAt";
        String sortDirection = filter.getSortDirection() != null ? filter.getSortDirection() : "DESC";

        String sortField = mapSortField(sortBy);

        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Sort sort = Sort.by(direction, sortField);

        return PageRequest.of(page, size, sort);
    }

    private String mapSortField(String sortBy) {
        return switch (sortBy.toLowerCase()) {
            case "startdatetime", "starttime", "start", "neardate" -> "createdAt";
            case "enddatetime", "endtime", "end" -> "createdAt";
            case "totalseats", "seats" -> "totalSeats";
            case "eventname", "name" -> "eventName";
            case "popular", "trending" -> "viewCount";
            case "price", "price_asc" -> "minPrice";
            case "createdat", "created", "newest" -> "createdAt";
            case "updatedat", "updated" -> "updatedAt";
            default -> "createdAt";
        };
    }

    @Transactional
    public EventResponse getEventById(Long eventId) {
        Event event = eventUtil.getEventOrElseThrow(eventId);
        recordView(event);
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

    public EventResponse createEvent(CreateEventRequest request, MultipartFile bannerFile, MultipartFile thumbnailFile, MultipartFile seatMapFile) {

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
                .organizerId(orgId)
                .isFeatured(request.getIsFeatured() != null && request.getIsFeatured())
                .category(request.getCategory())
                .province(locationUtil.getProvinceByCode(request.getProvinceCode()))
                .ward(locationUtil.getWardByCode(request.getWardCode()))
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .build();

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
                                .ticketTypeStatus(ticketRequest.getTicketTypeStatus())
                                .showtime(showtime)
                                .build();

                        showtime.getTicketTypes().add(ticketType);
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
                throw new AppException(ErrorCode.IO_EXCEPTION, "Không thể đọc ảnh sơ đồ chỗ: " + e.getMessage());
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

        return convertToDTO(eventRepository.findByIdWithDetails(savedEvent.getId()).orElse(savedEvent));
    }

    @Transactional
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

        Event updatedEvent = eventRepository.save(event);
        log.info("Updated event with ID: {}", eventId);
        return convertToDTO(updatedEvent);
    }

    @Transactional
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
        long totalPending = eventRepository.count(EventSpecification.withFilters(EventFilterRequest.builder().organizerId(orgId).approvalStatuses(List.of(EventApprovalStatus.PENDING)).build()));

        return OrgEventDto.builder()
                .totalEvents(totalEvents)
                .totalOnSales(totalOnSales)
                .totalPending(totalPending)
                .totalCompleted(totalCompleted)
                .events(BasePageResponse.fromPage(dtoPage))
                .build();
    }

    private EventResponse convertToDTO(Event event) {
        List<ShowtimeResponse> showtimeDTOs = null;
        if (event.getShowtimes() != null) {
            showtimeDTOs = event.getShowtimes().stream()
                    .map(this::convertShowtimeToDTO)
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

        return EventResponse.builder()
                .eventId(event.getId())
                .eventName(event.getEventName())
                .orgInternalResponse(orgInternalResponse)
                .description(event.getDescription())
                .venue(event.getVenue())
                .address(event.getFullAddress())
                .eventStatus(event.getEventStatus())
                .eventType(event.getEventType())
                .bannerImage(event.getBannerImage())
                .thumbnailImage(event.getThumbnailImage())
                .introduction(event.getIntroduction())
                .seatMapImage(event.getSeatMapImage())
                .totalSeats(event.getTotalSeats())
                .organizerId(event.getOrganizerId())
                .isFeatured(event.getIsFeatured())
                .category(event.getCategory())
                .latitude(event.getLatitude())
                .longitude(event.getLongitude())
                .showtimes(showtimeDTOs)
                .reviews(reviewDTOs)
                .build();
    }

    private ShowtimeResponse convertShowtimeToDTO(Showtime showtime) {
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
    public List<ListEventResponse> getRecommendedEvents(int limit) {
        Long userId = null;
        try {
            var auth = jwtUtil.getDataFromAuth();
            if (auth != null) userId = auth.userId();
        } catch (Exception ignored) {
            log.info("User not authenticated, returning fallback recommendations");
        }

        if (userId == null) {
            return getFallbackRecommendations(limit);
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

        if (knownEventIds.isEmpty()) {
            return getFallbackRecommendations(limit);
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
            return getFallbackRecommendations(limit);
        }

        LocalDateTime now = LocalDateTime.now();
        List<Event> candidates = eventRepository.findAll(
                Specification.where(
                        EventSpecification.withFilters(
                                EventFilterRequest.builder()
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
            return getFallbackRecommendations(limit);
        }

        List<ListEventResponse> recommendations = mapToResponseList(scoredCandidates);
        if(scoredCandidates.size() < limit){
            recommendations.addAll(getFallbackRecommendations(limit - scoredCandidates.size()));
        }

        return recommendations;
    }

    private List<ListEventResponse> getFallbackRecommendations(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        LocalDateTime now = LocalDateTime.now();
        Page<Event> upcomingEvents = eventRepository.findUpcomingEvents(now, now.plusMonths(3), pageable);
        return mapToResponseList(upcomingEvents.getContent());
    }

    private record ScoredEvent(Event event, int score) { }
}
