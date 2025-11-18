package com.capstone.inventoryservice.model.entity;

import com.capstone.inventoryservice.model.enums.TicketTypeStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "ticket_types")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "type_name", nullable = false)
    private String typeName;

    @Column(name = "description")
    private String description;

    @Column(name = "price", nullable = false)
    private BigDecimal price;

    @Column(name = "take_place_time")
    private OffsetDateTime takePlaceTime;

    @Column(name = "quantity_available", nullable = false)
    private Integer quantityAvailable;

    @Column(name = "quantity_sold")
    private Integer quantitySold;

    @Column(name = "min_purchase")
    private Integer minPurchase;

    @Column(name = "max_purchase")
    private Integer maxPurchase;

    @Column(name = "sale_start_date")
    private OffsetDateTime saleStartDate;

    @Column(name = "sale_end_date")
    private OffsetDateTime saleEndDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "ticket_type_status")
    private TicketTypeStatus ticketTypeStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
        if (quantitySold == null) {
            quantitySold = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}