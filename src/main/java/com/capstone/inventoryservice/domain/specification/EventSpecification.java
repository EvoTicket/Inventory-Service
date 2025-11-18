package com.capstone.inventoryservice.domain.specification;

import com.capstone.inventoryservice.domain.dto.request.EventFilterRequest;
import com.capstone.inventoryservice.model.entity.Event;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

public class EventSpecification {

    public static Specification<Event> withFilters(EventFilterRequest filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.getKeyword() != null && !filter.getKeyword().trim().isEmpty()) {
                String keyword = "%" + filter.getKeyword().toLowerCase() + "%";
                Predicate namePredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("eventName")), keyword
                );
                Predicate descPredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("description")), keyword
                );
                Predicate venuePredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("venue")), keyword
                );
                predicates.add(criteriaBuilder.or(namePredicate, descPredicate, venuePredicate));
            }


            if (filter.getCategoryIds() != null && !filter.getCategoryIds().isEmpty()) {
                predicates.add(root.get("category").get("id").in(filter.getCategoryIds()));
            }

            if (filter.getEventTypes() != null && !filter.getEventTypes().isEmpty()) {
                predicates.add(root.get("eventType").in(filter.getEventTypes()));
            }

            if (filter.getEventStatuses() != null && !filter.getEventStatuses().isEmpty()) {
                predicates.add(root.get("eventStatus").in(filter.getEventStatuses()));
            }

            if (filter.getProvinceCodes() != null && !filter.getProvinceCodes().isEmpty()) {
                predicates.add(root.get("province").get("code").in(filter.getProvinceCodes()));
            }

            if (filter.getIsFeatured() != null) {
                predicates.add(criteriaBuilder.equal(root.get("isFeatured"), filter.getIsFeatured()));
            }

            if (filter.getStartDate() != null) {
                OffsetDateTime startOfDay = filter.getStartDate().atStartOfDay().atOffset(ZoneOffset.ofHours(7));
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), startOfDay));
            }

            if (filter.getEndDate() != null) {
                OffsetDateTime endOfDay = filter.getEndDate().plusDays(1).atStartOfDay().atOffset(ZoneOffset.ofHours(7));
                predicates.add(criteriaBuilder.lessThan(root.get("createdAt"), endOfDay));
            }

            if (filter.getEventDate() != null) {
                OffsetDateTime startOfDay = filter.getEventDate().atStartOfDay().atOffset(ZoneOffset.ofHours(7));
                OffsetDateTime endOfDay = filter.getEventDate().plusDays(1).atStartOfDay().atOffset(ZoneOffset.ofHours(7));

                Predicate startBeforeOrEqual = criteriaBuilder.lessThanOrEqualTo(
                        root.get("startDatetime"), endOfDay
                );
                Predicate endAfter = criteriaBuilder.greaterThanOrEqualTo(
                        root.get("endDatetime"), startOfDay
                );
                predicates.add(criteriaBuilder.and(startBeforeOrEqual, endAfter));
            }

            if (filter.getMinSeats() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("totalSeats"), filter.getMinSeats()));
            }

            if (filter.getMaxSeats() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("totalSeats"), filter.getMaxSeats()));
            }

            if (filter.getIncludeExpired() != null && !filter.getIncludeExpired()) {
                OffsetDateTime now = OffsetDateTime.now();
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("endDatetime"), now));
            }

            query.distinct(true);
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}