package com.capstone.inventoryservice.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;

@Getter
@RequiredArgsConstructor
public enum EventSortOption {
    PRICE_ASC(Sort.by(Sort.Direction.ASC, "minPrice")),
    PRICE_DESC(Sort.by(Sort.Direction.DESC, "minPrice")),
    DATE_ASC(Sort.by(Sort.Direction.ASC, "createdAt")), // Or earliest showtime if formula added
    NEWEST(Sort.by(Sort.Direction.DESC, "createdAt")),
    POPULAR(Sort.by(Sort.Direction.DESC, "viewCount"));

    private final Sort sort;

    public static EventSortOption fromString(String value) {
        if (value == null) return NEWEST;
        try {
            return EventSortOption.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return NEWEST;
        }
    }
}
