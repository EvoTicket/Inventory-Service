package com.capstone.inventoryservice.domain.client;

import com.capstone.inventoryservice.exception.AppException;
import com.capstone.inventoryservice.exception.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
@RequiredArgsConstructor
@Slf4j
public class BankLookupClient {

    private final RestClient restClient;

    @Value("${banklookup.api-key}")
    private String apiKey;

    @Value("${banklookup.api-secret}")
    private String apiSecret;

    public String getOwnerName(String bankCode, String accountNumber) {
        try {
            String raw = restClient.post()
                    .uri("https://api.banklookup.net")
                    .header("x-api-key", apiKey)
                    .header("x-api-secret", apiSecret)
                    .body(new BankLookupRequest(bankCode, accountNumber))
                    .retrieve()
                    .body(String.class);

            log.info("Bank response: {}", raw);

            if (raw == null || raw.isBlank()) {
                throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
            }

            ObjectMapper mapper = new ObjectMapper();

            BankLookupResponse response =
                    mapper.readValue(raw, BankLookupResponse.class);

            return response.getData().getOwnerName();
        }
        catch (JsonProcessingException e) {
            log.error("Invalid bank response", e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        catch (Exception e) {
            log.error("Bank API error", e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

//        try {
//            BankLookupResponse response = restClient.post()
//                    .uri("https://api.banklookup.net")
//                    .header("x-api-key", apiKey)
//                    .header("x-api-secret", apiSecret)
//                    .body(new BankLookupRequest(bankCode, accountNumber))
//                    .retrieve()
//
//                    .onStatus(
//                            HttpStatusCode::is4xxClientError,
//                            (request, clientResponse) -> {
//                                int status = clientResponse.getStatusCode().value();
//                                switch (status) {
//                                    case 422 -> throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy tài khoản ngân hàng");
//                                    case 429 -> throw new AppException(ErrorCode.TOO_MANY_REQUESTS, "Quá nhiều yêu cầu, vui lòng thử lại sau");
//                                    case 402 -> throw new AppException(ErrorCode.BAD_REQUEST, "Hết credit API bank lookup");
//                                    default -> throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "Lỗi client: " + status);
//                                }
//                            }
//                    )
//
//                    .onStatus(
//                            HttpStatusCode::is5xxServerError,
//                            (request, clientResponse) -> {
//                                throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "Bank lookup server error");
//                            }
//                    )
//
//                    .body(BankLookupResponse.class);
//
//            if (response == null) {
//                throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "Không nhận được phản hồi từ bank lookup");
//            }
//
//            if (!Boolean.TRUE.equals(response.getSuccess())) {
//                throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR,
//                        response.getMsg() != null
//                                ? response.getMsg()
//                                : "Tra cứu thất bại"
//                );
//            }
//
//            if (response.getData() == null) {
//                throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "Không có dữ liệu tài khoản");
//            }
//
//            String ownerName = response.getData().getOwnerName();
//
//            if (ownerName == null || ownerName.isBlank()) {
//                throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy tên chủ tài khoản");
//            }
//
//            return ownerName;
//
//        } catch (RestClientResponseException ex) {
//            log.error("Bank lookup response error", ex);
//            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "Bank lookup error: " + ex.getStatusCode().value());
//        } catch (Exception ex) {
//            log.error("Bank lookup failed", ex);
//            throw ex;
//        }
    }
}
