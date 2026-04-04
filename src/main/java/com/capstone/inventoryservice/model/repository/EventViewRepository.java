package com.capstone.inventoryservice.model.repository;

import com.capstone.inventoryservice.model.entity.EventView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventViewRepository extends JpaRepository<EventView, Long> {
    
    @Query("SELECT COUNT(v) FROM EventView v WHERE v.event.id = :eventId AND v.userId = :userId AND v.createdAt >= :timeLimit")
    long countRecentViewsByUser(@Param("eventId") Long eventId, @Param("userId") Long userId, @Param("timeLimit") LocalDateTime timeLimit);
    
    @Query("SELECT v.event.id FROM EventView v WHERE v.userId = :userId GROUP BY v.event.id ORDER BY MAX(v.createdAt) DESC")
    List<Long> findViewedEventIdsByUserId(@Param("userId") Long userId);

    @Query("SELECT v.event.id, COUNT(v.id) FROM EventView v WHERE v.userId = :userId GROUP BY v.event.id")
    List<Object[]> countViewsByEventForUser(@Param("userId") Long userId);
}
