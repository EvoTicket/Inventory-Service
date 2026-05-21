package com.capstone.inventoryservice.domain.client;

import com.capstone.inventoryservice.domain.dto.BaseResponse;
import com.capstone.inventoryservice.domain.dto.request.OrderItemRequest;
import com.capstone.inventoryservice.domain.service.ShowtimeCheckerService;
import com.capstone.inventoryservice.domain.service.TicketReserveService;
import com.capstone.inventoryservice.domain.service.TicketTypeService;
import com.capstone.inventoryservice.domain.util.EventUtil;
import com.capstone.inventoryservice.exception.AppException;
import com.capstone.inventoryservice.exception.ErrorCode;
import com.capstone.inventoryservice.model.entity.Bank;
import com.capstone.inventoryservice.model.entity.Event;
import com.capstone.inventoryservice.model.entity.Showtime;
import com.capstone.inventoryservice.model.entity.TicketType;
import com.capstone.inventoryservice.model.repository.BankRepository;
import com.capstone.inventoryservice.model.repository.TicketTypeRepository;
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
    private final ShowtimeCheckerService showtimeCheckerService;
    private final EventUtil eventUtil;
    private final TicketTypeRepository ticketTypeRepository;
    private final IAMFeignClient iamFeignClient;
    private final BankRepository bankRepository;

    @PostMapping("/ticket-types/tickets")
    public ResponseEntity<BaseResponse<ListTicketTypesInternalResponse>> getTicketTypes(
            @RequestBody List<OrderItemRequest> listItems
    ) {
        return ResponseEntity.ok(BaseResponse.ok(ticketTypeService.getTicketTypes(listItems)));
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

    @GetMapping("/event/{ticketTypeId}")
    public ResponseEntity<BaseResponse<EventDetailInternalResponse>> getEventDetailsByTicketTypeId(
            @PathVariable Long ticketTypeId
    ){
        TicketType ticketType = ticketTypeRepository.findById(ticketTypeId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "ticket type not found"));
        Showtime showtime = ticketType.getShowtime();
        Event event = ticketType.getShowtime().getEvent();
        String organizerName = iamFeignClient.getOrganizationById(event.getOrganizerId()).getOrganizationName();
        EventDetailInternalResponse eventDetailInternalResponse = EventDetailInternalResponse.toDto(event, showtime, organizerName);
        return ResponseEntity.ok(BaseResponse.ok(eventDetailInternalResponse));
    }

    @PostMapping("/ticket-types/details")
    public ResponseEntity<BaseResponse<List<TicketTypeInternalResponse>>> getTicketDetails(
            @RequestBody List<Long> ticketTypeIds
    ) {
        return ResponseEntity.ok(BaseResponse.ok(ticketTypeService.getTicketDetailsInternal(ticketTypeIds)));
    }

    @GetMapping("/showtimes/{showtimeId}/checkers/{checkerId}/is-assigned")
    public ResponseEntity<Boolean> isCheckerAssigned(
            @PathVariable Long showtimeId,
            @PathVariable Long checkerId
    ) {
        boolean isAssigned = showtimeCheckerService.isCheckerAssigned(showtimeId, checkerId);
        return ResponseEntity.ok(isAssigned);
    }

    @GetMapping("/event/{eventId}/allow-resale")
    public ResponseEntity<Boolean> getAllowReservation(@PathVariable Long eventId) {
        boolean isAllowReservation = eventUtil.getEventOrElseThrow(eventId).getAllowResale();
        return ResponseEntity.ok(isAllowReservation);
    }

    @GetMapping("/bank/bin-code")
    public ResponseEntity<String> getBinCodeFromBankCode(@RequestParam String bankCode) {
        Bank bank = bankRepository.findByCode(bankCode)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Bank not found with code: " + bankCode));
        return ResponseEntity.ok(bank.getBin());
    }
}
