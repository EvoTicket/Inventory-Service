package com.capstone.inventoryservice.model.entity;

import com.capstone.inventoryservice.model.enums.EventStatus;
import com.capstone.inventoryservice.model.enums.EventType;
import jakarta.persistence.*;
import org.hibernate.annotations.Formula;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "events")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "event_name", nullable = false)
    private String eventName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "venue")
    private String venue;

    @Column(name = "address")
    private String address;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ward_code", referencedColumnName = "code",  nullable = false)
    private Ward ward;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "province_code", referencedColumnName = "code",  nullable = false)
    private Province province;

    @Column(name = "start_datetime")
    private LocalDateTime startDatetime;

    @Column(name = "end_datetime")
    private LocalDateTime endDatetime;

    @Column(name = "is_cancelled")
    @Builder.Default
    private Boolean isCancelled = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type")
    private EventType eventType;

    @Column(name = "banner_image")
    private String bannerImage;

    @Column(name = "thumbnail_image")
    private String thumbnailImage;

    @Column(name = "total_seats")
    private Integer totalSeats;

    @Column(name = "organizer_id")
    private Long organizerId;

    @Column(name = "is_featured")
    private Boolean isFeatured;

    @Enumerated(EnumType.STRING)
    @Column(name = "category")
    private com.capstone.inventoryservice.model.enums.EventCategory category;

    @Formula("(SELECT MIN(t.price) FROM inventory_service.ticket_types t WHERE t.event_id = id)")
    private BigDecimal minPrice;

    @Formula("(SELECT COALESCE(SUM(t.quantity_sold), 0) FROM inventory_service.ticket_types t WHERE t.event_id = id)")
    private Integer totalQuantitySold;

    @Formula("(SELECT COALESCE(SUM(t.quantity_total), 0) FROM inventory_service.ticket_types t WHERE t.event_id = id)")
    private Integer totalQuantityTotal;

    @Formula("(SELECT COUNT(v.id) FROM inventory_service.event_views v WHERE v.event_id = id)")
    private Integer viewCount;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<TicketType> ticketTypes = new HashSet<>();

    private BigDecimal latitude;

    private BigDecimal longitude;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Review> reviews = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public String getFullAddress() {
        if (ward != null && province != null) {
            return address + ", " + ward.getName() + ", " + province.getName();
        }
        return address;
    }

    @Transient
    public EventStatus getEventStatus() {
        if (this.isCancelled != null && this.isCancelled) {
            return EventStatus.CANCELLED;
        }

        LocalDateTime now = LocalDateTime.now();

        if (this.endDatetime != null && now.isAfter(this.endDatetime)) {
            return EventStatus.COMPLETED;
        }

        if (this.startDatetime != null && this.endDatetime != null &&
            !now.isBefore(this.startDatetime) && !now.isAfter(this.endDatetime)) {
            return EventStatus.ON_GOING;
        }

        LocalDateTime minSaleStart = null;
        LocalDateTime maxSaleEnd = null;

        if (this.ticketTypes != null && !this.ticketTypes.isEmpty()) {
            for (TicketType t : this.ticketTypes) {
                if (t.getSaleStartDate() != null) {
                    if (minSaleStart == null || t.getSaleStartDate().isBefore(minSaleStart)) {
                        minSaleStart = t.getSaleStartDate();
                    }
                }
                if (t.getSaleEndDate() != null) {
                    if (maxSaleEnd == null || t.getSaleEndDate().isAfter(maxSaleEnd)) {
                        maxSaleEnd = t.getSaleEndDate();
                    }
                }
            }
        }

        if (minSaleStart != null && now.isBefore(minSaleStart)) {
            return EventStatus.UPCOMING;
        }

        if (minSaleStart != null && maxSaleEnd != null && 
            !now.isBefore(minSaleStart) && !now.isAfter(maxSaleEnd)) {
            return EventStatus.ON_SALE;
        }

        if (maxSaleEnd != null && this.startDatetime != null &&
            now.isAfter(maxSaleEnd) && now.isBefore(this.startDatetime)) {
            return EventStatus.SALE_CLOSED;
        }

        return EventStatus.UPCOMING;
    }
}