package com.capstone.inventoryservice.domain.service;

import com.capstone.inventoryservice.domain.dto.request.ReserveRequest;
import com.capstone.inventoryservice.domain.dto.response.ReserveResponse;
import com.capstone.inventoryservice.exception.AppException;
import com.capstone.inventoryservice.exception.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final TicketReserveService ticketReserveService;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    private static final long SESSION_TTL_MINUTES = 15;

    public ReserveResponse reserveTickets(ReserveRequest request) {
        List<ReserveRequest.ReserveItem> reservedItems = new ArrayList<>();

        try {
            for (ReserveRequest.ReserveItem item : request.getItems()) {
                ticketReserveService.reserveOrThrow(item.getTicketTypeId(), item.getQty());
                reservedItems.add(item);
            }
        } catch (Exception e) {
            // Rollback already reserved tickets
            for (ReserveRequest.ReserveItem item : reservedItems) {
                ticketReserveService.release(item.getTicketTypeId(), item.getQty());
            }
            throw e;
        }

        String sessionId = UUID.randomUUID().toString();
        String sessionKey = "booking:session:" + sessionId;
        String dataKey = "booking:data:" + sessionId;

        try {
            String dataJson = objectMapper.writeValueAsString(request);
            // Save data with a slightly longer TTL (e.g. 20 mins) so it's guaranteed to be there when session expires
            stringRedisTemplate.opsForValue().set(dataKey, dataJson, SESSION_TTL_MINUTES + 5, TimeUnit.MINUTES);
            
            // Set the session key that will trigger expiration
            stringRedisTemplate.opsForValue().set(sessionKey, "active", SESSION_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (JsonProcessingException e) {
            // Rollback if serialization fails
            for (ReserveRequest.ReserveItem item : reservedItems) {
                ticketReserveService.release(item.getTicketTypeId(), item.getQty());
            }
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "Failed to serialize booking session data", e);
        }

        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(SESSION_TTL_MINUTES);
        
        return ReserveResponse.builder()
                .bookingSessionId(sessionId)
                .expiresAt(expiresAt)
                .remainingSeconds(SESSION_TTL_MINUTES * 60)
                .build();
    }
}
