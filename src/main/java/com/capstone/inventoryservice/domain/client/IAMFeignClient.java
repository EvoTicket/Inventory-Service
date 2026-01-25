package com.capstone.inventoryservice.domain.client;

import com.capstone.inventoryservice.config.FeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "iam-service",
        path = "/api/external",
        configuration = FeignClientConfig.class
)
public interface IAMFeignClient {

    @GetMapping("/organizations/{id}")
    OrgInternalResponse getOrganizationById(@PathVariable("id") Long id);

    @GetMapping("/users/{id}")
    UserInternalResponse getUserById(@PathVariable("id") Long id);
}
