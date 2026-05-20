package com.capstone.inventoryservice.domain.dto.request;

import lombok.*;

import java.math.BigDecimal;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateDraftStep3Request {

    private Boolean allowMultipleTicketTypesPerOrder;

    private Boolean allowDiscountCode;

    private Boolean allowResale;

    private BigDecimal maxResalePricePercentage;

    private BigDecimal organizerRoyaltyFeePercentage;

    private String postPurchaseInstruction;

    private String checkInInstruction;

    private String entryGateInstruction;
}
