package com.capstone.inventoryservice.domain.controller;

import com.capstone.inventoryservice.domain.dto.BaseResponse;
import com.capstone.inventoryservice.domain.dto.request.CreateReviewRequest;
import com.capstone.inventoryservice.domain.dto.request.UpdateReviewRequest;
import com.capstone.inventoryservice.domain.dto.response.ReviewResponse;
import com.capstone.inventoryservice.domain.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BaseResponse<ReviewResponse>> createReview(
            @RequestParam Long eventId,
            @RequestParam(required = false) String comment,
            @RequestParam(required = false) Integer rating,
            @RequestParam(value = "files", required = false) List<MultipartFile> images
    ) {
        CreateReviewRequest request  = CreateReviewRequest.builder()
                .eventId(eventId)
                .comment(comment)
                .rating(rating)
                .build();
        ReviewResponse response = reviewService.createReview(request, images);
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.ok(response));
    }

    @PutMapping(value = "/{reviewId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BaseResponse<ReviewResponse>> updateReview(
            @PathVariable Long reviewId,
            @RequestParam(required = false) String comment,
            @RequestParam(required = false) Integer rating,
            @RequestParam("files") List<MultipartFile> images
    ) {
        UpdateReviewRequest request  = UpdateReviewRequest.builder()
                .comment(comment)
                .rating(rating)
                .build();
        ReviewResponse response = reviewService.updateReview(reviewId, request, images);
        return ResponseEntity.ok(BaseResponse.ok(response));
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<BaseResponse<Boolean>> deleteReview(@PathVariable Long reviewId) {
        return ResponseEntity.ok(BaseResponse.ok(reviewService.deleteReview(reviewId)));
    }
}
