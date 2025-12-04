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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final Cloudinary cloudinary;
    private final JwtUtil jwtUtil;

    @Transactional
    public void saveUserMessage(Long userId,
                                String message,
                                List<MultipartFile> images) {
        List<String> imagesStr;
        try {
            imagesStr = uploadImages(images, userId);
        } catch (IOException ex){
            throw new AppException(ErrorCode.IO_EXCEPTION, "Không thể tải ảnh lên Cloudinary: " + ex.getMessage());
        }
        ChatMessage chatMessage = ChatMessage.builder()
                .userId(userId)
                .message(message)
                .senderType(SenderType.USER)
                .build();

        if (imagesStr != null && !imagesStr.isEmpty()) {
            for (String url : imagesStr) {
                ChatMessageMedia media = ChatMessageMedia.builder()
                        .chatMessage(chatMessage)
                        .url(url)
                        .build();
                chatMessage.getMediaList().add(media);
            }
        }
        chatMessageRepository.save(chatMessage);
    }

    @Transactional
    public void saveAssistantMessage(Long userId,
                                     String message) {
        chatMessageRepository.save(ChatMessage.builder()
                .userId(userId)
                .message(message)
                .senderType(SenderType.ASSISTANT)
                .build());
    }

    @SuppressWarnings("unchecked")
    public List<String> uploadImages(List<MultipartFile> images, Long userId) throws IOException {
        if (images == null || images.isEmpty()) {
            return new ArrayList<>();
        }

        List<Map<String, Object>> uploadResults = new ArrayList<>();
        String folder = "chat-bot/" + userId + "/images/";

        for (MultipartFile file : images) {
            if (file.isEmpty()) {
                continue;
            }

            String publicId = UUID.randomUUID().toString();

            Map<String, Object> options = new HashMap<>();
            options.put("resource_type", "image");
            options.put("folder", folder);
            options.put("public_id", publicId);
            options.put("overwrite", true);

            Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(), options);
            uploadResults.add(uploadResult);
        }

        return uploadResults.stream()
                .map(result -> (String) result.get("secure_url"))
                .toList();
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
