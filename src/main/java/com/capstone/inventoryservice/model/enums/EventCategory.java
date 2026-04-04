package com.capstone.inventoryservice.model.enums;

public enum EventCategory {
    LIVESTAGE("Livestage"),
    STAGE_ART("Sân khấu & Nghệ thuật"),
    WORKSHOP("Hội thảo & Workshop"),
    SPORTS("Thể thao"),
    EXHIBITION("Triển lãm / Trải nghiệm");

    private final String displayName;

    EventCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
