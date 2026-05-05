package com.capstone.inventoryservice.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum  EventApprovalStatus {
    PENDING(false),
    ACCEPTED(true),
    REJECTED(false);

    private final boolean accepted;
}
