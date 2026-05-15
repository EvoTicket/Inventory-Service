package com.capstone.inventoryservice.domain.service.chatbot;

import com.capstone.inventoryservice.model.entity.Event;
import com.capstone.inventoryservice.model.entity.Showtime;
import com.capstone.inventoryservice.model.entity.TicketType;
import com.capstone.inventoryservice.model.entity.UserFavoriteEvent;
import com.capstone.inventoryservice.model.enums.EventCategory;
import com.capstone.inventoryservice.model.repository.EventRepository;
import com.capstone.inventoryservice.model.repository.ShowtimeRepository;
import com.capstone.inventoryservice.model.repository.TicketTypeRepository;
import com.capstone.inventoryservice.model.repository.UserFavoriteEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/**
 * EvoTicketTools — Spring AI Tool Calling
 *
 * Mỗi @Tool là một hàm mà LLM có thể chủ động gọi khi cần thông tin cụ thể.
 * Thay vì inject toàn bộ context thủ công, LLM tự quyết định cần gọi tool nào.
 *
 * Tham khảo: https://docs.spring.io/spring-ai/reference/api/tools.html
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EvoTicketTools {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final EventRepository eventRepository;
    private final ShowtimeRepository showtimeRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final UserFavoriteEventRepository userFavoriteEventRepository;

    // =========================================================
    // Tool: Tìm kiếm sự kiện
    // =========================================================

    @Tool(description = """
            Tìm kiếm tất cả sự kiện đang có trong hệ thống EvoTicket.
            Trả về danh sách sự kiện gồm tên, địa điểm, trạng thái và lịch diễn (showtime) gần nhất.
            Dùng khi user hỏi về các sự kiện hiện có, sự kiện theo danh mục, hoặc muốn xem tổng quan.
            """)
    @Transactional(readOnly = true)
    public String getAllEvents() {
        log.info("[Tool] getAllEvents called");
        List<Event> events = eventRepository.findAll();
        if (events.isEmpty()) return "Hiện không có sự kiện nào trong hệ thống.";

        StringBuilder sb = new StringBuilder("=== DANH SÁCH SỰ KIỆN ===\n");
        for (Event e : events) {
            sb.append(formatEventSummary(e));
        }
        return sb.toString();
    }

    @Tool(description = """
            Tìm kiếm sự kiện sắp diễn ra trong tương lai (theo lịch showtime).
            Trả về danh sách sự kiện chưa kết thúc, sắp xếp theo thời gian gần nhất.
            Dùng khi user hỏi: sự kiện sắp tới, sự kiện tuần này, sự kiện tháng này, upcoming events.
            """)
    @Transactional(readOnly = true)
    public String getUpcomingEvents() {
        log.info("[Tool] getUpcomingEvents called");
        List<Showtime> showtimes = showtimeRepository.findUpcomingShowtimes(LocalDateTime.now());
        if (showtimes.isEmpty()) return "Hiện không có sự kiện nào sắp diễn ra.";

        StringBuilder sb = new StringBuilder("=== SỰ KIỆN SẮP DIỄN RA ===\n");
        showtimes.stream()
                .map(Showtime::getEvent)
                .distinct()
                .forEach(e -> sb.append(formatEventSummary(e)));
        return sb.toString();
    }

    @Tool(description = """
            Tìm sự kiện theo tên hoặc từ khóa (tìm kiếm không phân biệt hoa thường).
            Trả về các sự kiện có tên chứa từ khóa.
            Dùng khi user nhắc đến tên cụ thể của sự kiện hoặc muốn tìm kiếm.
            """)
    @Transactional(readOnly = true)
    public String searchEventsByName(
            @ToolParam(description = "Từ khóa tìm kiếm tên sự kiện, ví dụ: 'concert', 'festival', 'hội thảo'") String keyword
    ) {
        log.info("[Tool] searchEventsByName called with keyword={}", keyword);
        List<Event> all = eventRepository.findAll();
        String lowerKey = keyword.toLowerCase();

        List<Event> matched = all.stream()
                .filter(e -> e.getEventName() != null && e.getEventName().toLowerCase().contains(lowerKey))
                .toList();

        if (matched.isEmpty()) return "Không tìm thấy sự kiện nào khớp với từ khóa: " + keyword;

        StringBuilder sb = new StringBuilder("=== KẾT QUẢ TÌM KIẾM: \"" + keyword + "\" ===\n");
        matched.forEach(e -> sb.append(formatEventDetail(e)));
        return sb.toString();
    }

    @Tool(description = """
            Tìm sự kiện theo danh mục (category).
            Danh mục hợp lệ: LIVESTAGE, STAGE_ART, WORKSHOP, SPORTS, EXHIBITION.
            Dùng khi user hỏi về loại sự kiện cụ thể như: sân khấu, nghệ thuật, thể thao, hội thảo...
            """)
    @Transactional(readOnly = true)
    public String getEventsByCategory(
            @ToolParam(description = "Tên danh mục sự kiện, ví dụ: MUSIC, SPORT, WORKSHOP, LIVESTAGE") String categoryName
    ) {
        log.info("[Tool] getEventsByCategory called with category={}", categoryName);
        EventCategory category;
        try {
            category = EventCategory.valueOf(categoryName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return "Danh mục không hợp lệ: " + categoryName +
                   "\nDanh mục hợp lệ: " + Arrays.stream(EventCategory.values())
                           .map(EventCategory::getDisplayName).toList();
        }

        List<Event> events = eventRepository.findAll().stream()
                .filter(ev -> category.equals(ev.getCategory()))
                .toList();

        if (events.isEmpty()) return "Không có sự kiện nào trong danh mục: " + category.getDisplayName();

        StringBuilder sb = new StringBuilder("=== SỰ KIỆN DANH MỤC: " + category.getDisplayName() + " ===\n");
        events.forEach(e -> sb.append(formatEventSummary(e)));
        return sb.toString();
    }

    // =========================================================
    // Tool: Thông tin Showtime
    // =========================================================

    @Tool(description = """
            Lấy danh sách các suất diễn (showtime) của một sự kiện cụ thể theo ID sự kiện.
            Trả về thời gian bắt đầu, kết thúc, địa điểm và trạng thái của từng suất diễn.
            Dùng khi user hỏi: sự kiện này diễn ra lúc mấy giờ, có bao nhiêu suất, lịch diễn chi tiết.
            """)
    @Transactional(readOnly = true)
    public String getShowtimesByEventId(
            @ToolParam(description = "ID của sự kiện cần xem lịch diễn") Long eventId
    ) {
        log.info("[Tool] getShowtimesByEventId called with eventId={}", eventId);
        List<Showtime> showtimes = showtimeRepository.findByEventIdWithTicketTypes(eventId);
        if (showtimes.isEmpty()) return "Không tìm thấy suất diễn nào cho sự kiện ID: " + eventId;

        String eventName = showtimes.get(0).getEvent().getEventName();
        StringBuilder sb = new StringBuilder("=== LỊCH DIỄN: " + eventName + " ===\n");
        for (Showtime s : showtimes) {
            sb.append(formatShowtimeDetail(s));
        }
        return sb.toString();
    }

    @Tool(description = """
            Lấy danh sách suất diễn còn hoạt động (chưa hủy, chưa kết thúc) của một sự kiện.
            Trả về suất diễn từ thời điểm hiện tại trở đi, bao gồm thông tin vé.
            Dùng khi user muốn biết còn suất diễn nào có thể mua vé.
            """)
    @Transactional(readOnly = true)
    public String getActiveShowtimesByEventId(
            @ToolParam(description = "ID của sự kiện cần kiểm tra suất diễn còn hoạt động") Long eventId
    ) {
        log.info("[Tool] getActiveShowtimesByEventId called with eventId={}", eventId);
        List<Showtime> showtimes = showtimeRepository.findActiveShowtimesByEventId(eventId, LocalDateTime.now());

        if (showtimes.isEmpty()) return "Hiện không còn suất diễn nào đang hoạt động cho sự kiện ID: " + eventId;

        String eventName = showtimes.get(0).getEvent().getEventName();
        StringBuilder sb = new StringBuilder("=== SUẤT DIỄN ĐANG HOẠT ĐỘNG: " + eventName + " ===\n");
        for (Showtime s : showtimes) {
            sb.append(formatShowtimeDetail(s));
        }
        return sb.toString();
    }

    // =========================================================
    // Tool: Thông tin Vé
    // =========================================================

    @Tool(description = """
            Lấy thông tin vé của một sự kiện cụ thể theo ID sự kiện.
            Trả về tên loại vé, giá, số lượng còn lại, thời gian bán và trạng thái.
            Dùng khi user hỏi: giá vé, mua vé ở đâu, còn vé không, các loại vé.
            """)
    @Transactional(readOnly = true)
    public String getTicketsByEventId(
            @ToolParam(description = "ID của sự kiện cần xem thông tin vé") Long eventId
    ) {
        log.info("[Tool] getTicketsByEventId called with eventId={}", eventId);
        List<TicketType> tickets = ticketTypeRepository.findByEventId(eventId);
        if (tickets.isEmpty()) return "Không tìm thấy thông tin vé cho sự kiện ID: " + eventId;

        String eventName = tickets.get(0).getShowtime().getEvent().getEventName();
        StringBuilder sb = new StringBuilder("=== VÉ CỦA SỰ KIỆN: " + eventName + " ===\n");
        tickets.forEach(t -> sb.append(formatTicketDetail(t)));
        return sb.toString();
    }

    @Tool(description = """
            Lấy danh sách tất cả loại vé đang còn hiệu lực bán (trong thời gian mở bán).
            Trả về các vé đang trong giai đoạn bán, chưa hết hạn.
            Dùng khi user hỏi: đang bán vé gì, có thể mua vé nào ngay bây giờ.
            """)
    @Transactional(readOnly = true)
    public String getActiveTicketTypes() {
        log.info("[Tool] getActiveTicketTypes called");
        List<TicketType> tickets = ticketTypeRepository.findActiveTicketTypes(LocalDateTime.now());
        if (tickets.isEmpty()) return "Hiện không có loại vé nào đang trong giai đoạn mở bán.";

        StringBuilder sb = new StringBuilder("=== VÉ ĐANG MỞ BÁN ===\n");
        tickets.forEach(t -> sb.append(formatTicketDetail(t)));
        return sb.toString();
    }

    // =========================================================
    // Tool: Yêu thích & Cá nhân hóa
    // =========================================================

    @Tool(description = """
            Lấy danh sách sự kiện yêu thích của người dùng theo userId.
            Trả về các sự kiện mà user đã đánh dấu yêu thích, kèm thông tin cơ bản.
            Dùng khi user hỏi: sự kiện tôi đã thích, danh sách yêu thích của tôi.
            """)
    @Transactional(readOnly = true)
    public String getUserFavoriteEvents(
            @ToolParam(description = "ID của người dùng cần xem danh sách yêu thích") Long userId
    ) {
        log.info("[Tool] getUserFavoriteEvents called with userId={}", userId);
        if (userId == null) return "Không thể lấy danh sách yêu thích: chưa có thông tin người dùng.";

        List<UserFavoriteEvent> favorites = userFavoriteEventRepository.findByUserId(userId);
        if (favorites.isEmpty()) return "Bạn chưa yêu thích sự kiện nào.";

        StringBuilder sb = new StringBuilder("=== SỰ KIỆN YÊU THÍCH CỦA BẠN ===\n");
        favorites.forEach(f -> sb.append(formatEventSummary(f.getEvent())));
        return sb.toString();
    }

    // =========================================================
    // Tool: Thống kê
    // =========================================================

    @Tool(description = """
            Lấy thống kê tổng quan về hệ thống EvoTicket: số sự kiện, suất diễn, loại vé, danh mục.
            Dùng khi user hỏi: tổng số sự kiện, có bao nhiêu sự kiện, thống kê tổng quan.
            """)
    @Transactional(readOnly = true)
    public String getSystemStatistics() {
        log.info("[Tool] getSystemStatistics called");
        long totalEvents = eventRepository.count();
        long totalTicketTypes = ticketTypeRepository.count();
        long totalUpcomingShowtimes = showtimeRepository.findUpcomingShowtimes(LocalDateTime.now()).size();
        long totalCategories = EventCategory.values().length;

        return String.format("""
                === THỐNG KÊ HỆ THỐNG EVOTICKET ===
                - Tổng số sự kiện: %d
                - Suất diễn sắp tới: %d
                - Tổng loại vé: %d
                - Danh mục sự kiện: %d loại
                """, totalEvents, totalUpcomingShowtimes, totalTicketTypes, totalCategories);
    }

    @Tool(description = """
            Lấy danh sách các sự kiện có lượt xem cao nhất.
            Dùng khi user hỏi: sự kiện nào được quan tâm nhiều nhất, sự kiện được xem nhiều nhất.
            """)
    @Transactional(readOnly = true)
    public String getMostViewedEvents(
            @ToolParam(description = "Số lượng sự kiện muốn lấy, mặc định là 5") Integer limit
    ) {
        int actualLimit = (limit == null || limit <= 0) ? 5 : limit;
        log.info("[Tool] getMostViewedEvents called with limit={}", actualLimit);

        Page<Event> events = eventRepository.findMostViewedEvents(PageRequest.of(0, actualLimit));
        if (events.isEmpty()) return "Hiện chưa có dữ liệu lượt xem.";

        StringBuilder sb = new StringBuilder("=== SỰ KIỆN XEM NHIỀU NHẤT ===\n");
        events.forEach(e -> sb.append(formatEventSummary(e)));
        return sb.toString();
    }

    @Tool(description = """
            Lấy danh sách các sự kiện đang hot nhất, được mua nhiều nhất và xem nhiều nhất.
            Trả về danh sách sự kiện kèm theo các chỉ số phổ biến.
            Dùng khi user hỏi: sự kiện nào đang hot, sự kiện được mua nhiều nhất, top sự kiện, hot nhất.
            """)
    @Transactional(readOnly = true)
    public String getTrendingEvents(
            @ToolParam(description = "Số lượng sự kiện muốn lấy, mặc định là 5") Integer limit
    ) {
        int actualLimit = (limit == null || limit <= 0) ? 5 : limit;
        log.info("[Tool] getTrendingEvents called with limit={}", actualLimit);

        Page<Event> trending = eventRepository.findTrendingEvents(LocalDateTime.now(), PageRequest.of(0, actualLimit));
        if (trending.isEmpty()) return "Hiện chưa có thống kê sự kiện hot.";

        StringBuilder sb = new StringBuilder("=== SỰ KIỆN HOT NHẤT ===\n");
        trending.forEach(e -> sb.append(formatEventSummary(e)));
        return sb.toString();
    }

    @Tool(description = """
            Trả về câu trả lời cho các câu hỏi thường gặp (FAQ) về hệ thống EvoTicket như: 
            Cách mua vé, cách thanh toán, quy định hoàn tiền, hỗ trợ khách hàng, vé điện tử, blockchain.
            Dùng khi user hỏi các câu hỏi chung về quy trình hoặc quy định của hệ thống.
            """)
    public String getFAQ() {
        log.info("[Tool] getFAQ called");
        return """
                === CÂU HỎI THƯỜNG GẶP (FAQ) ===
                
                1. Làm thế nào để mua vé trên EvoTicket?
                   Bạn chỉ cần chọn sự kiện yêu thích, chọn suất diễn và loại vé, sau đó nhấn 'Mua ngay'. Hệ thống hỗ trợ thanh toán qua VNPay và các ví điện tử phổ biến.
                
                2. Tôi nhận vé bằng cách nào?
                   Sau khi thanh toán thành công, vé điện tử dưới dạng QR Code sẽ được gửi về Email của bạn và hiển thị trong mục 'Vé của tôi' trên website.
                
                3. Quy định về việc hoàn trả hoặc hủy vé?
                   Việc hoàn trả vé phụ thuộc vào chính sách riêng của từng Ban tổ chức (BTC). Bạn có thể xem thông tin này trong phần mô tả sự kiện hoặc liên hệ trực tiếp với BTC.
                
                4. Tại sao EvoTicket sử dụng Blockchain?
                   Chúng tôi sử dụng Blockchain để định danh mỗi chiếc vé, đảm bảo vé không thể bị làm giả, giúp bạn an tâm khi mua vé và hỗ trợ chuyển nhượng vé an toàn.
                
                5. Tôi có thể chuyển nhượng vé cho người khác không?
                   Có, bạn có thể sử dụng tính năng 'Ký gửi/Chuyển nhượng' trong mục quản lý vé để bán lại hoặc tặng vé cho người khác một cách minh bạch.
                
                6. Làm sao để liên hệ với bộ phận hỗ trợ khách hàng?
                   Bạn có thể gửi yêu cầu qua email support@evoticket.com hoặc liên hệ Fanpage EvoTicket để được hỗ trợ nhanh nhất.
                """;
    }

    public String getAllCategories() {
        log.info("[Tool] getAllCategories called");
        StringBuilder sb = new StringBuilder("=== DANH MỤC SỰ KIỆN EVOTICKET ===\n");
        for (EventCategory cat : EventCategory.values()) {
            sb.append("• ").append(cat.getDisplayName()).append(" (").append(cat.name()).append(")\n");
        }
        return sb.toString();
    }

    // =========================================================
    // Formatters nội bộ
    // =========================================================

    private String formatEventSummary(Event e) {
        LocalDateTime start = e.getEarliestStart();
        return String.format("• [ID:%d] %s | %s | Trạng thái: %s\n",
                e.getId(),
                e.getEventName(),
                start != null ? start.format(FORMATTER) : "N/A",
                e.getEventStatus());
    }

    private String formatEventDetail(Event e) {
        LocalDateTime start = e.getEarliestStart();
        LocalDateTime end = e.getLatestEnd();
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("""
                • [ID:%d] %s
                  - Địa điểm: %s
                  - Lịch diễn: %s → %s
                  - Trạng thái: %s | Danh mục: %s
                """,
                e.getId(),
                e.getEventName(),
                e.getFullAddress(),
                start != null ? start.format(FORMATTER) : "N/A",
                end != null ? end.format(FORMATTER) : "N/A",
                e.getEventStatus(),
                e.getCategory() != null ? e.getCategory().getDisplayName() : "N/A"
        ));
        return sb.toString();
    }

    private String formatShowtimeDetail(Showtime s) {
        String ticketInfo = "";
        if (s.getTicketTypes() != null && !s.getTicketTypes().isEmpty()) {
            long available = s.getTicketTypes().stream()
                    .filter(t -> t.getQuantityTotal() != null && t.getQuantitySold() != null)
                    .mapToLong(t -> t.getQuantityTotal() - t.getQuantitySold())
                    .sum();
            ticketInfo = " | Vé còn lại: " + available;
        }

        return String.format("  → Suất [ID:%d]: %s → %s | %s%s%s\n",
                s.getId(),
                s.getStartDatetime() != null ? s.getStartDatetime().format(FORMATTER) : "N/A",
                s.getEndDatetime() != null ? s.getEndDatetime().format(FORMATTER) : "N/A",
                s.getFullAddress() != null ? s.getFullAddress() : (s.getVenue() != null ? s.getVenue() : "N/A"),
                ticketInfo,
                Boolean.TRUE.equals(s.getIsCancelled()) ? " [ĐÃ HỦY]" : "");
    }

    private String formatTicketDetail(TicketType t) {
        int remaining = (t.getQuantityTotal() != null ? t.getQuantityTotal() : 0)
                      - (t.getQuantitySold() != null ? t.getQuantitySold() : 0);
        return String.format("""
                  • [%s] %s
                    - Giá: %s VND | Còn lại: %d vé | Đã bán: %d vé
                    - Bán từ: %s → %s
                    - Trạng thái: %s
                """,
                t.getShowtime().getEvent().getEventName(),
                t.getTypeName(),
                t.getPrice(),
                remaining,
                t.getQuantitySold() != null ? t.getQuantitySold() : 0,
                t.getSaleStartDate() != null ? t.getSaleStartDate().format(FORMATTER) : "N/A",
                t.getSaleEndDate() != null ? t.getSaleEndDate().format(FORMATTER) : "N/A",
                t.getTicketTypeStatus());
    }
}
