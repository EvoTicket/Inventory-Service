package com.capstone.inventoryservice.domain.service;

import com.capstone.inventoryservice.domain.dto.request.CreateReviewRequest;
import com.capstone.inventoryservice.domain.dto.request.UpdateReviewRequest;
import com.capstone.inventoryservice.domain.dto.response.ReviewResponse;
import com.capstone.inventoryservice.domain.mapper.ReviewMapper;
import com.capstone.inventoryservice.domain.util.EventUtil;
import com.capstone.inventoryservice.exception.AppException;
import com.capstone.inventoryservice.exception.ErrorCode;
import com.capstone.inventoryservice.model.entity.Event;
import com.capstone.inventoryservice.model.entity.Review;
import com.capstone.inventoryservice.model.repository.ReviewRepository;
import com.capstone.inventoryservice.security.JwtUtil;
import com.cloudinary.Cloudinary;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final EventUtil eventUtil;
    private final JwtUtil jwtUtil;
    private final Cloudinary cloudinary;
    private final ReviewMapper reviewMapper;

    @Transactional
    public ReviewResponse createReview(CreateReviewRequest request, List<MultipartFile> images) {
        validateCommentOrImages(request.getComment(), images);

        Event event = eventUtil.getEventOrElseThrow(request.getEventId());

        Long userId = jwtUtil.getDataFromAuth().userId();

        Set<String> imagesStr;
        try {
            imagesStr = new HashSet<>(uploadImages(images, userId, event.getId()));
        }   catch (IOException e) {
            throw new AppException(ErrorCode.IO_EXCEPTION, "Không thể tải ảnh lên Cloudinary: " + e.getMessage());
        }

        Review review = new Review();
        review.setEvent(event);
        review.setUserId(userId);
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review.setImages(imagesStr);

        review = reviewRepository.save(review);
        event.getReviews().add(review);

        return reviewMapper.mapToResponse(review);
    }

    @Transactional
    public ReviewResponse updateReview(Long reviewId, UpdateReviewRequest request, List<MultipartFile> images) {
        validateCommentOrImages(request.getComment(), images);
        Long userId = jwtUtil.getDataFromAuth().userId();

        Review review = reviewRepository.findByIdAndUserId(reviewId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.FORBIDDEN, "Comment này không phải của bạn hoặc không tìm thấy"));

        if (request.getRating() != null) {
            review.setRating(request.getRating());
        }

        if (request.getComment() != null) {
            review.setComment(request.getComment());
        }

        Set<String> imagesStr;
        try {
            imagesStr = new HashSet<>(uploadImages(images, userId, review.getEvent().getId()));
        }   catch (IOException e) {
            throw new AppException(ErrorCode.IO_EXCEPTION, "Không thể tải ảnh lên Cloudinary: " + e.getMessage());
        }
        if (images != null) {
            review.setImages(imagesStr);
        }

        Review updatedReview = reviewRepository.save(review);
        return reviewMapper.mapToResponse(updatedReview);
    }

    @Transactional
    public boolean deleteReview(Long reviewId) {
        Long userId = jwtUtil.getDataFromAuth().userId();
        Review review = reviewRepository.findByIdAndUserId(reviewId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.FORBIDDEN, "Comment này không phải của bạn hoặc không tìm thấy"));

        reviewRepository.delete(review);
        return true;
    }

    private void validateCommentOrImages(String comment, List<MultipartFile> images) {
        boolean hasComment = StringUtils.hasText(comment);
        boolean hasImages = images != null && !images.isEmpty();

        if (!hasComment && !hasImages) {
            throw new AppException(ErrorCode.BAD_REQUEST, "phải có ít nhất 1 comment hoặc ảnh");
        }
    }

    @SuppressWarnings("unchecked")
    public List<String> uploadImages(List<MultipartFile> images, Long userId, Long evenId) throws IOException {
        if (images == null || images.isEmpty()) {
            return new ArrayList<>();
        }

        List<Map<String, Object>> uploadResults = new ArrayList<>();
        String folder = "event/" + evenId + "/users/" + userId + "/reviews/";

        for (MultipartFile file : images) {
            if (file.isEmpty()) {
                continue;
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new IllegalArgumentException("Tất cả file phải là ảnh");
            }

            String publicId = UUID.randomUUID().toString();

            Map<String, Object> options = new HashMap<>();
            options.put("resource_type", "image");
            options.put("folder", folder);
            options.put("public_id", publicId);
            options.put("overwrite", true);

            Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(), options);
            uploadResults.add(uploadResult);
        }

        return uploadResults.stream()
                .map(result -> (String) result.get("secure_url"))
                .toList();
    }
}
