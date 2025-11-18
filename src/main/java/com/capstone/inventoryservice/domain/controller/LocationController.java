package com.capstone.inventoryservice.domain.controller;

import com.capstone.inventoryservice.model.entity.Province;
import com.capstone.inventoryservice.model.entity.Ward;
import com.capstone.inventoryservice.domain.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @GetMapping("/provinces")
    public List<Province> getAllProvinces() {
        return locationService.getAllProvinces();
    }

    @GetMapping("/wards")
    public List<Ward> getWardsByProvinceCode(@RequestParam("provinceCode") Integer provinceCode) {
        return locationService.getWardsByProvinceCode(provinceCode);
    }
}
