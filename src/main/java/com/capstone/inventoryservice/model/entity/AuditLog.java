package com.capstone.inventoryservice.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime timestamp;
    private String actor;
    private String role;
    private String action;
    private String target;
    private String severity; // "Critical" | "High" | "Medium" | "Low"
    private String result; // "Success" | "Partial" | "Failed"
    private String module;
    
    @Column(length = 1000)
    private String description;
    
    private String targetType;
    private String correlationId;
    private String auditId;
    
    @Column(length = 1000)
    private String note;
    
    private boolean sensitive;
}
