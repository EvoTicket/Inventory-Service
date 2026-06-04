package com.capstone.inventoryservice.model.repository;

import com.capstone.inventoryservice.model.entity.KbItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KbItemRepository extends JpaRepository<KbItem, Long> {

    Optional<KbItem> findBySource(String source);

    boolean existsBySource(String source);

    List<KbItem> findAllByOrderByUpdatedAtDesc();

    List<KbItem> findByCategory(String category);

    List<KbItem> findByStatus(String status);
}
