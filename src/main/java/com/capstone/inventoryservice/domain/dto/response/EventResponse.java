package com.capstone.inventoryservice.domain.dto.response;

import com.capstone.inventoryservice.domain.client.OrgClientResponse;
import com.capstone.inventoryservice.model.enums.EventStatus;
import com.capstone.inventoryservice.model.enums.EventType;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventResponse {
    private Long eventId;
    private String eventName;
    private OrgClientResponse orgClientResponse;
    private String description;
    private String venue;
    private String address;
    private OffsetDateTime startDatetime;
    private OffsetDateTime endDatetime;
    private EventStatus eventStatus;
    private EventType eventType;
    private String bannerImage;
    private String thumbnailImage;
    private Integer totalSeats;
    private Long organizerId;
    private Boolean isFeatured;
    private Long categoryId;
    private String categoryName;
    private List<TicketTypeResponse> ticketTypes;
}
