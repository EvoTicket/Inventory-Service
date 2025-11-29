package com.capstone.inventoryservice.domain.dto.response;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageResponse {
    private Long id;
    private String message;
    private List<String> images;
    private String senderType;
    private OffsetDateTime createdAt;
}