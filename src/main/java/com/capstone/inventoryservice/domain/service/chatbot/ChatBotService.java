package com.capstone.inventoryservice.domain.service.chatbot;

import com.capstone.inventoryservice.exception.AppException;
import com.capstone.inventoryservice.exception.ErrorCode;
import com.capstone.inventoryservice.security.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.content.Media;
import org.springframework.ai.document.Document;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class ChatBotService {

    private final ChatClient chatClient;
    private final EvoTicketTools evoTicketTools;
    private final JwtUtil jwtUtil;
    private final VectorStore vectorStore;
    private final ChatMemory chatMemory;

    public ChatBotService(
            ChatClient chatClient,
            EvoTicketTools evoTicketTools,
            JwtUtil jwtUtil,
            VectorStore vectorStore,
            ChatMemory chatMemory
    ) {
        this.chatClient = chatClient;
        this.evoTicketTools = evoTicketTools;
        this.jwtUtil = jwtUtil;
        this.vectorStore = vectorStore;
        this.chatMemory = chatMemory;
    }

    public record FileData(MimeType mimeType, Resource resource) {}

    public String chat(String question, List<MultipartFile> files, boolean useRag) {
        Long userId = jwtUtil.getDataFromAuth().userId();

        List<FileData> filesData = convertResources(files);
        List<Media> mediaList = filesData.stream()
                .map(fd -> new Media(fd.mimeType(), fd.resource()))
                .toList();
        List<Resource> resourceList = filesData.stream()
                .map(FileData::resource)
                .toList();

        if (useRag && !resourceList.isEmpty()) {
            CompletableFuture.runAsync(() -> ingestResources(resourceList));
        }

        return callToolCallingClient(userId, question, mediaList);
    }

    private String callToolCallingClient(Long userId, String question, List<Media> mediaList) {
        try {
            String fullQuestion = (userId != null)
                    ? question + "\n\n[Thông tin phiên: userId=" + userId + ", dùng giá trị này khi gọi tool cần userId]"
                    : question;

            Object conversationId = userId != null ? userId : "anonymous";

            String model = mediaList != null && !mediaList.isEmpty() ? "gemini-3-flash-preview" : "gemini-3.1-flash-lite-preview";

            return chatClient.prompt()
                    .options(GoogleGenAiChatOptions.builder()
                        .model(model)
                        .build())
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .user(u -> {
                        u.text(fullQuestion);
                        if (mediaList != null && !mediaList.isEmpty()) {
                            mediaList.forEach(u::media);
                        }
                    })
                    .tools(evoTicketTools)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("[ChatBot] Lỗi khi gọi AI (Tool Calling): {}", e.getMessage(), e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    private List<FileData> convertResources(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) return List.of();

        List<FileData> result = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;

            try {
                MimeType mime = Optional.ofNullable(file.getContentType())
                        .map(MimeTypeUtils::parseMimeType)
                        .orElse(MimeTypeUtils.APPLICATION_OCTET_STREAM);

                Resource resource = new InputStreamResource(file.getInputStream()) {
                    @Override
                    public String getFilename() {
                        return file.getOriginalFilename();
                    }
                };

                result.add(new FileData(mime, resource));

            } catch (Exception e) {
                log.error("[ChatBot] Lỗi đọc file: {}", file.getOriginalFilename(), e);
            }
        }

        return result;
    }

    private void ingestResources(List<Resource> resources) {
        try {
            TextSplitter splitter = new TokenTextSplitter();

            List<CompletableFuture<List<Document>>> futures = resources.stream()
                    .map(resource -> CompletableFuture.supplyAsync(() -> {
                        try {
                            TikaDocumentReader reader = new TikaDocumentReader(resource);
                            List<Document> docs = splitter.split(reader.read());
                            docs.forEach(d -> d.getMetadata().put("filename", resource.getFilename()));
                            return docs;
                        } catch (Exception e) {
                            log.error("[ChatBot] Lỗi khi đọc file {}: {}", resource.getFilename(), e.getMessage(), e);
                            return List.<Document>of();
                        }
                    }))
                    .toList();

            List<Document> allDocuments = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> futures.stream()
                            .flatMap(f -> f.join().stream())
                            .toList())
                    .join();

            if (!allDocuments.isEmpty()) {
                vectorStore.accept(allDocuments);
                log.info("[ChatBot] Đã ingest {} document chunks từ {} file(s)",
                        allDocuments.size(), resources.size());
            }
        } catch (Exception ex) {
            log.error("[ChatBot] Lỗi khi ingest file: {}", ex.getMessage(), ex);
        }
    }

    public List<Message> getChatMessages() {
        Long userId = jwtUtil.getDataFromAuth().userId();
        if (userId == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "User not authenticated");
        }

        return chatMemory.get(userId.toString());
    }

    public void clearChatHistory() {
        Long userId = jwtUtil.getDataFromAuth().userId();
        if (userId == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "User not authenticated");
        }

        chatMemory.clear(userId.toString());
    }
}
