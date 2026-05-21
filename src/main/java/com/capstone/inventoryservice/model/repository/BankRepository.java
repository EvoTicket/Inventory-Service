package com.capstone.inventoryservice.model.repository;

import com.capstone.inventoryservice.model.entity.Bank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BankRepository extends JpaRepository<Bank, Integer> {
    Optional<Bank> findByCode(String bankCode);
}
