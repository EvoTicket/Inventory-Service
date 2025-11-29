package com.capstone.inventoryservice.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "chat_message_media")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_message_id", nullable = false)
    private ChatMessage chatMessage;

    @Column(name = "url", nullable = false, columnDefinition = "TEXT")
    private String url;
}