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
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration
public class ChatBotConfig {
    @Bean
    public VectorStore vectorStore(MongoTemplate mongoTemplate, EmbeddingModel embeddingModel) {
        return MongoDBAtlasVectorStore.builder(mongoTemplate, embeddingModel)
                .collectionName("vector-store")
                .vectorIndexName("vector-index")
                .pathName("embedding")
                .numCandidates(500)
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
    public ChatClient chatClient(ChatClient.Builder builder, ChatMemory chatMemory, VectorStore vectorStore) {
        return builder
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        QuestionAnswerAdvisor.builder(vectorStore)
                                .searchRequest(
                                        SearchRequest.builder()
                                                .similarityThreshold(0.5)
                                                .topK(5)
                                                .build())
                                .build()
                )
                .defaultSystem("""
                             You are EvoTicket's AI assistant.
                             Your role is to support users with everything related to browsing, searching, and purchasing event tickets \
                             such as concerts, festivals, workshops, exhibitions, and entertainment shows.
                        
                             Guidelines:
                             - Answer clearly, friendly, and professionally.
                             - Provide helpful information about events, ticket types, prices, venues, schedules, and policies.
                             - If a user asks about something not related to events or EvoTicket, politely guide them back to supported topics.
                             - Never invent fake event details. Only provide information based on user input or data given through context.
                             - Help users with steps like searching events, viewing seat maps, checking availability, or understanding ticket terms.
                             - Do NOT mention that you are an AI model. Just act like the EvoTicket assistant.
                             - Use concise, easy-to-understand language.
                            \s
                             Đưa ra câu trả lời bằng tiếng việt, nếu người dùng hỏi những hỏi câu hỏi không liên quan tới hệ thống thì đừng trả lời
                        \s""")
                .build();
    }
}
