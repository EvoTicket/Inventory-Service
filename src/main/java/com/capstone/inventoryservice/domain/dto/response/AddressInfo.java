package com.capstone.inventoryservice.domain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressInfo {
    private Integer wardCode;
    private String wardName;
    private Integer provinceCode;
    private String provinceName;
    private String fullAddress;
}