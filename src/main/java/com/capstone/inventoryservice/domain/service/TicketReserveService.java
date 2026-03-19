package com.capstone.inventoryservice.domain.service;

import com.capstone.inventoryservice.domain.dto.event.OrderPaidEvent;
import com.capstone.inventoryservice.domain.dto.request.OrderItemRequest;
import com.capstone.inventoryservice.exception.AppException;
import com.capstone.inventoryservice.exception.ErrorCode;
import com.capstone.inventoryservice.model.repository.TicketTypeRepository;
import com.capstone.inventoryservice.producer.RedisStreamProducer;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketReserveService {
    private final TicketTypeRepository ticketTypeRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final DefaultRedisScript<Long> reserveScript;
    private final TicketRedisInitializer ticketRedisInitializer;
    private final RedisStreamProducer redisStreamProducer;

    public void reserveOrThrow(Long ticketTypeId, long qty) {
        String availableKey = "ticket:available:" + ticketTypeId;
        if (!stringRedisTemplate.hasKey(availableKey)) {
            ticketRedisInitializer.initFromDB(ticketTypeId);
        }

        String reservedKey  = "ticket:reserved:" + ticketTypeId;
        log.info("Reserving tickets: {}", availableKey);
        Long result = stringRedisTemplate.execute(
                reserveScript,
                List.of(availableKey, reservedKey),
                String.valueOf(qty)
        );

        log.info("Reserve ticket result: {}", result);
        if (result != 1) {
            throw new AppException(
                    ErrorCode.BAD_REQUEST,
                    "Ticket đã hết: " + ticketTypeId
            );
        }
    }

    public void release(Long ticketTypeId, long qty) {

        stringRedisTemplate.opsForValue()
                .increment("ticket:available:" + ticketTypeId, qty);

        stringRedisTemplate.opsForValue()
                .decrement("ticket:reserved:" + ticketTypeId, qty);
    }

    @Transactional
    public void  commitTickets(OrderPaidEvent event) {
        try {
            for (OrderItemRequest item : event.getItems()) {

                int updated = ticketTypeRepository.increaseSold(
                        item.getTicketTypeId(),
                        item.getQuantity()
                );

                if (updated == 0) {
                    throw new AppException(
                            ErrorCode.CONFLICT,
                            "Commit thất bại ticket " + item.getTicketTypeId()
                    );
                }

                stringRedisTemplate.opsForValue().decrement(
                        "ticket:reserved:" + item.getTicketTypeId(),
                        item.getQuantity()
                );
            }
            redisStreamProducer.sendMessage("commit-ticket-success",  event.getOrderCode());
        } catch (Exception e) {
            log.error("Commit tickets failed for order {}", e.getMessage());
            redisStreamProducer.sendMessage("commit-ticket-failed", event.getOrderCode());
            throw e;
        }
    }
}
