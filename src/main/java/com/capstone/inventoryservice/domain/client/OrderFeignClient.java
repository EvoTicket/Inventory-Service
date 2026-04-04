package com.capstone.inventoryservice.domain.client;

import com.capstone.inventoryservice.config.FeignClientConfig;
import com.capstone.inventoryservice.domain.dto.response.EventVolumeResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@FeignClient(
        name = "order-service",
        path = "/api/internal",
        configuration = FeignClientConfig.class
)
public interface OrderFeignClient {

    @PostMapping("/orders/volume")
    Map<Long, EventVolumeResponse> getVolumeForEvents(@RequestBody List<Long> eventIds);

    @GetMapping("/orders/user/{userId}/purchased-events")
    List<Long> getPurchasedEventIdsByUserId(@PathVariable Long userId);
}
