package com.capstone.inventoryservice.domain.service;

import com.capstone.inventoryservice.domain.dto.request.ReserveRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SessionExpirationListener extends KeyExpirationEventMessageListener {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final TicketReserveService ticketReserveService;

    public SessionExpirationListener(RedisMessageListenerContainer listenerContainer,
                                     StringRedisTemplate stringRedisTemplate,
                                     ObjectMapper objectMapper,
                                     TicketReserveService ticketReserveService) {
        super(listenerContainer);
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.ticketReserveService = ticketReserveService;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = message.toString();

        if (expiredKey.startsWith("booking:session:")) {
            String sessionId = expiredKey.replace("booking:session:", "");
            log.info("Booking session expired: {}", sessionId);

            String dataKey = "booking:data:" + sessionId;
            String dataJson = stringRedisTemplate.opsForValue().get(dataKey);

            if (dataJson != null) {
                try {
                    ReserveRequest request = objectMapper.readValue(dataJson, ReserveRequest.class);
                    // Release reserved tickets
                    for (ReserveRequest.ReserveItem item : request.getItems()) {
                        log.info("Releasing {} tickets for ticketType {}", item.getQty(), item.getTicketTypeId());
                        ticketReserveService.release(item.getTicketTypeId(), item.getQty());
                    }
                } catch (JsonProcessingException e) {
                    log.error("Failed to parse booking data for session {}", sessionId, e);
                } finally {
                    // Clean up the data key
                    stringRedisTemplate.delete(dataKey);
                }
            } else {
                log.warn("No booking data found for session {}", sessionId);
            }
        }
    }
}
