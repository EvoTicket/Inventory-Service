package com.capstone.inventoryservice.domain.service;

import com.capstone.inventoryservice.exception.AppException;
import com.capstone.inventoryservice.exception.ErrorCode;
import com.capstone.inventoryservice.model.entity.Event;
import com.capstone.inventoryservice.model.entity.EventCategory;
import com.capstone.inventoryservice.model.entity.TicketType;
import com.capstone.inventoryservice.model.entity.UserFavoriteEvent;
import com.capstone.inventoryservice.model.repository.EventCategoryRepository;
import com.capstone.inventoryservice.model.repository.EventRepository;
import com.capstone.inventoryservice.model.repository.TicketTypeRepository;
import com.capstone.inventoryservice.model.repository.UserFavoriteEventRepository;
import com.capstone.inventoryservice.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.content.Media;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChatBotService {
    private final ChatClient chatClient;
    private final EventRepository eventRepository;
    private final EventCategoryRepository eventCategoryRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final UserFavoriteEventRepository userFavoriteEventRepository;
    private final JwtUtil jwtUtil;

    public String chatWithSmartQuery(String question, MultipartFile file) {
        Long userId = jwtUtil.getDataFromAuth().userId();

        String queryType = determineQueryType(question);
        String contextData = buildSmartContextData(queryType, userId);

        String systemPrompt = """
                You are an intelligent event management assistant with access to real-time event data.
                
                RELEVANT DATA FOR THIS QUERY:
                {context}
                
                Provide accurate, concise answers based on this data.
                Format responses nicely with bullet points or numbered lists when appropriate.
                If asking about specific events, include key details like date, venue, and ticket info.
                Answer in Vietnamese if the question is in Vietnamese.
                """;

        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemPrompt);
        String formattedSystemPrompt = systemPromptTemplate.createMessage(Map.of("context", contextData)).getText();

        if (file != null && !file.isEmpty()) {
            try {
                String contentType = file.getContentType();
                MimeType mimeType = contentType != null
                        ? MimeTypeUtils.parseMimeType(contentType)
                        : MimeTypeUtils.APPLICATION_OCTET_STREAM;

                Media media = new Media(mimeType, file.getResource());

                return chatClient
                        .prompt()
                        .system(formattedSystemPrompt)
                        .user(promptUserSpec -> promptUserSpec
                                .media(media)
                                .text(question))
                        .call()
                        .content();
            } catch (Exception e) {
                throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, e.getMessage());
            }
        }

        return chatClient
                .prompt()
                .system(formattedSystemPrompt)
                .user(question)
                .call()
                .content();
    }

    private String buildContextData(Long userId) {
        StringBuilder context = new StringBuilder();

        List<Event> events = eventRepository.findAll();
        context.append("EVENTS:\n");
        events.forEach(event -> context.append(String.format(
                "- ID: %d, Name: %s, Venue: %s, Address: %s, Start: %s, End: %s, Status: %s, Type: %s, Total Seats: %d, Category: %s%n",
                event.getId(),
                event.getEventName(),
                event.getVenue(),
                event.getFullAddress(),
                event.getStartDatetime(),
                event.getEndDatetime(),
                event.getEventStatus(),
                event.getEventType(),
                event.getTotalSeats(),
                event.getCategory() != null ? event.getCategory().getCategoryName() : "N/A"
        )));

        context.append("\nTICKET TYPES:\n");
        List<TicketType> ticketTypes = ticketTypeRepository.findAll();
        ticketTypes.forEach(ticket -> context.append(String.format(
                "- Event: %s, Type: %s, Price: %s VND, Available: %d, Sold: %d, Sale Period: %s to %s, Status: %s%n",
                ticket.getEvent().getEventName(),
                ticket.getTypeName(),
                ticket.getPrice(),
                ticket.getQuantityAvailable(),
                ticket.getQuantitySold(),
                ticket.getSaleStartDate(),
                ticket.getSaleEndDate(),
                ticket.getTicketTypeStatus()
        )));

        context.append("\nEVENT CATEGORIES:\n");
        List<EventCategory> categories = eventCategoryRepository.findAll();
        categories.forEach(category -> context.append(String.format(
                "- ID: %d, Name: %s, Description: %s, Total Events: %d%n",
                category.getId(),
                category.getCategoryName(),
                category.getDescription(),
                category.getEvents() != null ? category.getEvents().size() : 0
        )));


        if (userId != null) {
            context.append("\nUSER'S FAVORITE EVENTS:\n");
            List<UserFavoriteEvent> favorites = userFavoriteEventRepository.findByUserId(userId);
            favorites.forEach(fav -> context.append(String.format(
                    "- Event: %s, Liked at: %s%n",
                    fav.getEvent().getEventName(),
                    fav.getLikedAt()
            )));
        }

        context.append("\nSTATISTICS:\n");
        context.append(String.format("- Total Events: %d%n", events.size()));
        context.append(String.format("- Total Categories: %d%n", categories.size()));
        context.append(String.format("- Total Ticket Types: %d%n", ticketTypes.size()));

        long upcomingEvents = events.stream()
                .filter(e -> e.getStartDatetime() != null && e.getStartDatetime().isAfter(OffsetDateTime.now()))
                .count();
        context.append(String.format("- Upcoming Events: %d%n", upcomingEvents));

        return context.toString();
    }

    private String determineQueryType(String question) {
        String lowerQuestion = question.toLowerCase();

        if (lowerQuestion.contains("vé") || lowerQuestion.contains("ticket") ||
                lowerQuestion.contains("giá") || lowerQuestion.contains("price")) {
            return "TICKETS";
        } else if (lowerQuestion.contains("danh mục") || lowerQuestion.contains("category") ||
                lowerQuestion.contains("loại sự kiện")) {
            return "CATEGORIES";
        } else if (lowerQuestion.contains("yêu thích") || lowerQuestion.contains("favorite") ||
                lowerQuestion.contains("like")) {
            return "FAVORITES";
        } else if (lowerQuestion.contains("sắp diễn ra") || lowerQuestion.contains("upcoming") ||
                lowerQuestion.contains("sắp tới")) {
            return "UPCOMING_EVENTS";
        }

        return "GENERAL";
    }

    private String buildSmartContextData(String queryType, Long userId) {
        StringBuilder context = new StringBuilder();

        switch (queryType) {
            case "TICKETS":
                List<TicketType> tickets = ticketTypeRepository.findAll();
                context.append("AVAILABLE TICKETS:\n");
                tickets.forEach(t -> context.append(formatTicketInfo(t)));
                break;

            case "CATEGORIES":
                List<EventCategory> categories = eventCategoryRepository.findAll();
                context.append("EVENT CATEGORIES:\n");
                categories.forEach(c -> context.append(formatCategoryInfo(c)));
                break;

            case "FAVORITES":
                if (userId != null) {
                    List<UserFavoriteEvent> favorites = userFavoriteEventRepository.findByUserId(userId);
                    context.append("YOUR FAVORITE EVENTS:\n");
                    favorites.forEach(f -> context.append(formatEventInfo(f.getEvent())));
                }
                break;

            case "UPCOMING_EVENTS":
                List<Event> upcomingEvents = eventRepository.findByStartDatetimeAfter(OffsetDateTime.now());
                context.append("UPCOMING EVENTS:\n");
                upcomingEvents.forEach(e -> context.append(formatEventInfo(e)));
                break;

            default:
                context.append(buildContextData(userId));
        }

        return context.toString();
    }

    private String formatEventInfo(Event event) {
        return String.format(
                """
                • %s
                  - Địa điểm: %s, %s
                  - Thời gian: %s đến %s
                  - Trạng thái: %s
                  - Tổng chỗ: %d
                
                """,
                event.getEventName(),
                event.getVenue(),
                event.getFullAddress(),
                event.getStartDatetime(),
                event.getEndDatetime(),
                event.getEventStatus(),
                event.getTotalSeats()
        );
    }

    private String formatTicketInfo(TicketType ticket) {
        return String.format(
                """
                • %s - %s
                  - Giá: %s VND
                  - Còn lại: %d/%d vé
                  - Đã bán: %d
                  - Thời gian bán: %s đến %s
                
                """,
                ticket.getEvent().getEventName(),
                ticket.getTypeName(),
                ticket.getPrice(),
                ticket.getQuantityAvailable(),
                ticket.getQuantityAvailable() + ticket.getQuantitySold(),
                ticket.getQuantitySold(),
                ticket.getSaleStartDate(),
                ticket.getSaleEndDate()
        );
    }

    private String formatCategoryInfo(EventCategory category) {
        return String.format(
                """
                • %s
                  - Mô tả: %s
                  - Số lượng sự kiện: %d
                
                """,
                category.getCategoryName(),
                category.getDescription(),
                category.getEvents() != null ? category.getEvents().size() : 0
        );
    }
}
