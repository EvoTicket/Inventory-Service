package com.capstone.inventoryservice.model.repository;

import com.capstone.inventoryservice.model.entity.UserFavoriteEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserFavoriteEventRepository extends JpaRepository<UserFavoriteEvent, Long> {

    boolean existsByUserIdAndEventId(Long userId, Long eventId);

    Optional<UserFavoriteEvent> findByUserIdAndEventId(Long userId, Long eventId);

    @Query("SELECT ufe FROM UserFavoriteEvent ufe " +
            "WHERE ufe.userId = :userId " +
            "AND (:keyword IS NULL OR :keyword = '' OR " +
            "LOWER(ufe.event.eventName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(ufe.event.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<UserFavoriteEvent> findByUserIdWithSearch(
            @Param("userId") Long userId,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    List<UserFavoriteEvent> findByUserId(Long userId);
}