package com.capstone.inventoryservice.domain.mapper;

import com.capstone.inventoryservice.domain.dto.response.ReviewResponse;
import com.capstone.inventoryservice.model.entity.Review;
import org.springframework.stereotype.Component;

@Component
public class ReviewMapper {

    public ReviewResponse mapToResponse(Review review) {
        ReviewResponse response = new ReviewResponse();
        response.setId(review.getId());
        response.setEventId(review.getEvent().getId());
        response.setUserId(review.getUserId());
        response.setRating(review.getRating());
        response.setComment(review.getComment());
        response.setImages(review.getImages());
        response.setCreatedAt(review.getCreatedAt());
        response.setUpdatedAt(review.getUpdatedAt());
        return response;
    }
}
