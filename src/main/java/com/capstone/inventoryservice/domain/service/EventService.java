package com.capstone.inventoryservice.domain.service;

import com.capstone.inventoryservice.domain.client.IAMFeignClient;
import com.capstone.inventoryservice.domain.client.OrgClientResponse;
import com.capstone.inventoryservice.domain.dto.BasePageResponse;
import com.capstone.inventoryservice.domain.dto.request.CreateEventRequest;
import com.capstone.inventoryservice.domain.dto.request.CreateTicketTypeRequest;
import com.capstone.inventoryservice.domain.dto.request.EventFilterRequest;
import com.capstone.inventoryservice.domain.dto.request.UpdateEventRequest;
import com.capstone.inventoryservice.domain.dto.response.EventResponse;
import com.capstone.inventoryservice.domain.dto.response.ListEventResponse;
import com.capstone.inventoryservice.domain.dto.response.ReviewResponse;
import com.capstone.inventoryservice.domain.dto.response.TicketTypeResponse;
import com.capstone.inventoryservice.domain.mapper.ReviewMapper;
import com.capstone.inventoryservice.model.entity.Event;
import com.capstone.inventoryservice.model.entity.EventCategory;
import com.capstone.inventoryservice.model.entity.TicketType;
import com.capstone.inventoryservice.exception.AppException;
import com.capstone.inventoryservice.exception.ErrorCode;
import com.capstone.inventoryservice.domain.mapper.TicketTypeMapper;
import com.capstone.inventoryservice.model.repository.EventCategoryRepository;
import com.capstone.inventoryservice.model.repository.EventRepository;
import com.capstone.inventoryservice.model.repository.ReviewRepository;
import com.capstone.inventoryservice.model.repository.TicketTypeRepository;
import com.capstone.inventoryservice.security.JwtUtil;
import com.capstone.inventoryservice.domain.specification.EventSpecification;
import com.capstone.inventoryservice.domain.util.EventUtil;
import com.capstone.inventoryservice.domain.util.LocationUtil;
import com.cloudinary.Cloudinary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

    private final EventRepository eventRepository;
    private final EventCategoryRepository categoryRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final JwtUtil jwtUtil;
    private final IAMFeignClient iamFeignClient;
    private final LocationUtil locationUtil;
    private final EventUtil eventUtil;
    private final TicketTypeMapper ticketTypeMapper;
    private final Cloudinary cloudinary;
    private final ReviewRepository reviewRepository;
    private final ReviewMapper reviewMapper;

    @Transactional(readOnly = true)
    public BasePageResponse<ListEventResponse> getEvents(EventFilterRequest filter) {
        Specification<Event> spec = EventSpecification.withFilters(filter);
        Pageable pageable = buildPageable(filter);
        Page<Event> eventPage = eventRepository.findAll(spec, pageable);

        if (eventPage.isEmpty()) {
            Page<ListEventResponse> emptyPage = Page.empty(pageable);
            return BasePageResponse.fromPage(emptyPage);
        }

        List<Long> eventIds = eventPage.getContent().stream()
                .map(Event::getId)
                .toList();

        Map<Long, Long> favoriteCountMap = getFavoriteCountMap(eventIds);

        Long currentUserId = jwtUtil.getDataFromAuth().userId();
        Set<Long> userFavoriteEventIds = currentUserId != null
                ? getUserFavoriteEventIds(currentUserId, eventIds)
                : Collections.emptySet();

        Page<ListEventResponse> dtoPage = eventPage.map(event -> {
            ListEventResponse dto = ListEventResponse.fromEntity(event);

            Long favoriteCount = favoriteCountMap.getOrDefault(event.getId(), 0L);
            dto.setFavoriteCount(favoriteCount);

            boolean isFavorite = userFavoriteEventIds.contains(event.getId());
            dto.setFavorite(isFavorite);

            return dto;
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
            case "startdatetime", "starttime", "start" -> "startDatetime";
            case "enddatetime", "endtime", "end" -> "endDatetime";
            case "totalseats", "seats" -> "totalSeats";
            case "eventname", "name" -> "eventName";
            case "createdat", "created" -> "createdAt";
            case "updatedat", "updated" -> "updatedAt";
            default -> "createdAt";
        };
    }

    @Transactional(readOnly = true)
    public EventResponse getEventById(Long eventId) {
        Event event = eventUtil.getEventOrElseThrow(eventId);
        return convertToDTO(event);
    }

    public EventResponse  createEvent(CreateEventRequest request) {
        EventCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Category not found with id: " + request.getCategoryId()));

        if (request.getEndDatetime().isBefore(request.getStartDatetime())) {
            throw new AppException(ErrorCode.BAD_REQUEST, "End datetime must be after start datetime");
        }

        Long orgId = jwtUtil.getDataFromAuth().organizationId();
        if(orgId == null) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Org id is null");
        }

        Event event = Event.builder()
                .eventName(request.getEventName())
                .description(request.getDescription())
                .venue(request.getVenue())
                .address(request.getAddress())
                .startDatetime(request.getStartDatetime())
                .endDatetime(request.getEndDatetime())
                .eventStatus(request.getEventStatus())
                .eventType(request.getEventType())
                .totalSeats(request.getTotalSeats())
                .organizerId(orgId)
                .isFeatured(request.getIsFeatured() != null && request.getIsFeatured())
                .category(category)
                .province(locationUtil.getProvinceByCode(request.getProvinceCode()))
                .ward(locationUtil.getWardByCode(request.getWardCode()))
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .build();

        if (request.getTicketTypes() != null && !request.getTicketTypes().isEmpty()) {
            for (CreateTicketTypeRequest ticketRequest : request.getTicketTypes()) {
                TicketType ticketType = TicketType.builder()
                        .typeName(ticketRequest.getTypeName())
                        .description(ticketRequest.getDescription())
                        .price(ticketRequest.getPrice())
                        .takePlaceTime(ticketRequest.getTakePlaceTime())
                        .quantityAvailable(ticketRequest.getQuantityAvailable())
                        .quantitySold(0)
                        .minPurchase(ticketRequest.getMinPurchase())
                        .maxPurchase(ticketRequest.getMaxPurchase())
                        .saleStartDate(ticketRequest.getSaleStartDate())
                        .saleEndDate(ticketRequest.getSaleEndDate())
                        .ticketTypeStatus(ticketRequest.getTicketTypeStatus())
                        .event(event)
                        .build();

                event.getTicketTypes().add(ticketType);
            }
        }
        Event savedEvent = eventRepository.save(event);

        return convertToDTO(eventRepository.findByIdWithTicketTypes(savedEvent.getId()).orElse(savedEvent));
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
        if (request.getStartDatetime() != null) {
            event.setStartDatetime(request.getStartDatetime());
        }
        if (request.getEndDatetime() != null) {
            if (request.getEndDatetime().isBefore(event.getStartDatetime())) {
                throw new AppException(ErrorCode.BAD_REQUEST, "End datetime must be after start datetime");
            }
            event.setEndDatetime(request.getEndDatetime());
        }
        if (request.getEventStatus() != null) {
            event.setEventStatus(request.getEventStatus());
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
        if (request.getCategoryId() != null) {
            EventCategory category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Category not found with id: " + request.getCategoryId()));
            event.setCategory(category);
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

    @Transactional
    @SuppressWarnings("unchecked")
    public String uploadEventImage(MultipartFile file, Long evenId, String type) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("File phải là ảnh");
        }

        String folder = "event/" + evenId + "/" + type + "/" ;

        String publicId = UUID.randomUUID().toString();

        Map<String, Object> options = new HashMap<>();
        options.put("resource_type", "image");
        options.put("folder",  folder);
        options.put("public_id", publicId);
        options.put("overwrite", true);

        try {
            Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(), options);
            Event event = eventUtil.getEventOrElseThrow(evenId);
            switch (type) {
                case "banner":
                    event.setBannerImage(uploadResult.get("url").toString());
                    break;
                case "thumbnail":
                    event.setThumbnailImage(uploadResult.get("url").toString());
                    break;
                default:
                    break;
            }
            return uploadResult.get("url").toString();
        } catch (IOException e) {
            throw new AppException(ErrorCode.IO_EXCEPTION, "Không thể tải ảnh lên Cloudinary: " + e.getMessage());
        }
    }

    private EventResponse convertToDTO(Event event) {
        List<TicketTypeResponse> ticketTypeDTOs = null;
        if (event.getTicketTypes() != null) {
            ticketTypeDTOs = event.getTicketTypes().stream()
                    .map(ticketTypeMapper::convertToDTO)
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
        OrgClientResponse orgClientResponse = iamFeignClient.getOrganizationById(event.getOrganizerId());

        return EventResponse.builder()
                .eventId(event.getId())
                .eventName(event.getEventName())
                .orgClientResponse(orgClientResponse)
                .description(event.getDescription())
                .venue(event.getVenue())
                .address(event.getAddress())
                .startDatetime(event.getStartDatetime())
                .endDatetime(event.getEndDatetime())
                .eventStatus(event.getEventStatus())
                .eventType(event.getEventType())
                .bannerImage(event.getBannerImage())
                .thumbnailImage(event.getThumbnailImage())
                .totalSeats(event.getTotalSeats())
                .organizerId(event.getOrganizerId())
                .isFeatured(event.getIsFeatured())
                .categoryId(event.getCategory() != null ? event.getCategory().getId() : null)
                .categoryName(event.getCategory() != null ? event.getCategory().getCategoryName() : null)
                .latitude(event.getLatitude())
                .longitude(event.getLongitude())
                .ticketTypes(ticketTypeDTOs)
                .reviews(reviewDTOs)
                .build();
    }
}