package com.capstone.inventoryservice.domain.service;

import com.capstone.inventoryservice.domain.dto.event.TicketCreatedEvent;
import com.capstone.inventoryservice.exception.AppException;
import com.capstone.inventoryservice.exception.ErrorCode;
import com.capstone.inventoryservice.model.entity.TicketType;
import com.capstone.inventoryservice.model.repository.TicketTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class TicketRedisInitializer {
    private final TicketTypeRepository ticketTypeRepository;
    private final StringRedisTemplate stringRedisTemplate;

    @EventListener
    public void onTicketCreated(TicketCreatedEvent e) {

        String availableKey = "ticket:available:" + e.getTicketTypeId();
        String reservedKey = "ticket:reserved:" + e.getTicketTypeId();

        stringRedisTemplate.opsForValue().set(
                availableKey,
                String.valueOf(e.getQuantity())
        );

        stringRedisTemplate.opsForValue().set(
                reservedKey,
                "0"
        );

        log.info(
                "INIT REDIS ticketTypeId={} available={}",
                e.getTicketTypeId(),
                e.getQuantity()
        );
    }

    @Transactional(readOnly = true)
    public void initFromDB(Long ticketTypeId) {

        TicketType ticket = ticketTypeRepository.findById(ticketTypeId)
                .orElseThrow(() -> new AppException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "TicketType not found: " + ticketTypeId
                ));

        int available = ticket.getQuantityTotal() - ticket.getQuantitySold();

        String availableKey = "ticket:available:" + ticketTypeId;
        String reservedKey = "ticket:reserved:" + ticketTypeId;

        boolean hasAvailable = stringRedisTemplate.hasKey(availableKey);
        if (hasAvailable) {
            return;
        }

        stringRedisTemplate.opsForValue().set(
                availableKey,
                String.valueOf(Math.max(available, 0))
        );

        stringRedisTemplate.opsForValue().set(
                reservedKey,
                "0"
        );

        log.info(
                "INIT REDIS FROM DB ticketTypeId={}, available={}",
                ticketTypeId,
                available
        );
    }

}
