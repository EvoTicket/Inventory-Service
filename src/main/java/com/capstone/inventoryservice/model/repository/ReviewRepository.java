package com.capstone.inventoryservice.model.repository;

import com.capstone.inventoryservice.model.entity.Event;
import com.capstone.inventoryservice.model.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByEvent(Event event);

    List<Review> findByEventIdOrderByCreatedAtAsc(Long eventId);

    List<Review> findByUserId(Long userId);

    Optional<Review> findByIdAndUserId(Long id, Long userId);

    boolean existsByEventIdAndUserId(Long eventId, Long userId);
}