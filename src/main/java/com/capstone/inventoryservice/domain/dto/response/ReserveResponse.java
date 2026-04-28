package com.capstone.inventoryservice.domain.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReserveResponse {
    private String bookingSessionId;
    private LocalDateTime expiresAt;
    private long remainingSeconds;
}
