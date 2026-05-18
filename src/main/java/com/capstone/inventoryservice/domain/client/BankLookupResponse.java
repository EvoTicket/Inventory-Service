package com.capstone.inventoryservice.domain.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class BankLookupResponse {

    private Integer code;
    private Boolean success;
    private DataResponse data;
    private String msg;

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    public static class DataResponse {
        private String ownerName;
    }
}