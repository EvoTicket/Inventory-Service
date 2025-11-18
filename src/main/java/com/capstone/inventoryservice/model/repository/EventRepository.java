package com.capstone.inventoryservice.model.repository;

import com.capstone.inventoryservice.model.entity.Event;
import com.capstone.inventoryservice.model.enums.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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
}