package com.capstone.inventoryservice.domain.dto.request;

import com.capstone.inventoryservice.model.enums.EventStatus;
import com.capstone.inventoryservice.model.enums.EventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventFilterRequest {

    private String keyword;

    private List<Long> categoryIds;
    private List<EventType> eventTypes;
    private List<EventStatus> eventStatuses;
    private List<Integer> provinceCodes;
    private Boolean isFeatured;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate eventDate;

    private Integer minSeats;
    private Integer maxSeats;

    private Boolean includeExpired;

    private Integer page = 0;
    private Integer size = 20;

    private String sortBy = "createdAt";
    private String sortDirection = "DESC";
}