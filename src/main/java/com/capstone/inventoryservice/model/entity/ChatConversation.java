package com.capstone.inventoryservice.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_conversation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatConversation {
    @Id
    private String id; // UUID string

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
