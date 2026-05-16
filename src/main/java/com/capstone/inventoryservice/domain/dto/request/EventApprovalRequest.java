package com.capstone.inventoryservice.domain.dto.request;

import com.capstone.inventoryservice.model.enums.EventApprovalStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EventApprovalRequest {

    @NotNull(message = "Approval status is required")
    private EventApprovalStatus approvalStatus;

    private String reason;
}
