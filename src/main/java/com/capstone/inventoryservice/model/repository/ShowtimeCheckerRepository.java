package com.capstone.inventoryservice.model.repository;

import com.capstone.inventoryservice.model.entity.ShowtimeChecker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShowtimeCheckerRepository extends JpaRepository<ShowtimeChecker, Long> {
    Optional<ShowtimeChecker> findByShowtimeIdAndCheckerId(Long showtimeId, Long checkerId);
    List<ShowtimeChecker> findByShowtimeId(Long showtimeId);
    List<ShowtimeChecker> findByCheckerId(Long checkerId);
}
