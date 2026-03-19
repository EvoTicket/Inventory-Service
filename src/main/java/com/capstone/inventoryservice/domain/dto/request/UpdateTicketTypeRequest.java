package com.capstone.inventoryservice.domain.dto.request;

import com.capstone.inventoryservice.model.enums.TicketTypeStatus;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTicketTypeRequest {
    private String typeName;
    private String description;
    private BigDecimal price;
    private LocalDateTime takePlaceTime;
    private Integer quantityAvailable;
    private Integer minPurchase;
    private Integer maxPurchase;
    private LocalDateTime saleStartDate;
    private LocalDateTime saleEndDate;
    private TicketTypeStatus ticketTypeStatus;
}