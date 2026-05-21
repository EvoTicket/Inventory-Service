package com.capstone.inventoryservice.domain.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardExportRequest {
    private String format; // "CSV", "XLSX", "PDF"
    private Integer days;  // e.g. 30, 60
    private List<String> sections; // "summary", "revenue", "tickets", "checkin", "resale"
    private String separator; // e.g. "," or ";" for CSV
    @Builder.Default
    private Boolean includeHeaders = true;
}
