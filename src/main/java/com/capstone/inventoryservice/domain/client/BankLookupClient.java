package com.capstone.inventoryservice.domain.client;

import com.capstone.inventoryservice.exception.AppException;
import com.capstone.inventoryservice.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;

@Component
@RequiredArgsConstructor
@Slf4j
public class BankLookupClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${banklookup.api-key}")
    private String apiKey;

    @Value("${banklookup.api-secret}")
    private String apiSecret;

    @PostConstruct
    public void init() {
        log.info("=== [BankLookupClient] Checking Credentials on Startup ===");
        log.info("API Key loaded: {}", (apiKey == null || apiKey.isBlank() || apiKey.startsWith("${")) 
                ? "NOT_FOUND/MISSING" 
                : "PRESENT (length=" + apiKey.length() + ", mask=" + maskString(apiKey) + ")");
        log.info("API Secret loaded: {}", (apiSecret == null || apiSecret.isBlank() || apiSecret.startsWith("${")) 
                ? "NOT_FOUND/MISSING" 
                : "PRESENT (length=" + apiSecret.length() + ", mask=" + maskString(apiSecret) + ")");
        log.info("==========================================================");
    }

    private String maskString(String str) {
        if (str == null || str.length() <= 8) {
            return "***";
        }
        return str.substring(0, 4) + "..." + str.substring(str.length() - 4);
    }

    public String getOwnerName(String bankCode, String accountNumber) {
        try {
            String responseStr = restClient.post()
                    .uri("https://api.banklookup.net")
                    .header("x-api-key", apiKey)
                    .header("x-api-secret", apiSecret)
                    .body(new BankLookupRequest(bankCode, accountNumber))
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::is4xxClientError,
                            (request, clientResponse) -> {
                                int status = clientResponse.getStatusCode().value();
                                switch (status) {
                                    case 422 -> throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy tài khoản ngân hàng");
                                    case 429 -> throw new AppException(ErrorCode.TOO_MANY_REQUESTS, "Quá nhiều yêu cầu, vui lòng thử lại sau");
                                    case 402 -> throw new AppException(ErrorCode.BAD_REQUEST, "Hết credit API bank lookup");
                                    default -> throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "Lỗi client: " + status);
                                }
                            }
                    )
                    .onStatus(
                            HttpStatusCode::is5xxServerError,
                            (request, clientResponse) -> {
                                throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "Bank lookup server error");
                            }
                    )
                    .body(String.class);

            log.info("Bank lookup successful. Raw response: {}", responseStr);

            if (responseStr == null || responseStr.isBlank()) {
                throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "Không nhận được phản hồi từ bank lookup");
            }

            BankLookupResponse response;
            try {
                response = objectMapper.readValue(responseStr, BankLookupResponse.class);
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                log.error("Failed to parse bank lookup response: {}", responseStr, e);
                throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "Lỗi phân tích phản hồi từ bank lookup");
            }

            if (!Boolean.TRUE.equals(response.getSuccess())) {
                throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR,
                        response.getMsg() != null
                                ? response.getMsg()
                                : "Tra cứu thất bại"
                );
            }

            if (response.getData() == null) {
                throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "Không có dữ liệu tài khoản");
            }

            String ownerName = response.getData().getOwnerName();

            if (ownerName == null || ownerName.isBlank()) {
                throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy tên chủ tài khoản");
            }

            return ownerName;

        } catch (RestClientResponseException ex) {
            log.error("Bank lookup response error", ex);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "Bank lookup error: " + ex.getStatusCode().value());
        } catch (Exception ex) {
            log.error("Bank lookup failed", ex);
            throw ex;
        }
    }
}
