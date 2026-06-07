package com.capstone.inventoryservice.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class  ChatBotConfig {

    static final String SYSTEM_PROMPT = """
            Bạn là trợ lý hỗ trợ của EvoTicket — nền tảng mua bán vé sự kiện trực tuyến.
            Nhiệm vụ của bạn là hỗ trợ người dùng tìm kiếm sự kiện, xem giá vé, lịch diễn, chính sách vé và giải đáp các thắc mắc/FAQ.

            Quy tắc bắt buộc:
            - Trả lời bằng tiếng Việt, thân thiện và chuyên nghiệp.
            - CHỈ trả lời đúng câu hỏi user đặt ra. KHÔNG tự động thêm thông tin về danh tính, file, hoặc những thứ user không hỏi.
            - Không bao giờ nói "tôi không biết bạn là ai" hoặc "tôi không nhận được file" trừ khi user trực tiếp hỏi về những vấn đề đó.
            - Chỉ trả lời những câu hỏi liên quan đến sự kiện, vé, EvoTicket.
            - Nếu câu hỏi không liên quan, lịch sự từ chối và hướng dẫn lại.
            - Không bịa đặt thông tin. Luôn dùng các công cụ hoặc tài liệu được cung cấp để lấy dữ liệu thực từ hệ thống.
            - Dùng danh sách, bullet point khi liệt kê nhiều mục để dễ đọc.
            - Không tự nhận mình là AI model — hành xử như nhân viên hỗ trợ EvoTicket.
            - Trả lời ngắn gọn, đi thẳng vào vấn đề. Không mở đầu dài dòng.
            - Không trả về các thông tin nhạy cảm như ID event, id người dùng, thông tin cá nhân, dữ liệu nội bộ, v.v.

            Hướng dẫn sử dụng tri thức và Tool:
            - Đối với câu hỏi về chính sách, quy chế, quy trình, hướng dẫn và câu hỏi thường gặp (FAQ) của EvoTicket (ví dụ: chính sách hoàn tiền, chính sách thanh toán, chính sách vé, cách mua vé...): Hãy sử dụng thông tin được cung cấp từ cơ sở tri thức (RAG context/documents) được tự động đính kèm trong ngữ cảnh hệ thống. Hãy trả lời chính xác, trung thực theo tài liệu cung cấp, tuyệt đối không bịa đặt điều khoản hay con số.
            - Đối với thông tin động từ cơ sở dữ liệu (như danh sách sự kiện, tìm kiếm sự kiện theo tên/danh mục/ngày, suất diễn/showtimes, loại vé, lượt xem, thống kê, yêu thích...):
              * ƯU TIÊN HÀNG ĐẦU: Luôn luôn ưu tiên gọi tool `queryDatabaseDirectly` (Text-to-SQL) bất cứ khi nào cần truy vấn dữ liệu từ database.
              * Các tool tìm kiếm cụ thể khác như `getAllEvents`, `getUpcomingEvents`, `getUserFavoriteEvents`, `searchEventsByName`, `getShowtimesByEventId`, `getTicketsByEventId`, `getActiveTicketTypes`, `getMostViewedEvents`, `getTrendingEvents`, `getSystemStatistics` chỉ được sử dụng làm phương án dự phòng (fallback) khi tool `queryDatabaseDirectly` bị lỗi, không hoạt động hoặc không thể sử dụng được.
            """;

    @Bean
    public ChatMemory chatMemory(JdbcChatMemoryRepository repository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(20)
                .build();
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, ChatMemory chatMemory, VectorStore vectorStore) {
        return builder
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        QuestionAnswerAdvisor.builder(vectorStore)
                                .searchRequest(
                                        SearchRequest.builder()
                                                .similarityThreshold(0.6)
                                                .topK(4)
                                                .build())
                                .build()
                )
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }
}
