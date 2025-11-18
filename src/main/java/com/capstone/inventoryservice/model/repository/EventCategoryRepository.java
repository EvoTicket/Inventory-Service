package com.capstone.inventoryservice.model.repository;

import com.capstone.inventoryservice.model.entity.EventCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EventCategoryRepository extends JpaRepository<EventCategory, Long> {

    Optional<EventCategory> findByCategoryName(String categoryName);

    boolean existsByCategoryName(String categoryName);
}