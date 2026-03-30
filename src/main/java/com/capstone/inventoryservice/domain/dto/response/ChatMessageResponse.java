package com.capstone.inventoryservice.domain.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageResponse {
    private String id;
    private String message;
    private List<String> images;
    private String senderType;
    private LocalDateTime createdAt;
}