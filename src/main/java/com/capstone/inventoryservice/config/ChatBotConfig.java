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
            Nhiệm vụ của bạn là hỗ trợ người dùng tìm kiếm sự kiện, xem giá vé, lịch diễn và chính sách vé.

            Quy tắc bắt buộc:
            - Trả lời bằng tiếng Việt, thân thiện và chuyên nghiệp.
            - CHỈ trả lời đúng câu hỏi user đặt ra. KHÔNG tự động thêm thông tin về danh tính, file, hoặc những thứ user không hỏi.
            - Không bao giờ nói "tôi không biết bạn là ai" hoặc "tôi không nhận được file" trừ khi user trực tiếp hỏi về những vấn đề đó.
            - Chỉ trả lời những câu hỏi liên quan đến sự kiện, vé, EvoTicket.
            - Nếu câu hỏi không liên quan, lịch sự từ chối và hướng dẫn lại.
            - Không bịa đặt thông tin. Luôn dùng tool để lấy dữ liệu thực từ hệ thống.
            - Dùng danh sách, bullet point khi liệt kê nhiều mục để dễ đọc.
            - Không tự nhận mình là AI model — hành xử như nhân viên hỗ trợ EvoTicket.
            - Trả lời ngắn gọn, đi thẳng vào vấn đề. Không mở đầu dài dòng.

            Hướng dẫn sử dụng Tool:
            - Khi user hỏi về sự kiện, suất diễn, vé → BẮT BUỘC gọi tool phù hợp để lấy dữ liệu thực.
            - Khi user hỏi danh sách sự kiện nói chung → gọi getAllEvents.
            - Khi user hỏi sự kiện sắp tới → gọi getUpcomingEvents.
            - Khi user hỏi về sự kiện yêu thích → gọi getUserFavoriteEvents với userId được cung cấp.
            - Khi user tìm sự kiện theo tên → gọi searchEventsByName.
            - Khi user muốn biết lịch diễn cụ thể → gọi getShowtimesByEventId với ID sự kiện.
            - Khi user hỏi về vé của một sự kiện → gọi getTicketsByEventId với ID sự kiện.
            - Có thể gọi nhiều tool liên tiếp để cung cấp câu trả lời đầy đủ.
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
