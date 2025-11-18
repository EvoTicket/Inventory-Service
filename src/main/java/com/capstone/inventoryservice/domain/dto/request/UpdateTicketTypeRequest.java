package com.capstone.inventoryservice.domain.dto.request;

import com.capstone.inventoryservice.model.enums.TicketTypeStatus;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTicketTypeRequest {
    private String typeName;
    private String description;
    private BigDecimal price;
    private OffsetDateTime takePlaceTime;
    private Integer quantityAvailable;
    private Integer minPurchase;
    private Integer maxPurchase;
    private OffsetDateTime saleStartDate;
    private OffsetDateTime saleEndDate;
    private TicketTypeStatus ticketTypeStatus;
}