package com.modoensayo.reviews.repository;

import com.modoensayo.reviews.domain.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
    List<Review> findByClassId(UUID classId);
    List<Review> findByReviewerId(UUID reviewerId);
    List<Review> findByTargetId(UUID targetId);
    List<Review> findByTargetTypeAndTargetId(String targetType, UUID targetId);
    List<Review> findTop30ByOrderByCreatedAtDesc();
}
