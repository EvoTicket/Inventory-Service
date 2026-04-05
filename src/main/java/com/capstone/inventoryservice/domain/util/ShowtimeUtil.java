package com.capstone.inventoryservice.domain.util;

import com.capstone.inventoryservice.model.entity.Showtime;
import com.capstone.inventoryservice.exception.AppException;
import com.capstone.inventoryservice.exception.ErrorCode;
import com.capstone.inventoryservice.model.repository.ShowtimeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ShowtimeUtil {
    private final ShowtimeRepository showtimeRepository;

    @Transactional(readOnly = true)
    public Showtime getShowtimeOrElseThrow(Long showtimeId) {
        return showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Showtime not found with id: " + showtimeId));
    }
}
