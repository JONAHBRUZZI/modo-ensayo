package com.modoensayo.reviews.service;

import com.modoensayo.reviews.domain.Review;
import com.modoensayo.reviews.dto.CreateReviewRequest;
import com.modoensayo.reviews.dto.ReviewResponse;
import com.modoensayo.reviews.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;

    public ReviewResponse create(UUID reviewerId, CreateReviewRequest req) {
        Review r = Review.builder()
                .classId(req.classId()).reviewerId(reviewerId)
                .targetType(req.targetType()).targetId(req.targetId())
                .score(req.score()).comment(req.comment())
                .build();
        return toResponse(reviewRepository.save(r));
    }

    public List<ReviewResponse> getByClass(UUID classId) {
        return reviewRepository.findByClassId(classId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    public List<ReviewResponse> getByUser(UUID userId) {
        return reviewRepository.findByReviewerId(userId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    public List<ReviewResponse> getByTarget(String type, UUID targetId) {
        return reviewRepository.findByTargetTypeAndTargetId(type, targetId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    private ReviewResponse toResponse(Review r) {
        return new ReviewResponse(r.getId(), r.getClassId(), r.getReviewerId(), "Usuario",
                r.getTargetType(), r.getTargetId(), r.getScore(), r.getComment(), r.getCreatedAt());
    }
}
