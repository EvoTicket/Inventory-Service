package com.capstone.inventoryservice.domain.client;

import com.capstone.inventoryservice.domain.dto.BaseResponse;
import com.capstone.inventoryservice.domain.dto.request.OrderItemRequest;
import com.capstone.inventoryservice.domain.service.TicketReserveService;
import com.capstone.inventoryservice.domain.service.TicketTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class InternalController {
    private final TicketTypeService ticketTypeService;
    private final TicketReserveService ticketReserveService;

    @PostMapping("/ticket-types/tickets")
    public ResponseEntity<BaseResponse<ListTicketTypesInternalResponse>> getTicketTypes(
            @RequestBody List<OrderItemRequest> listItems
    ) {
        return ResponseEntity.ok(BaseResponse.ok(ticketTypeService.getTicketTypes(listItems)));
    }

    @PostMapping("/ticket-types/reserve")
    public ResponseEntity<BaseResponse<Boolean>> reserveTickets(
            @RequestBody List<OrderItemRequest> items
    ) {
        log.info("Reserving tickets: {}", items);
        for (OrderItemRequest item : items) {
            ticketReserveService.reserveOrThrow(
                    item.getTicketTypeId(),
                    item.getQuantity()
            );
        }
        return ResponseEntity.ok(BaseResponse.ok(true));
    }

    @PostMapping("/ticket-types/release")
    public ResponseEntity<BaseResponse<Boolean>> releaseTickets(
            @RequestBody List<OrderItemRequest> items) {

        for (OrderItemRequest item : items) {
            ticketReserveService.release(
                    item.getTicketTypeId(),
                    item.getQuantity()
            );
        }
        return ResponseEntity.ok(BaseResponse.ok(true));
    }
}
