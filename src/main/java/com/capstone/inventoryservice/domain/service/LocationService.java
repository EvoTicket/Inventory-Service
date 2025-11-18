package com.capstone.inventoryservice.domain.service;

import com.capstone.inventoryservice.model.entity.Province;
import com.capstone.inventoryservice.model.entity.Ward;
import com.capstone.inventoryservice.model.repository.ProvinceRepository;
import com.capstone.inventoryservice.model.repository.WardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final ProvinceRepository provinceRepository;
    private final WardRepository wardRepository;

    public List<Province> getAllProvinces() {
        return provinceRepository.findAll();
    }

    public List<Ward> getWardsByProvinceCode(Integer provinceCode) {
        return wardRepository.findByProvinceCode(provinceCode);
    }
}