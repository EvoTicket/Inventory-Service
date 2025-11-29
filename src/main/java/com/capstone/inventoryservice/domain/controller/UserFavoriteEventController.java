package com.capstone.inventoryservice.domain.controller;

import com.capstone.inventoryservice.domain.dto.BasePageResponse;
import com.capstone.inventoryservice.domain.dto.BaseResponse;
import com.capstone.inventoryservice.domain.dto.response.UserFavoriteEventResponse;
import com.capstone.inventoryservice.domain.service.UserFavoriteEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class UserFavoriteEventController {

    private final UserFavoriteEventService userFavoriteEventService;

    @PostMapping
    public ResponseEntity<BaseResponse<UserFavoriteEventResponse>> addToFavorites(
            @RequestParam Long eventId) {
        UserFavoriteEventResponse result = userFavoriteEventService.addToFavorites(eventId);
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.created(result));
    }

    @DeleteMapping
    public ResponseEntity<BaseResponse<Boolean>> removeFromFavorites(
            @RequestParam Long eventId) {
        boolean result = userFavoriteEventService.removeFromFavorites(eventId);
        return ResponseEntity.ok(BaseResponse.ok(result));
    }

    @GetMapping
    public ResponseEntity<BasePageResponse<UserFavoriteEventResponse>> getUserFavorites(
            @RequestParam Long userId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page - 1 , size);

        BasePageResponse<UserFavoriteEventResponse> result = userFavoriteEventService
                .getUserFavorites(userId, keyword, pageable);

        return ResponseEntity.ok(result);
    }
}