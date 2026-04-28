package com.capstone.inventoryservice.domain.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ReserveRequest {
    @NotEmpty(message = "Danh sách items không được rỗng")
    @Valid
    private List<ReserveItem> items;

    @Data
    public static class ReserveItem {
        @NotNull(message = "Ticket Type ID không được để trống")
        private Long ticketTypeId;

        @NotNull(message = "Số lượng không được để trống")
        @Min(value = 1, message = "Số lượng phải lớn hơn 0")
        @JsonProperty("qty")
        private Integer qty;
    }
}
