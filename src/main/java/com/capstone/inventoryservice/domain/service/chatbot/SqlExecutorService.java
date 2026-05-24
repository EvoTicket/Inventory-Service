package com.capstone.inventoryservice.domain.service.chatbot;

import com.capstone.inventoryservice.exception.AppException;
import com.capstone.inventoryservice.exception.ErrorCode;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SqlExecutorService {

    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<?> execute(String sqlQuery) {
        try {
            log.info("[SqlExecutorService] Dang thuc thi cau lenh SQL: {}", sqlQuery);
            List<?> results = entityManager.createNativeQuery(sqlQuery).getResultList();
            log.info("[SqlExecutorService] Thuc thi SQL thanh cong, tra ve {} ket qua.", results.size());
            return results;
        } catch (Exception e) {
            log.error("[SqlExecutorService] Loi khi thuc thi SQL: ", e);
            throw new AppException(ErrorCode.BAD_REQUEST, "Loi thuc thi database: " + e.getMessage());
        }
    }
}
