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
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.content.Media;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ChatBotService — tối ưu hóa theo hướng:
 *
 * 1. KHÔNG RAG mặc định: câu hỏi thông thường dùng structured DB query để inject context.
 * 2. RAG chỉ bật khi user upload file — đọc nội dung file và tra cứu trong vector store.
 * 3. Intent detection mở rộng để load đúng dữ liệu, giảm noise đưa vào context.
 * 4. System prompt một nguồn duy nhất trong ChatBotConfig (không trùng lặp).
 */
@Slf4j
@Service
public class ChatBotService {

    private final ChatClient chatClient;
    private final ChatClient ragChatClient;
    private final EventRepository eventRepository;
    private final EventCategoryRepository eventCategoryRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final UserFavoriteEventRepository userFavoriteEventRepository;
    private final JwtUtil jwtUtil;
    private final ChatMessageService chatMessageService;
    private final VectorStore vectorStore;

    public ChatBotService(
            ChatClient chatClient,
            @Qualifier("ragChatClient") ChatClient ragChatClient,
            EventRepository eventRepository,
            EventCategoryRepository eventCategoryRepository,
            TicketTypeRepository ticketTypeRepository,
            UserFavoriteEventRepository userFavoriteEventRepository,
            JwtUtil jwtUtil,
            ChatMessageService chatMessageService,
            VectorStore vectorStore
    ) {
        this.chatClient = chatClient;
        this.ragChatClient = ragChatClient;
        this.eventRepository = eventRepository;
        this.eventCategoryRepository = eventCategoryRepository;
        this.ticketTypeRepository = ticketTypeRepository;
        this.userFavoriteEventRepository = userFavoriteEventRepository;
        this.jwtUtil = jwtUtil;
        this.chatMessageService = chatMessageService;
        this.vectorStore = vectorStore;
    }

    // =========================================================
    // Public API
    // =========================================================

    public String chat(String question, List<MultipartFile> files) {
        Long userId = jwtUtil.getDataFromAuth().userId();
        boolean hasFiles = files != null && !files.isEmpty();

        // Nếu có file → ingest vào vector store, dùng RAG client
        if (hasFiles) {
            ingestFiles(files);
            String answer = callRagClient(userId, question, files);
            saveMessages(userId, question, files, answer);
            return answer;
        }

        // Không có file → dùng structured context từ DB, không RAG
        QueryIntent intent = detectIntent(question);
        String context = buildContext(intent, userId);
        String answer = callChatClient(userId, question, context);
        saveMessages(userId, question, null, answer);
        return answer;
    }

    // =========================================================
    // Intent Detection
    // =========================================================

    /**
     * Phân loại ý định người dùng để chỉ load đúng dữ liệu cần thiết.
     * Tránh load toàn bộ DB vào context một cách mù quáng.
     */
    private QueryIntent detectIntent(String question) {
        String q = question.toLowerCase();

        // --- Ticket / Price ---
        if (containsAny(q, "vé", "ticket", "giá", "price", "mua", "bán", "còn vé", "hết vé",
                "loại vé", "thời gian bán")) {
            return QueryIntent.TICKETS;
        }

        // --- Category ---
        if (containsAny(q, "danh mục", "category", "loại sự kiện", "thể loại")) {
            return QueryIntent.CATEGORIES;
        }

        // --- Favorites ---
        if (containsAny(q, "yêu thích", "favorite", "đã thích", "đã like", "quan tâm")) {
            return QueryIntent.FAVORITES;
        }

        // --- Upcoming events ---
        if (containsAny(q, "sắp diễn ra", "upcoming", "sắp tới", "tuần này", "tháng này",
                "sắp có", "gần đây")) {
            return QueryIntent.UPCOMING_EVENTS;
        }

        // --- Search by name/keyword ---
        if (containsAny(q, "tìm", "search", "tên", "có sự kiện", "event nào")) {
            return QueryIntent.SEARCH_EVENTS;
        }

        // --- Statistics / Overview ---
        if (containsAny(q, "thống kê", "tổng", "bao nhiêu", "số lượng", "overview")) {
            return QueryIntent.STATISTICS;
        }

        return QueryIntent.GENERAL;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }

    private enum QueryIntent {
        TICKETS, CATEGORIES, FAVORITES, UPCOMING_EVENTS, SEARCH_EVENTS, STATISTICS, GENERAL
    }

    // =========================================================
    // Context Builders — load đúng dữ liệu theo intent
    // =========================================================

