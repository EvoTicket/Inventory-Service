package com.capstone.inventoryservice.domain.dto.request;

import com.capstone.inventoryservice.model.enums.TicketTypeStatus;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTicketTypeRequest {

    @NotBlank(message = "Type name is required")
    private String typeName;

    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal price;

    @NotNull(message = "Quantity total is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantityTotal;

    @Min(value = 1, message = "Min purchase must be at least 1")
    private Integer minPurchase;

    @Min(value = 1, message = "Max purchase must be at least 1")
    private Integer maxPurchase;

    private LocalDateTime saleStartDate;
    private LocalDateTime saleEndDate;

    @NotNull(message = "Ticket type status is required")
    private TicketTypeStatus ticketTypeStatus;
}
