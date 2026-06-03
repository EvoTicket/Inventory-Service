package com.capstone.inventoryservice.domain.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import com.capstone.inventoryservice.domain.dto.BasePageResponse;

@Data
@Builder
public class AdminAuditResponse {
    private List<StatCardDto> stats;
    private BasePageResponse<AuditLogDto> logs;

    @Data
    @Builder
    public static class StatCardDto {
        private String label;
        private String value;
        private String sub;
        private String color;
    }

    @Data
    @Builder
    public static class AuditLogDto {
        private Long id;
        private String timestamp;
        private String actor;
        private String role;
        private String action;
        private String target;
        private String severity;
        private String result;
        private String module;
        private String description;
        private String targetType;
        private String correlationId;
        private String auditId;
        private String note;
        private boolean sensitive;
    }
}
