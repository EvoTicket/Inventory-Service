package com.capstone.inventoryservice.domain.service.chatbot;

import com.capstone.inventoryservice.exception.AppException;
import com.capstone.inventoryservice.exception.ErrorCode;
import com.capstone.inventoryservice.security.JwtUtil;
import com.capstone.inventoryservice.model.entity.ChatConversation;
import com.capstone.inventoryservice.model.repository.ChatConversationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class ChatBotService {

    private final ChatClient chatClient;
    private final EvoTicketTools evoTicketTools;
    private final JwtUtil jwtUtil;
    private final VectorStore vectorStore;
    private final ChatMemory chatMemory;
    private final ChatConversationRepository chatConversationRepository;

    public ChatBotService(
            ChatClient chatClient,
            EvoTicketTools evoTicketTools,
            JwtUtil jwtUtil,
            VectorStore vectorStore,
            ChatMemory chatMemory,
            ChatConversationRepository chatConversationRepository
    ) {
        this.chatClient = chatClient;
        this.evoTicketTools = evoTicketTools;
        this.jwtUtil = jwtUtil;
        this.vectorStore = vectorStore;
        this.chatMemory = chatMemory;
        this.chatConversationRepository = chatConversationRepository;
    }

    public List<ChatConversation> listConversations(Long userId) {
        if (userId == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "User not authenticated");
        }
        return chatConversationRepository.findByUserIdOrderByUpdatedAtDesc(userId);
    }

    public ChatConversation createConversation(Long userId) {
        if (userId == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "User not authenticated");
        }
        ChatConversation conversation = ChatConversation.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .title("Cuộc trò chuyện mới")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return chatConversationRepository.save(conversation);
    }

    public void deleteConversation(Long userId, String conversationId) {
        if (userId == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "User not authenticated");
        }
        ChatConversation conversation = chatConversationRepository.findById(conversationId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy cuộc trò chuyện"));
        if (!conversation.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.FORBIDDEN, "Bạn không có quyền xóa cuộc trò chuyện này");
        }

        chatConversationRepository.deleteMessagesByConversationId(conversationId);
        chatMemory.clear(conversationId);
        chatConversationRepository.delete(conversation);
    }

    public ChatConversation renameConversation(Long userId, String conversationId, String newTitle) {
        if (userId == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "User not authenticated");
        }
        if (newTitle == null || newTitle.trim().isEmpty()) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Tiêu đề không được để trống");
        }
        ChatConversation conversation = chatConversationRepository.findById(conversationId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy cuộc trò chuyện"));
        if (!conversation.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.FORBIDDEN, "Bạn không có quyền đổi tên cuộc trò chuyện này");
        }
        conversation.setTitle(newTitle);
        conversation.setUpdatedAt(LocalDateTime.now());
        return chatConversationRepository.save(conversation);
    }

    private void updateConversationMetadata(String conversationId, String firstQuestion, Long userId) {
        if (conversationId == null || conversationId.trim().isEmpty() || "anonymous".equals(conversationId)) return;

        Optional<ChatConversation> convOpt = chatConversationRepository.findById(conversationId);
        if (convOpt.isPresent()) {
            ChatConversation conversation = convOpt.get();
            if (!conversation.getUserId().equals(userId)) {
                throw new AppException(ErrorCode.FORBIDDEN, "Bạn không có quyền truy cập cuộc trò chuyện này");
            }

            if ("Cuộc trò chuyện mới".equals(conversation.getTitle()) && firstQuestion != null && !firstQuestion.trim().isEmpty()) {
                String cleanTitle = firstQuestion.trim();
                if (cleanTitle.length() > 50) {
                    cleanTitle = cleanTitle.substring(0, 47) + "...";
                }
                conversation.setTitle(cleanTitle);
            }

            conversation.setUpdatedAt(LocalDateTime.now());
            chatConversationRepository.save(conversation);
        }
    }

    public Flux<String> chatStream(String conversationId, String question) {
        Long userId = jwtUtil.getDataFromAuth().userId();

        updateConversationMetadata(conversationId, question, userId);

        return callToolCallingClientStream(userId, conversationId, question);
    }

    private Flux<String> callToolCallingClientStream(Long userId, String conversationId, String question) {
        try {
            String fullQuestion = (userId != null)
                    ? question + "\n\n[Thông tin phiên: userId=" + userId + ", dùng giá trị này khi gọi tool cần userId]"
                    : question;

            Object chatConvId = (conversationId != null && !conversationId.trim().isEmpty())
                    ? conversationId
                    : (userId != null ? userId.toString() : "anonymous");

            String model = "gemini-3.1-flash-lite";

            return chatClient.prompt()
                    .options(GoogleGenAiChatOptions.builder()
                            .model(model)
                            .build())
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatConvId))
                    .user(u -> u.text(fullQuestion))
                    .tools(evoTicketTools)
                    .stream()
                    .content();
        } catch (Exception e) {
            log.error("[ChatBot] Lỗi khi gọi AI (Tool Calling): {}", e.getMessage(), e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    public String chat(String conversationId, String question) {
        Long userId = jwtUtil.getDataFromAuth().userId();

        updateConversationMetadata(conversationId, question, userId);

        return callToolCallingClient(userId, conversationId, question);
    }

    private String callToolCallingClient(Long userId, String conversationId, String question) {
        try {
            String fullQuestion = (userId != null)
                    ? question + "\n\n[Thông tin phiên: userId=" + userId + ", dùng giá trị này khi gọi tool cần userId]"
                    : question;

            Object chatConvId = (conversationId != null && !conversationId.trim().isEmpty())
                    ? conversationId
                    : (userId != null ? userId.toString() : "anonymous");

            String model = "gemini-3.1-flash-lite";

            return chatClient.prompt()
                    .options(GoogleGenAiChatOptions.builder()
                            .model(model)
                            .build())
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatConvId))
                    .user(u -> u.text(fullQuestion))
                    .tools(evoTicketTools)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("[ChatBot] Lỗi khi gọi AI (Tool Calling): {}", e.getMessage(), e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    public List<Message> getChatMessages(String conversationId) {
        Long userId = jwtUtil.getDataFromAuth().userId();
        if (userId == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "User not authenticated");
        }

        String targetId = conversationId;
        if (targetId == null || targetId.trim().isEmpty()) {
            targetId = userId.toString();
        } else {
            Optional<ChatConversation> convOpt = chatConversationRepository.findById(targetId);
            if (convOpt.isPresent() && !convOpt.get().getUserId().equals(userId)) {
                throw new AppException(ErrorCode.FORBIDDEN, "Bạn không có quyền truy cập cuộc trò chuyện này");
            }
        }

        return chatMemory.get(targetId);
    }

    public void clearChatHistory(String conversationId) {
        Long userId = jwtUtil.getDataFromAuth().userId();
        if (userId == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "User not authenticated");
        }

        String targetId = conversationId;
        if (targetId == null || targetId.trim().isEmpty()) {
            targetId = userId.toString();
        } else {
            Optional<ChatConversation> convOpt = chatConversationRepository.findById(targetId);
            if (convOpt.isPresent() && !convOpt.get().getUserId().equals(userId)) {
                throw new AppException(ErrorCode.FORBIDDEN, "Bạn không có quyền xóa cuộc trò chuyện này");
            }
        }

        chatMemory.clear(targetId);
    }
}