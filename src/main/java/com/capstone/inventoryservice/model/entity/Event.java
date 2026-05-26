package com.capstone.inventoryservice.model.entity;

import com.capstone.inventoryservice.model.enums.*;
import jakarta.persistence.*;
import org.hibernate.annotations.Formula;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

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

    @Column(name = "event_name")
    private String eventName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "venue")
    private String venue;

    @Column(name = "address")
    private String address;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ward_code", referencedColumnName = "code")
    private Ward ward;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "province_code", referencedColumnName = "code")
    private Province province;

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

    @Column(name = "introduction", columnDefinition = "TEXT")
    private String introduction;

    @Column(name = "seat_map_image")
    private String seatMapImage;

    @Column(name = "total_seats")
    private Integer totalSeats;

    @Column(name = "organizer_id")
    private Long organizerId;

    @Column(name = "bank_info_id")
    private Long bankInfoId;

    @Column(name = "is_featured")
    private Boolean isFeatured;

    @Column(name = "short_description")
    private String shortDescription;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "contact_phone")
    private String contactPhone;

    @Column(name = "allow_multiple_ticket_types_per_order")
    @Builder.Default
    private Boolean allowMultipleTicketTypesPerOrder = false;

    @Column(name = "allow_discount_code")
    @Builder.Default
    private Boolean allowDiscountCode = false;

    @Column(name = "allow_resale")
    @Builder.Default
    private Boolean allowResale = false;

    @Column(name = "max_resale_price_percentage", precision = 5, scale = 4)
    private BigDecimal maxResalePricePercentage;

    @Column(name = "organizer_royalty_fee_percentage", precision = 5, scale = 4)
    private BigDecimal organizerRoyaltyFeePercentage;

    @Column(name = "post_purchase_instruction", columnDefinition = "TEXT")
    private String postPurchaseInstruction;

    @Column(name = "check_in_instruction", columnDefinition = "TEXT")
    private String checkInInstruction;

    @Column(name = "entry_gate_instruction", columnDefinition = "TEXT")
    private String entryGateInstruction;

    @Column(name = "reconciliation_note", columnDefinition = "TEXT")
    private String reconciliationNote;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false)
    @Builder.Default
    private EventApprovalStatus approvalStatus = EventApprovalStatus.DRAFT;

    @Column(name = "current_step")
    private Long currentStep;

    @Enumerated(EnumType.STRING)
    @Column(name = "category")
    private EventCategory category;

    @Formula("(SELECT MIN(t.price) FROM inventory_service.ticket_types t INNER JOIN inventory_service.showtimes s ON t.showtime_id = s.id WHERE s.event_id = id)")
    private BigDecimal minPrice;

    @Formula("(SELECT COALESCE(SUM(t.quantity_sold), 0) FROM inventory_service.ticket_types t INNER JOIN inventory_service.showtimes s ON t.showtime_id = s.id WHERE s.event_id = id)")
    private Integer totalQuantitySold;

    @Formula("(SELECT COALESCE(SUM(t.quantity_total), 0) FROM inventory_service.ticket_types t INNER JOIN inventory_service.showtimes s ON t.showtime_id = s.id WHERE s.event_id = id)")
    private Integer totalQuantityTotal;

    @Formula("(SELECT COUNT(v.id) FROM inventory_service.event_views v WHERE v.event_id = id)")
    private Integer viewCount;

    @Formula("(SELECT COUNT(sc.id) FROM inventory_service.showtime_checkers sc INNER JOIN inventory_service.showtimes s ON sc.showtime_id = s.id WHERE s.event_id = id)")
    private Integer checkers;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Showtime> showtimes = new HashSet<>();

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
        if (approvalStatus == null) {
            approvalStatus = EventApprovalStatus.DRAFT;
        }
        if (isCancelled == null) {
            isCancelled = false;
        }
        if (allowMultipleTicketTypesPerOrder == null) {
            allowMultipleTicketTypesPerOrder = false;
        }
        if (allowDiscountCode == null) {
            allowDiscountCode = false;
        }
        if (allowResale == null) {
            allowResale = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Transient
    public String getFullAddress() {
        String wardName = ward != null ? ward.getName() : "";
        String provinceName = province != null ? province.getName() : "";
        
        java.util.List<String> parts = new java.util.ArrayList<>();
        if (venue != null && !venue.isBlank()) parts.add(venue);
        if (address != null && !address.isBlank()) parts.add(address);
        if (!wardName.isEmpty()) parts.add(wardName);
        if (!provinceName.isEmpty()) parts.add(provinceName);
        
        return String.join(", ", parts);
    }

    @Transient
    private Stream<TicketType> streamTicketTypes() {
        if (showtimes == null) return Stream.empty();

        return showtimes.stream()
                .filter(s -> !Boolean.TRUE.equals(s.getIsCancelled()))
                .filter(s -> s.getTicketTypes() != null)
                .flatMap(s -> s.getTicketTypes().stream());
    }

    @Transient
    public int getTotalSold() {
        return streamTicketTypes()
                .map(t -> t.getQuantitySold() != null ? t.getQuantitySold() : 0)
                .mapToInt(Integer::intValue)
                .sum();
    }

    @Transient
    public TicketAvailabilityStatus getTicketAvailabilityStatus() {
        int totalSold = getTotalSold();
        int totalCapacity = getTotalCapacity();

        if (totalSold >= totalCapacity) {
            return TicketAvailabilityStatus.SOLD_OUT;
        }

        if (totalSold * 10 >= totalCapacity * 9) {
            return TicketAvailabilityStatus.ALMOST_SOLD_OUT;
        }

        return TicketAvailabilityStatus.AVAILABLE;
    }

    @Transient
    public int getTotalCapacity() {
        return streamTicketTypes()
                .map(t -> t.getQuantityTotal() != null ? t.getQuantityTotal() : 0)
                .mapToInt(Integer::intValue)
                .sum();
    }

    @Transient
    private LocalDateTime getShowtimeDate(
            Function<Showtime, LocalDateTime> mapper,
            Comparator<LocalDateTime> comparator) {

        if (showtimes == null) return null;

        return showtimes.stream()
                .filter(s -> !Boolean.TRUE.equals(s.getIsCancelled()))
                .map(mapper)
                .filter(Objects::nonNull)
                .min(comparator)
                .orElse(null);
    }

    @Transient
    public LocalDateTime getEarliestStart() {
        return getShowtimeDate(
                Showtime::getStartDatetime,
                Comparator.naturalOrder()
        );
    }

    @Transient
    public LocalDateTime getLatestEnd() {
        return getShowtimeDate(
                Showtime::getEndDatetime,
                Comparator.reverseOrder()
        );
    }

    @Transient
    private LocalDateTime getSaleDate(Function<TicketType, LocalDateTime> mapper,
                                      Comparator<LocalDateTime> comparator) {

        if (showtimes == null) return null;

        return streamTicketTypes()
                .map(mapper)
                .filter(Objects::nonNull)
                .min(comparator)
                .orElse(null);
    }

    @Transient
    public LocalDateTime getMinSaleStart() {
        return getSaleDate(
                TicketType::getSaleStartDate,
                Comparator.naturalOrder()
        );
    }

    @Transient
    public LocalDateTime getMaxSaleEnd() {
        return getSaleDate(
                TicketType::getSaleEndDate,
                Comparator.reverseOrder()
        );
    }

    @Transient
    public EventStatus getEventStatus() {
        if (this.isCancelled != null && this.isCancelled) {
            return EventStatus.CANCELLED;
        }

        LocalDateTime now = LocalDateTime.now();

        if (this.getLatestEnd() != null && now.isAfter(this.getLatestEnd())) {
            return EventStatus.COMPLETED;
        }

        if (this.getEarliestStart() != null && this.getLatestEnd() != null &&
            !now.isBefore(this.getEarliestStart()) && !now.isAfter(this.getLatestEnd())) {
            return EventStatus.ON_GOING;
        }


        if (this.getEarliestStart() != null && now.isBefore(this.getEarliestStart())) {
            return EventStatus.UPCOMING;
        }

        if (this.getMinSaleStart() != null && this.getMaxSaleEnd() != null &&
            !now.isBefore(this.getMinSaleStart()) && !now.isAfter(this.getMaxSaleEnd())) {
            return EventStatus.ON_SALE;
        }

        if (this.getMaxSaleEnd() != null && this.getEarliestStart() != null &&
            now.isAfter(this.getMaxSaleEnd()) && now.isBefore(this.getEarliestStart())) {
            return EventStatus.SALE_CLOSED;
        }

        return EventStatus.UPCOMING;
    }

    @Transient
    public BigDecimal getFloorPrice() {
        if (getShowtimes() == null) return BigDecimal.ZERO;

        return streamTicketTypes()
                .map(TicketType::getPrice)
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }
}
