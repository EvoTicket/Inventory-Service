package com.capstone.inventoryservice.domain.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BookingSessionResponse {
    private BookingSessionData sessionData;
    private LocalDateTime expiresAt;
    private long remainingSeconds;
}
