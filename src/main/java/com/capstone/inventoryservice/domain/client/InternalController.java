package com.capstone.inventoryservice.domain.client;

import com.capstone.inventoryservice.domain.dto.BaseResponse;
import com.capstone.inventoryservice.domain.dto.request.OrderItemRequest;
import com.capstone.inventoryservice.domain.service.TicketReserveService;
import com.capstone.inventoryservice.domain.service.TicketTypeService;
import com.capstone.inventoryservice.exception.AppException;
import com.capstone.inventoryservice.exception.ErrorCode;
import com.capstone.inventoryservice.model.entity.Event;
import com.capstone.inventoryservice.model.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class InternalController {
    private final TicketTypeService ticketTypeService;
    private final TicketReserveService ticketReserveService;
    private final EventRepository eventRepository;
    private final IAMFeignClient iamFeignClient;

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

    @GetMapping("/event/{eventId}")
    public ResponseEntity<BaseResponse<EventDetailResponse>> getEventDetails(@PathVariable Long eventId){
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "event not found"));
        String organizerName = iamFeignClient.getOrganizationById(event.getOrganizerId()).getOrganizationName();
        EventDetailResponse eventDetailResponse = EventDetailResponse.toDto(event, organizerName);
        return ResponseEntity.ok(BaseResponse.ok(eventDetailResponse));
    }
}
