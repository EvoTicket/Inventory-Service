package com.capstone.inventoryservice.domain.controller;

import com.capstone.inventoryservice.domain.dto.BaseResponse;
import com.capstone.inventoryservice.domain.dto.request.CreateTicketTypeRequest;
import com.capstone.inventoryservice.domain.dto.request.UpdateTicketTypeRequest;
import com.capstone.inventoryservice.domain.dto.response.TicketTypeResponse;
import com.capstone.inventoryservice.domain.service.TicketTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ticket-types")
@RequiredArgsConstructor
public class TicketTypeController {

    private final TicketTypeService ticketTypeService;

    @GetMapping("/event/{eventId}")
    public ResponseEntity<BaseResponse<List<TicketTypeResponse>>> getTicketTypesByEvent(@PathVariable Long eventId) {
        return ResponseEntity.ok(BaseResponse.ok("lấy danh sách ticket thành công", ticketTypeService.getTicketTypesByEvent(eventId)));
    }

    @GetMapping("/{ticketTypeId}")
    public ResponseEntity<BaseResponse<TicketTypeResponse>> getTicketTypeById(@PathVariable Long ticketTypeId) {
        return ResponseEntity.ok(BaseResponse.ok(ticketTypeService.getTicketTypeById(ticketTypeId)));
    }

    @GetMapping("/active")
    public ResponseEntity<BaseResponse<List<TicketTypeResponse>>> getActiveTicketTypes() {
        return ResponseEntity.ok(BaseResponse.ok(ticketTypeService.getActiveTicketTypes()));
    }

    @PostMapping
    public ResponseEntity<BaseResponse<TicketTypeResponse>> createTicketType(@Valid @RequestBody CreateTicketTypeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.ok(ticketTypeService.createTicketType(request)));
    }

    @PutMapping("/{ticketTypeId}")
    public ResponseEntity<BaseResponse<TicketTypeResponse>> updateTicketType(
            @PathVariable Long ticketTypeId,
            @Valid @RequestBody UpdateTicketTypeRequest request) {
        return ResponseEntity.ok(BaseResponse.ok(ticketTypeService.updateTicketType(ticketTypeId, request)));
    }

    @DeleteMapping("/{ticketTypeId}")
    public ResponseEntity<BaseResponse<Boolean>> deleteTicketType(@PathVariable Long ticketTypeId) {
        return ResponseEntity.ok(BaseResponse.ok(ticketTypeService.deleteTicketType(ticketTypeId)));
    }
}