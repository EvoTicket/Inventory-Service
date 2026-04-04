package com.capstone.inventoryservice.domain.specification;

import com.capstone.inventoryservice.domain.dto.request.EventFilterRequest;
import com.capstone.inventoryservice.model.enums.EventStatus;
import com.capstone.inventoryservice.model.enums.TicketAvailabilityStatus;
import com.capstone.inventoryservice.model.entity.Event;
import com.capstone.inventoryservice.model.entity.TicketType;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
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


            if (filter.getCategories() != null && !filter.getCategories().isEmpty()) {
                predicates.add(root.get("category").in(filter.getCategories()));
            }

            if (filter.getEventTypes() != null && !filter.getEventTypes().isEmpty()) {
                predicates.add(root.get("eventType").in(filter.getEventTypes()));
            }

            if (filter.getEventStatuses() != null && !filter.getEventStatuses().isEmpty()) {
                List<Predicate> statusPredicates = new ArrayList<>();
                LocalDateTime now = LocalDateTime.now();

                for (com.capstone.inventoryservice.model.enums.EventStatus status : filter.getEventStatuses()) {
                    switch (status) {
                        case CANCELLED:
                            statusPredicates.add(criteriaBuilder.isTrue(root.get("isCancelled")));
                            break;
                        case COMPLETED:
                            statusPredicates.add(criteriaBuilder.and(
                                    criteriaBuilder.isFalse(root.get("isCancelled")),
                                    criteriaBuilder.lessThan(root.get("endDatetime"), now)
                            ));
                            break;
                        case ON_GOING:
                            statusPredicates.add(criteriaBuilder.and(
                                    criteriaBuilder.isFalse(root.get("isCancelled")),
                                    criteriaBuilder.lessThanOrEqualTo(root.get("startDatetime"), now),
                                    criteriaBuilder.greaterThanOrEqualTo(root.get("endDatetime"), now)
                            ));
                            break;
                        case UPCOMING:
                            Subquery<LocalDateTime> upMinSub = query.subquery(LocalDateTime.class);
                            Root<TicketType> upTicket = upMinSub.from(TicketType.class);
                            upMinSub.select(criteriaBuilder.least(upTicket.<LocalDateTime>get("saleStartDate")));
                            upMinSub.where(criteriaBuilder.equal(upTicket.get("event"), root));

                            Predicate upCond1 = criteriaBuilder.and(
                                    criteriaBuilder.isNotNull(upMinSub),
                                    criteriaBuilder.lessThan(criteriaBuilder.literal(now), upMinSub)
                            );
                            Predicate upCond2 = criteriaBuilder.and(
                                    criteriaBuilder.isNull(upMinSub),
                                    criteriaBuilder.lessThan(criteriaBuilder.literal(now), root.get("startDatetime"))
                            );

                            statusPredicates.add(criteriaBuilder.and(
                                    criteriaBuilder.isFalse(root.get("isCancelled")),
                                    criteriaBuilder.or(upCond1, upCond2)
                            ));
                            break;
                        case ON_SALE:
                            Subquery<LocalDateTime> onSaleMin = query.subquery(LocalDateTime.class);
                            Root<TicketType> onSaleTicketMin = onSaleMin.from(TicketType.class);
                            onSaleMin.select(criteriaBuilder.least(onSaleTicketMin.<LocalDateTime>get("saleStartDate")));
                            onSaleMin.where(criteriaBuilder.equal(onSaleTicketMin.get("event"), root));

                            Subquery<LocalDateTime> onSaleMax = query.subquery(LocalDateTime.class);
                            Root<TicketType> onSaleTicketMax = onSaleMax.from(TicketType.class);
                            onSaleMax.select(criteriaBuilder.greatest(onSaleTicketMax.<LocalDateTime>get("saleEndDate")));
                            onSaleMax.where(criteriaBuilder.equal(onSaleTicketMax.get("event"), root));

                            Predicate saleRange = criteriaBuilder.and(
                                    criteriaBuilder.greaterThanOrEqualTo(criteriaBuilder.literal(now), onSaleMin),
                                    criteriaBuilder.lessThanOrEqualTo(criteriaBuilder.literal(now), onSaleMax)
                            );
                            statusPredicates.add(criteriaBuilder.and(
                                    criteriaBuilder.isFalse(root.get("isCancelled")),
                                    criteriaBuilder.isNotNull(onSaleMin),
                                    saleRange
                            ));
                            break;
                        case SALE_CLOSED:
                            Subquery<LocalDateTime> closedMax = query.subquery(LocalDateTime.class);
                            Root<TicketType> closedTicketMax = closedMax.from(TicketType.class);
                            closedMax.select(criteriaBuilder.greatest(closedTicketMax.<LocalDateTime>get("saleEndDate")));
                            closedMax.where(criteriaBuilder.equal(closedTicketMax.get("event"), root));

                            Predicate closedCond = criteriaBuilder.and(
                                    criteriaBuilder.greaterThan(criteriaBuilder.literal(now), closedMax),
                                    criteriaBuilder.lessThan(criteriaBuilder.literal(now), root.get("startDatetime"))
                            );
                            statusPredicates.add(criteriaBuilder.and(
                                    criteriaBuilder.isFalse(root.get("isCancelled")),
                                    criteriaBuilder.isNotNull(closedMax),
                                    closedCond
                            ));
                            break;
                    }
                }
                if (!statusPredicates.isEmpty()) {
                    predicates.add(criteriaBuilder.or(statusPredicates.toArray(new Predicate[0])));
                }
            }

            if (filter.getProvinceCodes() != null && !filter.getProvinceCodes().isEmpty()) {
                predicates.add(root.get("province").get("code").in(filter.getProvinceCodes()));
            }

            if (filter.getIsFeatured() != null) {
                predicates.add(criteriaBuilder.equal(root.get("isFeatured"), filter.getIsFeatured()));
            }

            if (filter.getStartDate() != null) {
                LocalDateTime startOfDay = filter.getStartDate().atStartOfDay();
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), startOfDay));
            }

            if (filter.getEndDate() != null) {
                LocalDateTime endOfDay = filter.getEndDate().plusDays(1).atStartOfDay();
                predicates.add(criteriaBuilder.lessThan(root.get("createdAt"), endOfDay));
            }

            if (filter.getEventDate() != null) {
                LocalDateTime startOfDay = filter.getEventDate().atStartOfDay();
                LocalDateTime endOfDay = filter.getEventDate().plusDays(1).atStartOfDay();

                Predicate startBeforeOrEqual = criteriaBuilder.lessThanOrEqualTo(
                        root.get("startDatetime"), endOfDay
                );
                Predicate endAfter = criteriaBuilder.greaterThanOrEqualTo(
                        root.get("endDatetime"), startOfDay
                );
                predicates.add(criteriaBuilder.and(startBeforeOrEqual, endAfter));
            }

            if (filter.getMinPrice() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("minPrice"), filter.getMinPrice()));
            }

            if (filter.getMaxPrice() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("minPrice"), filter.getMaxPrice()));
            }

            if (filter.getTicketAvailabilityStatuses() != null && !filter.getTicketAvailabilityStatuses().isEmpty()) {
                List<Predicate> availabilityPreds = new ArrayList<>();
                Expression<Integer> soldExpr = root.get("totalQuantitySold");
                Expression<Integer> totalExpr = root.get("totalQuantityTotal");

                for (TicketAvailabilityStatus status : filter.getTicketAvailabilityStatuses()) {
                    switch (status) {
                        case SOLD_OUT:
                            availabilityPreds.add(criteriaBuilder.greaterThanOrEqualTo(soldExpr, totalExpr));
                            break;
                        case ALMOST_SOLD_OUT:
                            availabilityPreds.add(criteriaBuilder.and(
                                    criteriaBuilder.greaterThanOrEqualTo(criteriaBuilder.prod(soldExpr, 10).as(Integer.class), criteriaBuilder.prod(totalExpr, 9).as(Integer.class)),
                                    criteriaBuilder.lessThan(soldExpr, totalExpr)
                            ));
                            break;
                        case AVAILABLE:
                            availabilityPreds.add(criteriaBuilder.lessThan(criteriaBuilder.prod(soldExpr, 10).as(Integer.class), criteriaBuilder.prod(totalExpr, 9).as(Integer.class)));
                            break;
                    }
                }
                if (!availabilityPreds.isEmpty()) {
                    predicates.add(criteriaBuilder.or(availabilityPreds.toArray(new Predicate[0])));
                }
            }

            if (filter.getIncludeExpired() != null && !filter.getIncludeExpired()) {
                LocalDateTime now = LocalDateTime.now();
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("endDatetime"), now));
            }

            query.distinct(true);
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}