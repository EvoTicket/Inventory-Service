package com.capstone.inventoryservice.domain.dto.request;

import com.capstone.inventoryservice.model.enums.EventType;
import com.capstone.inventoryservice.model.enums.EventCategory;
import jakarta.validation.Valid;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateEventRequest {
    private String eventName;
    private String description;
    private String venue;
    private String address;
    private Boolean isCancelled;
    private EventType eventType;
    private Integer totalSeats;
    private Boolean isFeatured;
    private EventCategory category;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Long bankInfoId;
    private String shortDescription;
    private String contactEmail;
    private String contactPhone;
    private Boolean allowMultipleTicketTypesPerOrder;
    private Boolean allowDiscountCode;
    private Boolean allowResale;
    private String postPurchaseInstruction;
    private String checkInInstruction;
    private String entryGateInstruction;
    private String reconciliationNote;

    @Valid
    private List<UpdateShowtimeRequest> showtimes;
}
