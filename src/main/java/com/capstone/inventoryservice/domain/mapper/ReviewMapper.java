package com.capstone.inventoryservice.domain.mapper;

import com.capstone.inventoryservice.domain.client.IAMFeignClient;
import com.capstone.inventoryservice.domain.client.UserInternalResponse;
import com.capstone.inventoryservice.domain.dto.response.ReviewResponse;
import com.capstone.inventoryservice.model.entity.Review;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ReviewMapper {
    private final IAMFeignClient iamFeignClient;

    public ReviewResponse mapToResponse(Review review) {
        String fullName = Optional
                .ofNullable(iamFeignClient.getUserById(review.getUserId()))
                .map(UserInternalResponse::getUserFullName)
                .orElse(null);
        String avatarUrl = Optional
                .ofNullable(iamFeignClient.getUserById(review.getUserId()))
                .map(UserInternalResponse::getUserAvatarUrl)
                .orElse(null);
        return ReviewResponse.builder()
                .id(review.getId())
                .eventId(review.getEvent().getId())
                .userId(review.getUserId())
                .userFullName(fullName)
                .userAvatarUrl(avatarUrl)
                .rating(review.getRating())
                .comment(review.getComment())
                .images(review.getImages() != null ? new java.util.LinkedHashSet<>(review.getImages()) : null)
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}
