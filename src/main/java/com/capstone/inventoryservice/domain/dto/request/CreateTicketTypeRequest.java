package com.capstone.inventoryservice.domain.dto.request;

import com.capstone.inventoryservice.model.enums.TicketTypeStatus;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTicketTypeRequest {

    @NotNull(message = "Event id is required")
    private Long eventId;

    @NotBlank(message = "Type name is required")
    private String typeName;

    private String description;

    private OffsetDateTime takePlaceTime;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal price;

    @NotNull(message = "Quantity available is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantityAvailable;

    @Min(value = 1, message = "Min purchase must be at least 1")
    private Integer minPurchase;

    @Min(value = 1, message = "Max purchase must be at least 1")
    private Integer maxPurchase;

    private OffsetDateTime saleStartDate;
    private OffsetDateTime saleEndDate;

    @NotNull(message = "Ticket type status is required")
    private TicketTypeStatus ticketTypeStatus;
}