    private String buildContext(QueryIntent intent, Long userId) {
        return switch (intent) {
            case TICKETS -> buildTicketsContext();
            case CATEGORIES -> buildCategoriesContext();
            case FAVORITES -> buildFavoritesContext(userId);
            case UPCOMING_EVENTS -> buildUpcomingEventsContext();
            case SEARCH_EVENTS -> buildAllEventsContext(); // fallback search qua toàn bộ tên event
            case STATISTICS -> buildStatisticsContext();
            case GENERAL -> buildGeneralContext(userId);
        };
    }

    private String buildTicketsContext() {
        StringBuilder sb = new StringBuilder("=== THÔNG TIN VÉ ===\n");
        ticketTypeRepository.findAll().forEach(t -> sb.append(formatTicket(t)));
        return sb.toString();
    }

    private String buildCategoriesContext() {
        StringBuilder sb = new StringBuilder("=== DANH MỤC SỰ KIỆN ===\n");
        eventCategoryRepository.findAll().forEach(c -> sb.append(formatCategory(c)));
        return sb.toString();
    }

    private String buildFavoritesContext(Long userId) {
        if (userId == null) return "Không có thông tin yêu thích (chưa đăng nhập).";
        StringBuilder sb = new StringBuilder("=== SỰ KIỆN YÊU THÍCH CỦA BẠN ===\n");
        List<UserFavoriteEvent> favorites = userFavoriteEventRepository.findByUserId(userId);
        if (favorites.isEmpty()) {
            sb.append("Bạn chưa yêu thích sự kiện nào.\n");
        } else {
            favorites.forEach(f -> sb.append(formatEvent(f.getEvent())));
        }
        return sb.toString();
    }

    private String buildUpcomingEventsContext() {
        StringBuilder sb = new StringBuilder("=== SỰ KIỆN SẮP DIỄN RA ===\n");
        List<Event> events = eventRepository.findByStartDatetimeAfter(LocalDateTime.now());
        if (events.isEmpty()) {
            sb.append("Hiện không có sự kiện sắp diễn ra.\n");
        } else {
            events.forEach(e -> sb.append(formatEvent(e)));
        }
        return sb.toString();
    }

    private String buildAllEventsContext() {
        StringBuilder sb = new StringBuilder("=== TẤT CẢ SỰ KIỆN ===\n");
        eventRepository.findAll().forEach(e -> sb.append(formatEventBrief(e)));
        return sb.toString();
    }

    private String buildStatisticsContext() {
        long totalEvents = eventRepository.count();
        long totalCategories = eventCategoryRepository.count();
        long totalTicketTypes = ticketTypeRepository.count();
        long upcoming = eventRepository.findByStartDatetimeAfter(LocalDateTime.now()).size();

        return String.format("""
                === THỐNG KÊ TỔNG QUAN ===
                - Tổng số sự kiện: %d
                - Sự kiện sắp diễn ra: %d
                - Tổng danh mục: %d
                - Tổng loại vé: %d
                """, totalEvents, upcoming, totalCategories, totalTicketTypes);
    }

    /**
     * GENERAL: chỉ load tóm tắt sự kiện + vé — không load description dài.
     * Tránh gửi quá nhiều token vào context không cần thiết.
     */
    private String buildGeneralContext(Long userId) {
        StringBuilder sb = new StringBuilder();

        // Tóm tắt sự kiện (tên, ngày, địa điểm, trạng thái)
        sb.append("=== SỰ KIỆN ===\n");
        eventRepository.findAll().forEach(e -> sb.append(formatEventBrief(e)));

        // Tóm tắt vé
        sb.append("\n=== VÉ ===\n");
        ticketTypeRepository.findAll().forEach(t -> sb.append(formatTicketBrief(t)));

        // Yêu thích nếu có user
        if (userId != null) {
            List<UserFavoriteEvent> favs = userFavoriteEventRepository.findByUserId(userId);
            if (!favs.isEmpty()) {
                sb.append("\n=== YÊU THÍCH ===\n");
                favs.forEach(f -> sb.append("- ").append(f.getEvent().getEventName()).append("\n"));
            }
        }

        return sb.toString();
    }

    // =========================================================
    // AI Client Calls
    // =========================================================

