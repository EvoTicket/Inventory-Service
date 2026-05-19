package com.capstone.inventoryservice.domain.client;

import com.capstone.inventoryservice.config.FeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "iam-service",
        path = "/api/internal",
        configuration = FeignClientConfig.class
)
public interface IAMFeignClient {

    @GetMapping("/organizations/{id}")
    OrgInternalResponse getOrganizationById(@PathVariable Long id);

    @GetMapping("/users/{id}")
    UserInternalResponse getUserById(@PathVariable Long id);

    @GetMapping("/users/count-since")
    long getNewUsersCount(@RequestParam("since") String since);

    @GetMapping("/bank-infos/{id}")
    BankInfoInternalResponse getBankInfoById(@PathVariable("id") Long id);
}
