package com.capstone.inventoryservice.model.repository;

import com.capstone.inventoryservice.model.entity.Showtime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ShowtimeRepository extends JpaRepository<Showtime, Long>, JpaSpecificationExecutor<Showtime> {

    List<Showtime> findByEventId(Long eventId);

    @Query("""
            SELECT s FROM Showtime s
            LEFT JOIN FETCH s.ticketTypes
            WHERE s.event.id = :eventId
            """)
    List<Showtime> findByEventIdWithTicketTypes(@Param("eventId") Long eventId);

    @Query("""
            SELECT s FROM Showtime s
            WHERE s.event.id = :eventId
            AND s.isCancelled = false
            AND s.endDatetime > :now
            """)
    List<Showtime> findActiveShowtimesByEventId(@Param("eventId") Long eventId, @Param("now") LocalDateTime now);

    @Query("""
            SELECT s FROM Showtime s
            WHERE s.startDatetime >= :now
            AND s.isCancelled = false
            ORDER BY s.startDatetime ASC
            """)
    List<Showtime> findUpcomingShowtimes(@Param("now") LocalDateTime now);
}