    private String callChatClient(Long userId, String question, String context) {
        String userMessage = buildUserMessage(question, context);
        try {
            return chatClient.prompt()
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, userId))
                    .user(userMessage)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("[ChatBot] Lỗi khi gọi AI (no-RAG): {}", e.getMessage(), e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    private String callRagClient(Long userId, String question, List<MultipartFile> files) {
        try {
            return ragChatClient.prompt()
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, userId))
                    .user(user -> {
                        user.text(question);
                        if (files != null) {
                            for (MultipartFile file : files) {
                                String ct = file.getContentType();
                                MimeType mime = ct != null
                                        ? MimeTypeUtils.parseMimeType(ct)
                                        : MimeTypeUtils.APPLICATION_OCTET_STREAM;
                                user.media(new Media(mime, file.getResource()));
                            }
                        }
                    })
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("[ChatBot] Lỗi khi gọi AI (RAG): {}", e.getMessage(), e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * Đặt context vào phần user message thay vì system prompt.
     * Giúp tách biệt rõ: system = hành vi, user = dữ liệu + câu hỏi.
     */
    private String buildUserMessage(String question, String context) {
        if (context == null || context.isBlank()) {
            return question;
        }
        return String.format("""
                [DỮ LIỆU HỆ THỐNG — chỉ dùng những thông tin dưới đây để trả lời]
                %s
                
                [CÂU HỎI CỦA NGƯỜI DÙNG]
                %s
                """, context, question);
    }

    // =========================================================
    // Ingest (chỉ ingest file, KHÔNG ingest câu hỏi)
    // =========================================================

    /**
     * Chỉ đọc nội dung FILE và đưa vào vector store.
     * Câu hỏi của user KHÔNG được ingest — tránh gây nhiễu embedding.
     */
    private void ingestFiles(List<MultipartFile> files) {
        try {
            List<Document> allDocuments = new ArrayList<>();
            TextSplitter splitter = new TokenTextSplitter();

            for (MultipartFile file : files) {
                if (file.isEmpty()) continue;

                Resource resource = new ByteArrayResource(file.getBytes()) {
                    @Override
                    public String getFilename() {
                        return file.getOriginalFilename();
                    }
                };

                TikaDocumentReader reader = new TikaDocumentReader(resource);
                List<Document> docs = splitter.split(reader.read());
                docs.forEach(d -> d.getMetadata().put("filename", resource.getFilename()));
                allDocuments.addAll(docs);
            }

            if (!allDocuments.isEmpty()) {
                vectorStore.accept(allDocuments);
                log.info("[ChatBot] Đã ingest {} document chunks từ {} file(s)",
                        allDocuments.size(), files.size());
            }
        } catch (Exception ex) {
            log.error("[ChatBot] Lỗi khi ingest file: {}", ex.getMessage(), ex);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }

    // =========================================================
    // Async Save
    // =========================================================

    private void saveMessages(Long userId, String question, List<MultipartFile> files, String answer) {
        chatMessageService.saveUserMessage(userId, question, files);
        chatMessageService.saveAssistantMessage(userId, answer);
    }

    // =========================================================
    // Formatters — ngắn gọn, đủ thông tin
    // =========================================================

    private String formatEvent(Event e) {
        return String.format("""
                • %s
                  - Địa điểm: %s
                  - Thời gian: %s → %s
                  - Trạng thái: %s | Chỗ ngồi: %d
                """,
                e.getEventName(),
                e.getFullAddress(),
                e.getStartDatetime(), e.getEndDatetime(),
                e.getEventStatus(), e.getTotalSeats()
        );
    }

    /** Tóm tắt rất ngắn — dùng cho GENERAL context tránh token bloat */
    private String formatEventBrief(Event e) {
        return String.format("- %s | %s | %s\n",
                e.getEventName(),
                e.getStartDatetime() != null ? e.getStartDatetime().toLocalDate() : "N/A",
                e.getEventStatus()
        );
    }

    private String formatTicket(TicketType t) {
        return String.format("""
                • [%s] %s
                  - Giá: %s VND | Còn lại: %d | Đã bán: %d
                  - Bán từ: %s đến %s | Trạng thái: %s
                """,
                t.getEvent().getEventName(),
                t.getTypeName(),
                t.getPrice(),
                t.getQuantityTotal() - t.getQuantitySold(),
                t.getQuantitySold(),
                t.getSaleStartDate(), t.getSaleEndDate(),
                t.getTicketTypeStatus()
        );
    }

    private String formatTicketBrief(TicketType t) {
        return String.format("- [%s] %s — %s VND\n",
                t.getEvent().getEventName(), t.getTypeName(), t.getPrice());
    }

    private String formatCategory(EventCategory c) {
        return String.format("• %s — %s (%d sự kiện)\n",
                c.getCategoryName(),
                c.getDescription(),
                c.getEvents() != null ? c.getEvents().size() : 0
        );
    }
}
