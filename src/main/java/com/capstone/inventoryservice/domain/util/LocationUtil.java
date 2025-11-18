package com.capstone.inventoryservice.domain.util;

import com.capstone.inventoryservice.domain.dto.response.AddressInfo;
import com.capstone.inventoryservice.model.entity.Province;
import com.capstone.inventoryservice.model.entity.Ward;
import com.capstone.inventoryservice.exception.AppException;
import com.capstone.inventoryservice.exception.ErrorCode;
import com.capstone.inventoryservice.model.repository.ProvinceRepository;
import com.capstone.inventoryservice.model.repository.WardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LocationUtil {
    private final ProvinceRepository provinceRepository;
    private final WardRepository wardRepository;

    public Province getProvinceByCode(Integer provinceCode) {
        return provinceRepository.findByCode(provinceCode)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "không tìm thấy tỉnh"));
    }

    public Ward getWardByCode(Integer wardCode) {
        return wardRepository.findByCode(wardCode)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "không tìm thấy phường/xã"));
    }

    public AddressInfo getAddressInfo(Province province, Ward ward, String fullAddress) {
        return AddressInfo.builder()
                .wardCode(ward != null ? ward.getCode() : null)
                .wardName(ward != null ? ward.getName() : null)
                .provinceCode(province != null ? province.getCode() : null)
                .provinceName(province != null ? province.getName() : null)
                .fullAddress(fullAddress)
                .build();
    }
}
