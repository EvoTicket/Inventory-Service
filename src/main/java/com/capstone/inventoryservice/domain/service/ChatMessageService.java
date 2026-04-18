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
import org.jspecify.annotations.NonNull;
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

    public record ImageData(String filename, String contentType, byte[] bytes) {

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ImageData(String filename1, String type, byte[] bytes1))) return false;
            return filename.equals(filename1)
                    && contentType.equals(type)
                    && Arrays.equals(bytes, bytes1);
        }

        @Override
        public int hashCode() {
            int result = filename.hashCode();
            result = 31 * result + contentType.hashCode();
            result = 31 * result + Arrays.hashCode(bytes);
            return result;
        }

        @Override
        public @NonNull String toString() {
            return "ImageData[" +
                    "filename=" + filename +
                    ", contentType=" + contentType +
                    ", bytes=" + Arrays.toString(bytes) +
                    ']';
        }
    }

    public void saveUserMessage(Long userId,
                                String message,
                                List<MultipartFile> images) {
        List<ImageData> imageBytes = (images == null || images.isEmpty())
                ? List.of()
                : images.stream()
                .filter(f -> !f.isEmpty())
                .map(f -> {
                    try {
                        return new ImageData(f.getOriginalFilename(), f.getContentType(), f.getBytes());
                    } catch (IOException e) {
                        throw new AppException(ErrorCode.IO_EXCEPTION, "Không thể đọc file: " + e.getMessage());
                    }
                })
                .toList();

        List<ChatMessageMedia> mediaList = uploadImagesAsync(imageBytes, userId)
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

    public void saveAssistantMessage(Long userId,
                                     String message) {
        chatMessageRepository.save(ChatMessage.builder()
                .userId(userId)
                .message(message)
                .senderType(SenderType.ASSISTANT)
                .build());
    }

    public CompletableFuture<List<String>> uploadImagesAsync(List<ImageData> images, Long userId) {
        if (images == null || images.isEmpty()) {
            return CompletableFuture.completedFuture(new ArrayList<>());
        }

        String folder = "chat-bot/" + userId + "/images/";

        List<CompletableFuture<String>> futures = images.stream()
                .map(imageData -> CompletableFuture.supplyAsync(() -> {
                    try {
                        String publicId = UUID.randomUUID().toString();
                        Map<String, Object> options = new HashMap<>();
                        options.put("resource_type", "image");
                        options.put("folder", folder);
                        options.put("public_id", publicId);
                        options.put("overwrite", true);

                        @SuppressWarnings("unchecked")
                        Map<String, Object> uploadResult = cloudinary.uploader().upload(imageData.bytes(), options);
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
