package com.capstone.inventoryservice.domain.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateDraftStep4Request {

    @NotNull(message = "Thông tin tài khoản ngân hàng không được để trống")
    private Long bankInfoId;

    private String reconciliationNote;
}
