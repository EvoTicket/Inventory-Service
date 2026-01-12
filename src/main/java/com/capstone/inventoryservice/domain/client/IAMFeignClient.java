package com.capstone.inventoryservice.domain.client;

import com.capstone.inventoryservice.config.FeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "iam-service",
        configuration = FeignClientConfig.class
)
public interface IAMFeignClient {

    @GetMapping("/client/organizations/{id}")
    OrgClientResponse getOrganizationById(@PathVariable("id") Long id);

    @GetMapping("/client/users/{id}")
    UserClientResponse getUserById(@PathVariable("id") Long id);
}
