package com.capstone.inventoryservice.model.repository;

import com.capstone.inventoryservice.model.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("""
        SELECT a FROM AuditLog a
        WHERE (:search IS NULL OR
               LOWER(a.actor) LIKE :search OR
               LOWER(a.action) LIKE :search OR
               LOWER(a.target) LIKE :search OR
               LOWER(a.description) LIKE :search)
          AND (:sensitive IS NULL OR a.sensitive = :sensitive)
          AND (:module IS NULL OR a.module = :module)
    """)
    Page<AuditLog> searchLogs(
            @Param("search") String search,
            @Param("sensitive") Boolean sensitive,
            @Param("module") String module,
            Pageable pageable
    );

    long countBySensitive(boolean sensitive);
    long countByResult(String result);
}
