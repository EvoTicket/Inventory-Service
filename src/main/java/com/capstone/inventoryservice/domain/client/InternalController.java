package com.capstone.inventoryservice.domain.client;

import com.capstone.inventoryservice.domain.dto.BaseResponse;
import com.capstone.inventoryservice.domain.dto.request.OrderItemRequest;
import com.capstone.inventoryservice.domain.service.TicketTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class InternalController {
    private final TicketTypeService ticketTypeService;

    @PostMapping("/ticket-types/tickets")
    public ResponseEntity<BaseResponse<ListTicketTypesInternalResponse>> getTicketTypes(
            @RequestBody List<OrderItemRequest> listItems
    ) {
        return ResponseEntity.ok(BaseResponse.ok(ticketTypeService.getTicketTypes(listItems)));
    }
}
