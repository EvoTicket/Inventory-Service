package com.capstone.inventoryservice.domain.client;

import com.capstone.inventoryservice.domain.dto.response.AddressInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrgClientResponse {
    private Long id;
    private String organizationName;
    private String logoUrl;
    private AddressInfo addressInfo;
    private String businessPhone;
    private String businessEmail;
}
