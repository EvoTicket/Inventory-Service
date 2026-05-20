package com.capstone.inventoryservice.domain.scheduler;

import com.capstone.inventoryservice.model.enums.EventApprovalStatus;
import com.capstone.inventoryservice.model.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventCleanupScheduler {

    private final EventRepository eventRepository;

    // Run every day at midnight
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void cleanupDraftEvents() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(30);
        log.info("Starting cleanup of draft events created before: {}", threshold);
        try {
            eventRepository.deleteByApprovalStatusAndCreatedAtBefore(EventApprovalStatus.DRAFT, threshold);
            log.info("Completed cleanup of draft events");
        } catch (Exception e) {
            log.error("Failed to clean up draft events: {}", e.getMessage(), e);
        }
    }
}
