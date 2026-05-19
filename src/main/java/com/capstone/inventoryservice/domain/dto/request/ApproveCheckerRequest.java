package com.capstone.inventoryservice.domain.dto.request;

import com.capstone.inventoryservice.model.enums.CheckerAssignmentStatus;
import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApproveCheckerRequest {
    private CheckerAssignmentStatus status; // APPROVED or REJECTED
}
