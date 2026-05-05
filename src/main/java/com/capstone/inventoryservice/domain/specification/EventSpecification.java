package com.capstone.inventoryservice.domain.specification;

import com.capstone.inventoryservice.domain.dto.request.EventFilterRequest;
import com.capstone.inventoryservice.model.enums.EventStatus;
import com.capstone.inventoryservice.model.enums.TicketAvailabilityStatus;
import com.capstone.inventoryservice.model.entity.Event;
import com.capstone.inventoryservice.model.entity.Showtime;
import com.capstone.inventoryservice.model.entity.TicketType;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class EventSpecification {

    private EventSpecification() {
        throw new UnsupportedOperationException("Utility class");
    }

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

            if (filter.getOrganizerId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("organizerId"), filter.getOrganizerId()));
            }

            if (filter.getApprovalStatuses() != null && !filter.getApprovalStatuses().isEmpty()) {
                predicates.add(root.get("approvalStatus").in(filter.getApprovalStatuses()));
            }

            if (filter.getEventStatuses() != null && !filter.getEventStatuses().isEmpty()) {
                List<Predicate> statusPredicates = new ArrayList<>();
                LocalDateTime now = LocalDateTime.now();

                Join<Event, Showtime> showtimeJoin = root.join("showtimes", JoinType.LEFT);
                Join<Showtime, TicketType> ticketTypeJoin = showtimeJoin.join("ticketTypes", JoinType.LEFT);

                for (EventStatus status : filter.getEventStatuses()) {
                    switch (status) {
                        case CANCELLED:
                            statusPredicates.add(criteriaBuilder.isTrue(root.get("isCancelled")));
                            break;
                        case COMPLETED:
                            Subquery<LocalDateTime> completedMaxEnd = query.subquery(LocalDateTime.class);
                            Root<Showtime> completedShowtime = completedMaxEnd.from(Showtime.class);
                            completedMaxEnd.select(criteriaBuilder.greatest(completedShowtime.<LocalDateTime>get("endDatetime")));
                            completedMaxEnd.where(criteriaBuilder.equal(completedShowtime.get("event"), root),
                                    criteriaBuilder.isFalse(completedShowtime.get("isCancelled")));

                            statusPredicates.add(criteriaBuilder.and(
                                    criteriaBuilder.isFalse(root.get("isCancelled")),
                                    criteriaBuilder.lessThan(completedMaxEnd, now)
                            ));
                            break;
                        case ON_GOING:
                            Subquery<LocalDateTime> onGoingMinStart = query.subquery(LocalDateTime.class);
                            Root<Showtime> ogStart = onGoingMinStart.from(Showtime.class);
                            onGoingMinStart.select(criteriaBuilder.least(ogStart.<LocalDateTime>get("startDatetime")));
                            onGoingMinStart.where(criteriaBuilder.equal(ogStart.get("event"), root),
                                    criteriaBuilder.isFalse(ogStart.get("isCancelled")));

                            Subquery<LocalDateTime> onGoingMaxEnd = query.subquery(LocalDateTime.class);
                            Root<Showtime> ogEnd = onGoingMaxEnd.from(Showtime.class);
                            onGoingMaxEnd.select(criteriaBuilder.greatest(ogEnd.<LocalDateTime>get("endDatetime")));
                            onGoingMaxEnd.where(criteriaBuilder.equal(ogEnd.get("event"), root),
                                    criteriaBuilder.isFalse(ogEnd.get("isCancelled")));

                            statusPredicates.add(criteriaBuilder.and(
                                    criteriaBuilder.isFalse(root.get("isCancelled")),
                                    criteriaBuilder.lessThanOrEqualTo(onGoingMinStart, now),
                                    criteriaBuilder.greaterThanOrEqualTo(onGoingMaxEnd, now)
                            ));
                            break;
                        case UPCOMING:
                            Subquery<LocalDateTime> upMinSaleStart = query.subquery(LocalDateTime.class);
                            Root<TicketType> upTicket = upMinSaleStart.from(TicketType.class);
                            Join<TicketType, Showtime> upShowtime = upTicket.join("showtime");
                            upMinSaleStart.select(criteriaBuilder.least(upTicket.<LocalDateTime>get("saleStartDate")));
                            upMinSaleStart.where(criteriaBuilder.equal(upShowtime.get("event"), root));

                            Subquery<LocalDateTime> upEarliestStart = query.subquery(LocalDateTime.class);
                            Root<Showtime> upShow = upEarliestStart.from(Showtime.class);
                            upEarliestStart.select(criteriaBuilder.least(upShow.<LocalDateTime>get("startDatetime")));
                            upEarliestStart.where(criteriaBuilder.equal(upShow.get("event"), root),
                                    criteriaBuilder.isFalse(upShow.get("isCancelled")));

                            Predicate upCond1 = criteriaBuilder.and(
                                    criteriaBuilder.isNotNull(upMinSaleStart),
                                    criteriaBuilder.lessThan(criteriaBuilder.literal(now), upMinSaleStart)
                            );
                            Predicate upCond2 = criteriaBuilder.and(
                                    criteriaBuilder.isNull(upMinSaleStart),
                                    criteriaBuilder.lessThan(criteriaBuilder.literal(now), upEarliestStart)
                            );

                            statusPredicates.add(criteriaBuilder.and(
                                    criteriaBuilder.isFalse(root.get("isCancelled")),
                                    criteriaBuilder.or(upCond1, upCond2)
                            ));
                            break;
                        case ON_SALE:
                            Subquery<LocalDateTime> onSaleMin = query.subquery(LocalDateTime.class);
                            Root<TicketType> onSaleTicketMin = onSaleMin.from(TicketType.class);
                            Join<TicketType, Showtime> onSaleShowtimeMin = onSaleTicketMin.join("showtime");
                            onSaleMin.select(criteriaBuilder.least(onSaleTicketMin.<LocalDateTime>get("saleStartDate")));
                            onSaleMin.where(criteriaBuilder.equal(onSaleShowtimeMin.get("event"), root));

                            Subquery<LocalDateTime> onSaleMax = query.subquery(LocalDateTime.class);
                            Root<TicketType> onSaleTicketMax = onSaleMax.from(TicketType.class);
                            Join<TicketType, Showtime> onSaleShowtimeMax = onSaleTicketMax.join("showtime");
                            onSaleMax.select(criteriaBuilder.greatest(onSaleTicketMax.<LocalDateTime>get("saleEndDate")));
                            onSaleMax.where(criteriaBuilder.equal(onSaleShowtimeMax.get("event"), root));

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
                            Join<TicketType, Showtime> closedShowtimeMax = closedTicketMax.join("showtime");
                            closedMax.select(criteriaBuilder.greatest(closedTicketMax.<LocalDateTime>get("saleEndDate")));
                            closedMax.where(criteriaBuilder.equal(closedShowtimeMax.get("event"), root));

                            Subquery<LocalDateTime> closedEarliestStart = query.subquery(LocalDateTime.class);
                            Root<Showtime> closedShow = closedEarliestStart.from(Showtime.class);
                            closedEarliestStart.select(criteriaBuilder.least(closedShow.<LocalDateTime>get("startDatetime")));
                            closedEarliestStart.where(criteriaBuilder.equal(closedShow.get("event"), root),
                                    criteriaBuilder.isFalse(closedShow.get("isCancelled")));

                            Predicate closedCond = criteriaBuilder.and(
                                    criteriaBuilder.greaterThan(criteriaBuilder.literal(now), closedMax),
                                    criteriaBuilder.lessThan(criteriaBuilder.literal(now), closedEarliestStart)
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

                Subquery<Long> eventDateSubquery = query.subquery(Long.class);
                Root<Showtime> showtimeSub = eventDateSubquery.from(Showtime.class);
                eventDateSubquery.select(showtimeSub.get("event").get("id"));
                eventDateSubquery.where(criteriaBuilder.and(
                        criteriaBuilder.equal(showtimeSub.get("event"), root),
                        criteriaBuilder.lessThanOrEqualTo(showtimeSub.get("startDatetime"), endOfDay),
                        criteriaBuilder.greaterThanOrEqualTo(showtimeSub.get("endDatetime"), startOfDay),
                        criteriaBuilder.isFalse(showtimeSub.get("isCancelled"))
                ));

                predicates.add(criteriaBuilder.exists(eventDateSubquery));
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
                Subquery<LocalDateTime> notExpiredMaxEnd = query.subquery(LocalDateTime.class);
                Root<Showtime> notExpiredShowtime = notExpiredMaxEnd.from(Showtime.class);
                notExpiredMaxEnd.select(criteriaBuilder.greatest(notExpiredShowtime.<LocalDateTime>get("endDatetime")));
                notExpiredMaxEnd.where(criteriaBuilder.equal(notExpiredShowtime.get("event"), root),
                        criteriaBuilder.isFalse(notExpiredShowtime.get("isCancelled")));

                predicates.add(criteriaBuilder.greaterThanOrEqualTo(notExpiredMaxEnd, now));
            }

            query.distinct(true);
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
