package com.capstone.inventoryservice.model.entity;

import com.capstone.inventoryservice.model.enums.SenderType;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chat_message")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 50)
    private Long userId;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false, length = 10)
    private SenderType senderType;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "chatMessage", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ChatMessageMedia> mediaList = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now(ZoneId.systemDefault());
        }
    }
}
