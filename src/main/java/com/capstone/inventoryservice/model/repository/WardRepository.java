package com.capstone.inventoryservice.model.repository;

import com.capstone.inventoryservice.model.entity.Ward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WardRepository extends JpaRepository<Ward, Integer> {
    Optional<Ward> findByCode(Integer code);

    List<Ward> findByProvinceCode(Integer provinceCode);
}