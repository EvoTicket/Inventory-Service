package com.capstone.inventoryservice.model.repository;

import com.capstone.inventoryservice.model.entity.Event;
import com.capstone.inventoryservice.model.enums.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    @Query("SELECT e FROM Event e WHERE e.startDatetime >= :startDate AND e.startDatetime <= :endDate")
    List<Event> findEventsByDateRange(@Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate);

    @Query("SELECT e FROM Event e WHERE e.eventName LIKE %:keyword% OR e.description LIKE %:keyword%")
    List<Event> searchEventsByKeyword(@Param("keyword") String keyword);

    @Query("SELECT e FROM Event e WHERE e.eventStatus = :status AND e.startDatetime > :currentDate ORDER BY e.startDatetime ASC")
    List<Event> findUpcomingEventsByStatus(@Param("status") EventStatus status,
                                           @Param("currentDate") LocalDateTime currentDate);

    @Query("SELECT e FROM Event e LEFT JOIN FETCH e.ticketTypes WHERE e.id = :eventId")
    Optional<Event> findByIdWithTicketTypes(@Param("eventId") Long eventId);

    @Query("SELECT ufe.event.id, COUNT(ufe.id) FROM UserFavoriteEvent ufe " +
            "WHERE ufe.event.id IN :eventIds " +
            "GROUP BY ufe.event.id")
    List<Object[]> countFavoritesByEventIds(@Param("eventIds") List<Long> eventIds);

    @Query("SELECT ufe.event.id FROM UserFavoriteEvent ufe " +
            "WHERE ufe.userId = :userId AND ufe.event.id IN :eventIds")
    List<Long> findFavoriteEventIdsByUserId(@Param("userId") Long userId,
                                            @Param("eventIds") List<Long> eventIds);

    List<Event> findByStartDatetimeAfter(OffsetDateTime dateTime);
    List<Event> findByCategoryId(Long categoryId);
}