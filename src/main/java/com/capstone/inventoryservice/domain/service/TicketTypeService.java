package com.capstone.inventoryservice.domain.service;

import com.capstone.inventoryservice.domain.dto.request.CreateTicketTypeRequest;
import com.capstone.inventoryservice.domain.dto.request.OrderItemRequest;
import com.capstone.inventoryservice.domain.dto.request.UpdateTicketTypeRequest;
import com.capstone.inventoryservice.domain.client.ListTicketTypesInternalResponse;
import com.capstone.inventoryservice.domain.dto.response.TicketTypeResponse;
import com.capstone.inventoryservice.model.entity.Showtime;
import com.capstone.inventoryservice.model.entity.TicketType;
import com.capstone.inventoryservice.exception.AppException;
import com.capstone.inventoryservice.exception.ErrorCode;
import com.capstone.inventoryservice.domain.mapper.TicketTypeMapper;
import com.capstone.inventoryservice.model.repository.TicketTypeRepository;
import com.capstone.inventoryservice.domain.util.ShowtimeUtil;
import com.capstone.inventoryservice.domain.util.TicketTypeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketTypeService {

    private final TicketTypeRepository ticketTypeRepository;
    private final TicketTypeUtil ticketTypeUtil;
    private final ShowtimeUtil showtimeUtil;
    private final TicketTypeMapper ticketTypeMapper;

    @Transactional(readOnly = true)
    public List<TicketTypeResponse> getTicketTypesByEvent(Long eventId) {
        return ticketTypeRepository.findByEventId(eventId).stream()
                .map(ticketTypeMapper::convertToDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public TicketTypeResponse getTicketTypeById(Long ticketTypeId) {
        TicketType ticketType = ticketTypeUtil.getTicketTypeOrElseThrow(ticketTypeId);
        return ticketTypeMapper.convertToDTO(ticketType);
    }

    @Transactional(readOnly = true)
    public ListTicketTypesInternalResponse getTicketTypes(List<OrderItemRequest> listItems) {
        if (listItems == null || listItems.isEmpty()) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Danh sách ticket không được rỗng");
        }

        List<ListTicketTypesInternalResponse.TicketDetailResponse> ticketDetails = new ArrayList<>();
        Long eventId = null;
        String eventName = null;
        Long showtimeId = null;

        for (OrderItemRequest item : listItems) {

            TicketType ticket = ticketTypeRepository
                    .findAvailableTicket(item.getTicketTypeId(), item.getQuantity())
                    .orElseThrow(() ->
                            new AppException(
                                    ErrorCode.BAD_REQUEST,
                                    "Ticket không đủ số lượng: " + item.getTicketTypeId()
                            )
                    );

            if (eventId == null) {
                eventId = ticket.getShowtime().getEvent().getId();
                eventName = ticket.getShowtime().getEvent().getEventName();
                showtimeId = ticket.getShowtime().getId();
            } else if (!eventId.equals(ticket.getShowtime().getEvent().getId())
                    && !showtimeId.equals(ticket.getShowtime().getId())
            ) {
                throw new AppException(
                        ErrorCode.BAD_REQUEST,
                        "Các ticket phải thuộc cùng một sự kiện và lịch chiếu"
                );
            }

            ListTicketTypesInternalResponse.TicketDetailResponse detail =
                    ListTicketTypesInternalResponse.TicketDetailResponse.builder()
                            .ticketTypeId(ticket.getId())
                            .ticketTypeName(ticket.getTypeName())
                            .quantity(item.getQuantity().longValue())
                            .price(ticket.getPrice())
                            .build();

            ticketDetails.add(detail);
        }

        return ListTicketTypesInternalResponse.builder()
                .eventId(eventId)
                .eventName(eventName)
                .showtimeId(showtimeId)
                .ticketDetails(ticketDetails)
                .build();
    }

    @Transactional(readOnly = true)
    public List<TicketTypeResponse> getActiveTicketTypes() {
        return ticketTypeRepository.findActiveTicketTypes(LocalDateTime.now()).stream()
                .map(ticketTypeMapper::convertToDTO)
                .toList();
    }

    @Transactional
    public TicketTypeResponse updateTicketType(Long ticketTypeId, UpdateTicketTypeRequest request) {
        TicketType ticketType = ticketTypeUtil.getTicketTypeOrElseThrow(ticketTypeId);

        if (request.getTypeName() != null) {
            ticketType.setTypeName(request.getTypeName());
        }
        if (request.getDescription() != null) {
            ticketType.setDescription(request.getDescription());
        }
        if (request.getPrice() != null) {
            ticketType.setPrice(request.getPrice());
        }
        if (request.getQuantityAvailable() != null) {
            ticketType.setQuantityTotal(request.getQuantityAvailable());
        }
        if (request.getMinPurchase() != null) {
            ticketType.setMinPurchase(request.getMinPurchase());
        }
        if (request.getMaxPurchase() != null) {
            ticketType.setMaxPurchase(request.getMaxPurchase());
        }
        if (request.getSaleStartDate() != null) {
            ticketType.setSaleStartDate(request.getSaleStartDate());
        }
        if (request.getSaleEndDate() != null) {
            ticketType.setSaleEndDate(request.getSaleEndDate());
        }
        if (request.getTicketTypeStatus() != null) {
            ticketType.setTicketTypeStatus(request.getTicketTypeStatus());
        }

        TicketType updatedTicketType = ticketTypeRepository.save(ticketType);
        return ticketTypeMapper.convertToDTO(updatedTicketType);
    }

    @Transactional
    public boolean deleteTicketType(Long ticketTypeId) {
        TicketType ticketType = ticketTypeUtil.getTicketTypeOrElseThrow(ticketTypeId);

        if (ticketType.getQuantitySold() > 0) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Cannot delete ticket type with sold tickets");
        }

        ticketTypeRepository.delete(ticketType);
        return true;
    }
}