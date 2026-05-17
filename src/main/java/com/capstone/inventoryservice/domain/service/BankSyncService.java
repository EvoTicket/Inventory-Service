package com.capstone.inventoryservice.domain.service;

import com.capstone.inventoryservice.model.entity.Bank;
import com.capstone.inventoryservice.model.repository.BankRepository;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BankSyncService {

    private final BankRepository bankRepository;
    private final RestClient restClient = RestClient.create();

    @Transactional
    public List<Bank> syncBanks() {
        try {
            String url = "https://api.vietqr.io/v2/banks";
            VietQrResponse response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(VietQrResponse.class);
            
            if (response != null && "00".equals(response.getCode()) && response.getData() != null) {
                List<Bank> banks = response.getData().stream().map(dto -> 
                    Bank.builder()
                        .id(dto.getId())
                        .name(dto.getName())
                        .code(dto.getCode())
                        .bin(dto.getBin())
                        .shortName(dto.getShortName())
                        .logo(dto.getLogo())
                        .transferSupported(dto.getTransferSupported())
                        .lookupSupported(dto.getLookupSupported())
                        .support(dto.getSupport())
                        .isTransfer(dto.getIsTransfer())
                        .swiftCode(dto.getSwift_code())
                        .build()
                ).collect(Collectors.toList());
                
                bankRepository.saveAll(banks);
                log.info("Successfully synced {} banks from VietQR.", banks.size());
                return banks;
            } else {
                log.warn("Failed to fetch banks from VietQR. Response code: {}", response != null ? response.getCode() : "null");
                throw new RuntimeException("Failed to fetch banks from VietQR API");
            }
        } catch (Exception e) {
            log.error("Error syncing banks from VietQR", e);
            throw new RuntimeException("Error syncing banks from VietQR", e);
        }
    }

    public List<Bank> getAllBanks() {
        return bankRepository.findAll();
    }

    @Data
    public static class VietQrResponse {
        private String code;
        private String desc;
        private List<BankDto> data;
    }

    @Data
    public static class BankDto {
        private Integer id;
        private String name;
        private String code;
        private String bin;
        private String shortName;
        private String logo;
        private Integer transferSupported;
        private Integer lookupSupported;
        @JsonProperty("short_name")
        private String short_name;
        private Integer support;
        private Integer isTransfer;
        private String swift_code;
    }
}
