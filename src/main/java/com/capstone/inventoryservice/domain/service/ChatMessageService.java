package com.capstone.inventoryservice.domain.service;

import com.capstone.inventoryservice.domain.dto.response.ChatMessageResponse;
import com.capstone.inventoryservice.exception.AppException;
import com.capstone.inventoryservice.exception.ErrorCode;
import com.capstone.inventoryservice.model.entity.ChatMessage;
import com.capstone.inventoryservice.model.entity.ChatMessageMedia;
import com.capstone.inventoryservice.model.enums.SenderType;
import com.capstone.inventoryservice.model.repository.ChatMessageRepository;
import com.capstone.inventoryservice.security.JwtUtil;
import com.cloudinary.Cloudinary;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final Cloudinary cloudinary;
    private final JwtUtil jwtUtil;

    @Async
    public void saveUserMessage(Long userId,
                                String message,
                                List<MultipartFile> images) {
        List<ChatMessageMedia> mediaList = uploadImagesAsync(images, userId)
                .thenApply(imagesStr -> {
                    List<ChatMessageMedia> media = new ArrayList<>();
                    if (imagesStr != null && !imagesStr.isEmpty()) {
                        for (String url : imagesStr) {
                            media.add(ChatMessageMedia.builder().url(url).build());
                        }
                    }
                    return media;
                })
                .exceptionally(ex -> {
                    throw new AppException(ErrorCode.IO_EXCEPTION, "Không thể tải ảnh lên Cloudinary: " + ex.getMessage());
                })
                .join();

        chatMessageRepository.save(
                ChatMessage.builder()
                        .userId(userId)
                        .message(message)
                        .senderType(SenderType.USER)
                        .mediaList(mediaList)
                        .build()
        );
    }

    @Async
    public void saveAssistantMessage(Long userId,
                                     String message) {
        chatMessageRepository.save(ChatMessage.builder()
                .userId(userId)
                .message(message)
                .senderType(SenderType.ASSISTANT)
                .build());
    }

    public CompletableFuture<List<String>> uploadImagesAsync(List<MultipartFile> images, Long userId) {
        if (images == null || images.isEmpty()) {
            return CompletableFuture.completedFuture(new ArrayList<>());
        }

        String folder = "chat-bot/" + userId + "/images/";

        List<CompletableFuture<String>> futures = images.stream()
                .filter(file -> !file.isEmpty())
                .map(file -> CompletableFuture.supplyAsync(() -> {
                    try {
                        String publicId = UUID.randomUUID().toString();
                        Map<String, Object> options = new HashMap<>();
                        options.put("resource_type", "image");
                        options.put("folder", folder);
                        options.put("public_id", publicId);
                        options.put("overwrite", true);

                        @SuppressWarnings("unchecked")
                        Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(), options);
                        return (String) uploadResult.get("secure_url");
                    } catch (IOException e) {
                        throw new AppException(ErrorCode.IO_EXCEPTION, "Không thể tải ảnh lên Cloudinary: " + e.getMessage());
                    }
                }))
                .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .toList());
    }

    public List<ChatMessageResponse> getUserChatHistory() {
        Long userId = jwtUtil.getDataFromAuth().userId();
        return chatMessageRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toResponse).toList();
    }

    private ChatMessageResponse toResponse(ChatMessage chatMessage) {
        return ChatMessageResponse.builder()
                .id(chatMessage.getId())
                .message(chatMessage.getMessage())
                .senderType(chatMessage.getSenderType().toString())
                .images(chatMessage.getMediaList().stream().map(ChatMessageMedia::getUrl).toList())
                .createdAt(chatMessage.getCreatedAt())
                .build();
    }
}
