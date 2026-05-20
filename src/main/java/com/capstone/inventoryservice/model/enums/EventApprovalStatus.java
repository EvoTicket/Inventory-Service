package com.capstone.inventoryservice.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EventApprovalStatus {
    DRAFT(false),
    PENDING_REVIEW(false),
    PUBLISHED(true),
    REJECTED(false),
    CANCELLED(false);

    private final boolean accepted;
}
