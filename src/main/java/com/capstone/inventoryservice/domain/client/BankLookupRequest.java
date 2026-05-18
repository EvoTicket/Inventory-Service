package com.capstone.inventoryservice.domain.client;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankLookupRequest {
    private String bank;
    private String account;
}
