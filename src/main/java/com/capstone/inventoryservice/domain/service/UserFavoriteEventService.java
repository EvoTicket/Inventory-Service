package com.capstone.inventoryservice.domain.service;

import com.capstone.inventoryservice.domain.dto.BasePageResponse;
import com.capstone.inventoryservice.domain.dto.response.UserFavoriteEventResponse;
import com.capstone.inventoryservice.domain.util.EventUtil;
import com.capstone.inventoryservice.exception.AppException;
import com.capstone.inventoryservice.exception.ErrorCode;
import com.capstone.inventoryservice.model.entity.Event;
import com.capstone.inventoryservice.model.entity.UserFavoriteEvent;
import com.capstone.inventoryservice.model.repository.UserFavoriteEventRepository;
import com.capstone.inventoryservice.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserFavoriteEventService {
    private final UserFavoriteEventRepository userFavoriteEventRepository;
    private final EventUtil eventUtil;
    private final JwtUtil jwtUtil;

    @Transactional
    public UserFavoriteEventResponse addToFavorites(Long eventId) {
        Long userId = jwtUtil.getDataFromAuth().userId();
        if (userFavoriteEventRepository.existsByUserIdAndEventId(userId, eventId)) {
            throw new AppException(ErrorCode.CONFLICT, "Event already in favorites");
        }

        Event event = eventUtil.getEventOrElseThrow(eventId);

        UserFavoriteEvent userFavoriteEvent = new UserFavoriteEvent();
        userFavoriteEvent.setUserId(userId);
        userFavoriteEvent.setEvent(event);
        userFavoriteEvent.setLikedAt(OffsetDateTime.now(ZoneOffset.ofHours(7)));

        UserFavoriteEvent saved = userFavoriteEventRepository.save(userFavoriteEvent);

        return mapToDTO(saved);
    }

    @Transactional(readOnly = true)
    public BasePageResponse<UserFavoriteEventResponse> getUserFavorites(Long userId, String keyword, Pageable pageable) {

        Page<UserFavoriteEvent> page = userFavoriteEventRepository
                .findByUserIdWithSearch(userId, keyword, pageable);

        List<UserFavoriteEventResponse> content = page.getContent().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        return BasePageResponse.<UserFavoriteEventResponse>builder()
                .content(content)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    @Transactional
    public boolean removeFromFavorites(Long eventId) {
        Long userId = jwtUtil.getDataFromAuth().userId();

        UserFavoriteEvent userFavoriteEvent = userFavoriteEventRepository
                .findByUserIdAndEventId(userId, eventId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Favorite not found"));

        userFavoriteEventRepository.delete(userFavoriteEvent);
        return true;
    }

    private UserFavoriteEventResponse mapToDTO(UserFavoriteEvent entity) {
        Event event = entity.getEvent();
        return UserFavoriteEventResponse.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .eventId(event.getId())
                .eventName(event.getEventName())
                .eventDescription(event.getDescription())
                .eventStartDate(event.getStartDatetime())
                .eventEndDate(event.getEndDatetime())
                .likedAt(entity.getLikedAt())
                .build();
    }
}
