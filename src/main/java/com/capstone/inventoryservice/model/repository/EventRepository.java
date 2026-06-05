package com.capstone.inventoryservice.model.repository;

import com.capstone.inventoryservice.model.entity.Event;
import com.capstone.inventoryservice.model.enums.EventApprovalStatus;

import com.capstone.inventoryservice.model.enums.EventCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    @Query("SELECT e FROM Event e " +
            "LEFT JOIN e.showtimes s " +
            "LEFT JOIN s.ticketTypes tt " +
            "WHERE e.isCancelled = false AND e.approvalStatus = com.capstone.inventoryservice.model.enums.EventApprovalStatus.PUBLISHED AND e.id IN (" +
            "  SELECT e2.id FROM Event e2 JOIN e2.showtimes s2 WHERE s2.endDatetime > :now" +
            ") " +
            "GROUP BY e " +
            "ORDER BY (COALESCE(SUM(tt.quantitySold), 0) * 5 + (SELECT COUNT(v) FROM EventView v WHERE v.event = e)) DESC, " +
            "e.createdAt ASC")
    Page<Event> findTrendingEvents(@Param("now") LocalDateTime now, Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.isCancelled = false AND e.approvalStatus = com.capstone.inventoryservice.model.enums.EventApprovalStatus.PUBLISHED ORDER BY e.viewCount DESC")
    Page<Event> findMostViewedEvents(Pageable pageable);

    @Query("""
            SELECT DISTINCT e
            FROM Event e
            LEFT JOIN FETCH e.showtimes s
            LEFT JOIN FETCH s.ticketTypes tt
            WHERE e.id = :eventId
            """)
    Optional<Event> findByIdWithDetails(@Param("eventId") Long eventId);

    @Query("SELECT ufe.event.id, COUNT(ufe.id) FROM UserFavoriteEvent ufe " +
            "WHERE ufe.event.id IN :eventIds " +
            "GROUP BY ufe.event.id")
    List<Object[]> countFavoritesByEventIds(@Param("eventIds") List<Long> eventIds);

    @Query("SELECT ufe.event.id FROM UserFavoriteEvent ufe " +
            "WHERE ufe.userId = :userId AND ufe.event.id IN :eventIds")
    List<Long> findFavoriteEventIdsByUserId(@Param("userId") Long userId,
                                            @Param("eventIds") List<Long> eventIds);


    @Query("""
                SELECT e FROM Event e 
                WHERE e.isCancelled = false 
                  AND e.approvalStatus = com.capstone.inventoryservice.model.enums.EventApprovalStatus.PUBLISHED
                  AND e.id IN (SELECT s.event.id FROM Showtime s WHERE s.startDatetime > :now AND s.startDatetime <= :oneMonthLater)
                ORDER BY e.createdAt ASC 
            """)
    Page<Event> findUpcomingEvents(
            @Param("now") LocalDateTime now,
            @Param("oneMonthLater") LocalDateTime oneMonthLater,
            Pageable pageable
    );

    @Query("SELECT e FROM Event e WHERE e.category = :category AND e.isCancelled = false AND e.approvalStatus = com.capstone.inventoryservice.model.enums.EventApprovalStatus.PUBLISHED")
    Page<Event> findAcceptedByCategory(@Param("category") EventCategory category, Pageable pageable);

    @Query("SELECT DISTINCT e FROM Event e " +
            "LEFT JOIN FETCH e.ward " +
            "LEFT JOIN FETCH e.province " +
            "LEFT JOIN FETCH e.showtimes s " +
            "LEFT JOIN FETCH s.ticketTypes " +
            "WHERE e.organizerId = :organizerId")
    List<Event> findByOrganizerId(@Param("organizerId") Long organizerId);

    Page<Event> findByOrganizerId(Long organizerId, Pageable pageable);
    
    @Query("SELECT DISTINCT e FROM Event e " +
            "WHERE e.isCancelled = false " +
            "AND e.approvalStatus = com.capstone.inventoryservice.model.enums.EventApprovalStatus.PUBLISHED " +
            "AND e.id IN (SELECT s.event.id FROM Showtime s WHERE s.endDatetime > :now)")
    List<Event> findCandidateEvents(@Param("now") LocalDateTime now);

    void deleteByApprovalStatusAndCreatedAtBefore(EventApprovalStatus status, LocalDateTime dateTime);

    long countByOrganizerIdAndApprovalStatus(Long organizerId, EventApprovalStatus status);

    long countByApprovalStatus(EventApprovalStatus approvalStatus);
}

