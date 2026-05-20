package com.capstone.inventoryservice.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "showtimes")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Showtime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "start_datetime", nullable = false)
    private LocalDateTime startDatetime;

    @Column(name = "end_datetime", nullable = false)
    private LocalDateTime endDatetime;

    @Column(name = "venue")
    private String venue;

    @Column(name = "address")
    private String address;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ward_code")
    private Ward ward;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "province_code")
    private Province province;

    @Column(name = "is_cancelled")
    @Builder.Default
    private Boolean isCancelled = false;

    @OneToMany(mappedBy = "showtime", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<TicketType> ticketTypes = new HashSet<>();

    @OneToMany(mappedBy = "showtime", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<ShowtimeChecker> checkers = new HashSet<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
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
}
