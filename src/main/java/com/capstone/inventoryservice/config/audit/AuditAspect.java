package com.capstone.inventoryservice.config.audit;

import com.capstone.inventoryservice.model.entity.AuditLog;
import com.capstone.inventoryservice.model.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditLogRepository auditLogRepository;

    @Around("@annotation(auditAction)")
    public Object auditMethod(ProceedingJoinPoint joinPoint, AuditAction auditAction) throws Throwable {
        LocalDateTime timestamp = LocalDateTime.now();
        
        // 1. Get current actor & role
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = (auth != null && auth.isAuthenticated()) ? auth.getName() : "System";
        String role = "SYSTEM";
        if (auth != null && auth.getAuthorities() != null && !auth.getAuthorities().isEmpty()) {
            role = auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
        }

        // 2. Resolve Target Name from arguments
        String target = "N/A";
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg instanceof Long) {
                target = auditAction.targetType() + " ID: " + arg;
                break;
            } else if (arg instanceof List && !((List<?>) arg).isEmpty()) {
                target = auditAction.targetType() + " List: " + arg;
                break;
            } else if (arg != null) {
                try {
                    var method = arg.getClass().getMethod("getEventName");
                    Object name = method.invoke(arg);
                    if (name != null) {
                        target = name.toString();
                        break;
                    }
                } catch (Exception ignored) {}
            }
        }

        String correlationId = UUID.randomUUID().toString();
        String auditId = "AUD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Object resultObj;
        try {
            resultObj = joinPoint.proceed();
            
            // 3. Log Success
            AuditLog logEntity = AuditLog.builder()
                    .timestamp(timestamp)
                    .actor(actor)
                    .role(role)
                    .action(auditAction.action())
                    .target(target)
                    .severity(auditAction.severity())
                    .result("Success")
                    .module(auditAction.module())
                    .description("Thực hiện thành công hành động " + auditAction.action() + " trên đối tượng " + target)
                    .targetType(auditAction.targetType())
                    .correlationId(correlationId)
                    .auditId(auditId)
                    .sensitive(auditAction.sensitive())
                    .build();
            
            auditLogRepository.save(logEntity);
            return resultObj;
        } catch (Throwable throwable) {
            // 4. Log Failure
            AuditLog logEntity = AuditLog.builder()
                    .timestamp(timestamp)
                    .actor(actor)
                    .role(role)
                    .action(auditAction.action())
                    .target(target)
                    .severity(auditAction.severity())
                    .result("Failed")
                    .module(auditAction.module())
                    .description("Thất bại hành động " + auditAction.action() + " trên đối tượng " + target)
                    .targetType(auditAction.targetType())
                    .correlationId(correlationId)
                    .auditId(auditId)
                    .note("Lỗi: " + throwable.getMessage())
                    .sensitive(auditAction.sensitive())
                    .build();
            
            auditLogRepository.save(logEntity);
            throw throwable;
        }
    }
}
