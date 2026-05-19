package com.capstone.inventoryservice.domain.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankInfoInternalResponse {
    private Long id;
    private String profileName;
    private String bankCode;
    private String bankName;
    private String bankAccountNumber;
    private String bankOwnerName;
}
