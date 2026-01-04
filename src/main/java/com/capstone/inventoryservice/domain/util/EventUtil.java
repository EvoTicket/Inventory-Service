package com.capstone.inventoryservice.domain.util;

import com.capstone.inventoryservice.model.entity.Event;
import com.capstone.inventoryservice.exception.AppException;
import com.capstone.inventoryservice.exception.ErrorCode;
import com.capstone.inventoryservice.model.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class EventUtil {
    private final EventRepository eventRepository;

    @Transactional(readOnly = true)
    public Event getEventOrElseThrow(Long eventId) {
        return eventRepository.findByIdWithDetails(eventId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Event not found with id: " + eventId));
    }
}
