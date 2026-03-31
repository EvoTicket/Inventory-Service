package com.capstone.inventoryservice.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.mongo.MongoChatMemoryRepository;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.mongodb.atlas.MongoDBAtlasVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration
public class ChatBotConfig {

    static final String SYSTEM_PROMPT = """
            Bạn là trợ lý AI của EvoTicket — nền tảng mua bán vé sự kiện trực tuyến.
            Nhiệm vụ của bạn là hỗ trợ người dùng tìm kiếm sự kiện, xem giá vé, lịch diễn và chính sách vé.

            Quy tắc:
            - Trả lời bằng tiếng Việt, thân thiện và chuyên nghiệp.
            - Chỉ trả lời những câu hỏi liên quan đến sự kiện, vé, EvoTicket.
            - Nếu câu hỏi không liên quan, lịch sự từ chối và hướng dẫn lại.
            - Không bịa đặt thông tin. Chỉ dùng dữ liệu được cung cấp trong context.
            - Dùng danh sách, bullet point khi liệt kê nhiều mục để dễ đọc.
            - Không tự nhận mình là AI model — hành xử như nhân viên hỗ trợ EvoTicket.
            """;

    @Bean
    public VectorStore vectorStore(MongoTemplate mongoTemplate, EmbeddingModel embeddingModel) {
        return MongoDBAtlasVectorStore.builder(mongoTemplate, embeddingModel)
                .collectionName("vector-store")
                .vectorIndexName("vector-index")
                .pathName("embedding")
                .numCandidates(200)
                .initializeSchema(true)
                .batchingStrategy(new TokenCountBatchingStrategy())
                .build();
    }

    @Bean
    public ChatMemory chatMemory(MongoChatMemoryRepository mongoChatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(mongoChatMemoryRepository)
                .maxMessages(20)
                .build();
    }

    @Bean
    @Primary
    public ChatClient chatClient(ChatClient.Builder builder, ChatMemory chatMemory) {
        return builder
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }

    @Bean("ragChatClient")
    public ChatClient ragChatClient(ChatClient.Builder builder, ChatMemory chatMemory, VectorStore vectorStore) {
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
