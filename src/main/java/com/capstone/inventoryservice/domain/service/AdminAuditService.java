package com.capstone.inventoryservice.domain.service;

import com.capstone.inventoryservice.domain.dto.BasePageResponse;
import com.capstone.inventoryservice.domain.dto.response.AdminAuditResponse;
import com.capstone.inventoryservice.model.entity.AuditLog;
import com.capstone.inventoryservice.model.repository.AuditLogRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminAuditService {

    private final AuditLogRepository auditLogRepository;

    @PostConstruct
    public void seedAuditLogs() {
        if (auditLogRepository.count() == 0) {
            LocalDateTime now = LocalDateTime.now();
            auditLogRepository.save(createLog(now.minusMinutes(10), "admin@evoticket.com", "ADMIN", "Khóa tài khoản", "User: org.linh@gmail.com", "High", "Success", "User Management", "Khóa tài khoản nhà tổ chức do vi phạm chính sách resale vượt mức.", "User", true, null));
            auditLogRepository.save(createLog(now.minusMinutes(45), "admin@evoticket.com", "ADMIN", "Cập nhật phí nền tảng", "System Configuration", "Critical", "Success", "System Config", "Cập nhật phí nền tảng tăng lên 2.5% cho tất cả giao dịch resale.", "System", true, null));
            auditLogRepository.save(createLog(now.minusHours(2), "system.web3-worker", "SYSTEM", "Mint NFT Ticket", "Ticket ID: 19028", "Low", "Success", "Web3 Services", "Giao dịch mint NFT hoàn tất trên blockchain Polygon.", "Ticket", false, null));
            auditLogRepository.save(createLog(now.minusHours(4), "buyer.anh@gmail.com", "BUYER", "Đặt vé sự kiện", "Event ID: 421", "Low", "Success", "Ticket Ordering", "Đặt mua thành công 2 vé VIP sự kiện nhạc hội.", "Order", false, null));
            auditLogRepository.save(createLog(now.minusHours(6), "organizer.music@gmail.com", "ORGANIZER", "Tạo sự kiện", "Event: Rock Festival 2026", "Medium", "Success", "Event Management", "Tạo sự kiện âm nhạc Rock Festival và gửi yêu cầu kiểm duyệt.", "Event", false, null));
            auditLogRepository.save(createLog(now.minusDays(1).minusHours(2), "admin@evoticket.com", "ADMIN", "Thay đổi cổng thanh toán", "System Configuration", "Critical", "Failed", "System Config", "Không kết nối được cổng PayOS để cập nhật tham số checkout.", "System", true, "Connection timeout to PayOS API"));
            auditLogRepository.save(createLog(now.minusDays(1).minusHours(8), "organizer.run@gmail.com", "ORGANIZER", "Hủy sự kiện", "Event ID: 104", "High", "Success", "Event Management", "Hủy sự kiện do điều kiện thời tiết bất khả kháng.", "Event", false, null));
            auditLogRepository.save(createLog(now.minusDays(2), "admin@evoticket.com", "ADMIN", "Cấp quyền kiểm soát viên", "User: checker.nam@gmail.com", "Medium", "Success", "User Management", "Cấp quyền Checker cho nhân viên soát vé tại cổng chính.", "User", true, null));
        }
    }

    public AdminAuditResponse getAuditDashboard(String tab, String search, int page, int size) {
        int pageIndex = Math.max(0, page - 1);
        Pageable pageable = PageRequest.of(pageIndex, size, Sort.by(Sort.Direction.DESC, "timestamp"));

        String searchPattern = (search != null && !search.trim().isEmpty()) ? "%" + search.toLowerCase().trim() + "%" : null;

        Boolean sensitive = null;
        String module = null;

        if ("sensitive".equalsIgnoreCase(tab)) {
            sensitive = true;
        } else if ("config".equalsIgnoreCase(tab)) {
            module = "System Config";
        }

        Page<AuditLog> logsPage = auditLogRepository.searchLogs(searchPattern, sensitive, module, pageable);
        
        List<AdminAuditResponse.AuditLogDto> dtoList = logsPage.getContent().stream()
                .map(this::mapToDto)
                .toList();

        BasePageResponse<AdminAuditResponse.AuditLogDto> pagedResponse = new BasePageResponse<>();
        pagedResponse.setContent(dtoList);
        pagedResponse.setPageNumber(logsPage.getNumber());
        pagedResponse.setPageSize(logsPage.getSize());
        pagedResponse.setTotalElements(logsPage.getTotalElements());
        pagedResponse.setTotalPages(logsPage.getTotalPages());
        pagedResponse.setLast(logsPage.isLast());

        // 2. Compile Stats
        long totalLogs = auditLogRepository.count();
        long sensitiveActions = auditLogRepository.countBySensitive(true);
        long failedActions = auditLogRepository.countByResult("Failed");
        
        // Mock static changes baseline + actual changes in DB
        long systemChanges = 48 + auditLogRepository.count(); 

        List<AdminAuditResponse.StatCardDto> stats = List.of(
                createStatCard("total_logs", String.format("%,d", totalLogs), "logs_30d", "indigo"),
                createStatCard("sensitive_actions", String.format("%,d", sensitiveActions), "require_mfa", "rose"),
                createStatCard("failed_actions", String.format("%,d", failedActions), "failed_rate_low", "rose"),
                createStatCard("system_changes", String.format("%,d", systemChanges), "changes_applied", "amber")
        );

        return AdminAuditResponse.builder()
                .stats(stats)
                .logs(pagedResponse)
                .build();
    }

    private AdminAuditResponse.AuditLogDto mapToDto(AuditLog entity) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM HH:mm");
        return AdminAuditResponse.AuditLogDto.builder()
                .id(entity.getId())
                .timestamp(entity.getTimestamp().format(formatter))
                .actor(entity.getActor())
                .role(entity.getRole())
                .action(entity.getAction())
                .target(entity.getTarget())
                .severity(entity.getSeverity())
                .result(entity.getResult())
                .module(entity.getModule())
                .description(entity.getDescription())
                .targetType(entity.getTargetType())
                .correlationId(entity.getCorrelationId())
                .auditId(entity.getAuditId())
                .note(entity.getNote())
                .sensitive(entity.isSensitive())
                .build();
    }

    private AdminAuditResponse.StatCardDto createStatCard(String label, String value, String sub, String color) {
        return AdminAuditResponse.StatCardDto.builder()
                .label(label)
                .value(value)
                .sub(sub)
                .color(color)
                .build();
    }

    private AuditLog createLog(LocalDateTime dt, String actor, String role, String action, String target, String severity, String result, String module, String desc, String targetType, boolean sensitive, String note) {
        return AuditLog.builder()
                .timestamp(dt)
                .actor(actor)
                .role(role)
                .action(action)
                .target(target)
                .severity(severity)
                .result(result)
                .module(module)
                .description(desc)
                .targetType(targetType)
                .correlationId(java.util.UUID.randomUUID().toString())
                .auditId("AUD-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .sensitive(sensitive)
                .note(note)
                .build();
    }
}
