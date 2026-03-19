package com.capstone.inventoryservice.domain.dto.event;

import com.capstone.inventoryservice.domain.dto.request.OrderItemRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderPaidEvent {
    private String orderCode;
    private List<OrderItemRequest> items;
}
