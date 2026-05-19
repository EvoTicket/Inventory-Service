package com.capstone.inventoryservice.domain.mapper;

import com.capstone.inventoryservice.domain.dto.response.ShowtimeCheckerResponse;
import com.capstone.inventoryservice.model.entity.ShowtimeChecker;
import org.springframework.stereotype.Component;

@Component
public class ShowtimeCheckerMapper {

    public ShowtimeCheckerResponse convertToDTO(ShowtimeChecker showtimeChecker) {
        if (showtimeChecker == null) {
            return null;
        }
        return ShowtimeCheckerResponse.builder()
                .id(showtimeChecker.getId())
                .showtimeId(showtimeChecker.getShowtime() != null ? showtimeChecker.getShowtime().getId() : null)
                .checkerId(showtimeChecker.getCheckerId())
                .status(showtimeChecker.getStatus())
                .assignedBy(showtimeChecker.getAssignedBy())
                .createdAt(showtimeChecker.getCreatedAt())
                .updatedAt(showtimeChecker.getUpdatedAt())
                .build();
    }
}
