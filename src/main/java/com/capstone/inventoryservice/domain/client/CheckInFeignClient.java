package com.capstone.inventoryservice.domain.client;

import com.capstone.inventoryservice.config.FeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(
        name = "checkin-service",
        path = "/api/internal",
        configuration = FeignClientConfig.class
)
public interface CheckInFeignClient {

    @GetMapping("/checkin/organizer-stats")
    OrganizerCheckInStatsInternalResponse getOrganizerStats(
            @RequestParam("eventIds") List<Long> eventIds
    );
}
