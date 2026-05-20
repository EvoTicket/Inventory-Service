package com.capstone.inventoryservice.domain.dto.request;

import com.capstone.inventoryservice.model.enums.EventType;
import com.capstone.inventoryservice.model.enums.EventCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateDraftStep1Request {

    @NotBlank(message = "Tên sự kiện không được để trống")
    @Size(max = 255, message = "Tên sự kiện không được vượt quá 255 ký tự")
    private String eventName;

    private String introduction;

    private EventType eventType;

    private EventCategory category;

    private String venue;

    private Integer provinceCode;

    private Integer wardCode;

    private String address;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private String shortDescription;

    private String description;
}
