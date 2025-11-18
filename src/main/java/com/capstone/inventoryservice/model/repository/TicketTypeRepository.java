package com.capstone.inventoryservice.model.repository;

import com.capstone.inventoryservice.model.entity.TicketType;
import com.capstone.inventoryservice.model.enums.TicketTypeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TicketTypeRepository extends JpaRepository<TicketType, Long> {

    List<TicketType> findByEventId(Long eventId);

    List<TicketType> findByTicketTypeStatus(TicketTypeStatus status);

    @Query("SELECT t FROM TicketType t WHERE t.event.id = :eventId AND t.ticketTypeStatus = :status")
    List<TicketType> findByEventAndStatus(@Param("eventId") Long eventId,
                                          @Param("status") TicketTypeStatus status);

    @Query("SELECT t FROM TicketType t WHERE t.saleStartDate <= :currentDate AND t.saleEndDate >= :currentDate")
    List<TicketType> findActiveTicketTypes(@Param("currentDate") LocalDateTime currentDate);

    @Modifying
    @Query("UPDATE TicketType t SET t.quantitySold = t.quantitySold + :quantity WHERE t.id = :ticketTypeId")
    int incrementQuantitySold(@Param("ticketTypeId") Long ticketTypeId,
                              @Param("quantity") Integer quantity);

    @Modifying
    @Query("UPDATE TicketType t SET t.quantityAvailable = t.quantityAvailable - :quantity WHERE t.id = :ticketTypeId")
    int decrementQuantityAvailable(@Param("ticketTypeId") Long ticketTypeId,
                                   @Param("quantity") Integer quantity);

    @Query("SELECT t FROM TicketType t WHERE t.id = :ticketTypeId AND t.quantityAvailable >= :requestedQuantity")
    Optional<TicketType> findAvailableTicket(@Param("ticketTypeId") Long ticketTypeId,
                                             @Param("requestedQuantity") Integer requestedQuantity);
}