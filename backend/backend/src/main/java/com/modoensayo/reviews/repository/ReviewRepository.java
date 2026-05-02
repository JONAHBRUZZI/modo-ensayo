package com.modoensayo.reviews.repository;

import com.modoensayo.reviews.domain.Review;
import com.modoensayo.reviews.enums.ReviewTargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
    boolean existsByClassIdAndReviewerIdAndTargetTypeAndTargetId(
            UUID classId, UUID reviewerId, ReviewTargetType targetType, UUID targetId);

    @Query("select avg(r.score) from Review r where r.targetType = :targetType and r.targetId = :targetId")
    Double findAverageScore(ReviewTargetType targetType, UUID targetId);

    long countByTargetTypeAndTargetId(ReviewTargetType targetType, UUID targetId);

    List<Review> findByClassIdAndReviewerIdAndTargetType(UUID classId, UUID reviewerId, ReviewTargetType targetType);

    Optional<Review> findByClassIdAndReviewerIdAndTargetTypeAndTargetId(
            UUID classId, UUID reviewerId, ReviewTargetType targetType, UUID targetId);
}
