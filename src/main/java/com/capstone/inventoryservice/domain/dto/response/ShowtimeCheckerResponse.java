package com.capstone.inventoryservice.domain.dto.response;

import com.capstone.inventoryservice.model.enums.CheckerAssignmentStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShowtimeCheckerResponse {
    private Long id;
    private Long showtimeId;
    private Long checkerId;
    private CheckerAssignmentStatus status;
    private Long assignedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
