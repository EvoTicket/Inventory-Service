package com.capstone.inventoryservice.domain.dto.response;

import com.capstone.inventoryservice.domain.client.OrgInternalResponse;
import com.capstone.inventoryservice.model.enums.EventApprovalStatus;
import com.capstone.inventoryservice.model.enums.EventStatus;
import com.capstone.inventoryservice.model.enums.EventType;
import com.capstone.inventoryservice.model.enums.EventCategory;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventResponse {
    private Long eventId;
    private String eventName;
    private OrgInternalResponse orgInternalResponse;
    private String description;
    private String venue;
    private String address;
    private EventStatus eventStatus;
    private EventApprovalStatus approvalStatus;
    private EventType eventType;
    private String bannerImage;
    private String thumbnailImage;
    private String introduction;
    private String seatMapImage;
    private Integer totalSeats;
    private Long organizerId;
    private Boolean isFeatured;
    private EventCategory category;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private List<ShowtimeResponse> showtimes;
    private List<ReviewResponse> reviews;
}
