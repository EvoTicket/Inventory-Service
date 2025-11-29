package com.capstone.inventoryservice.model.repository;

import com.capstone.inventoryservice.model.entity.ChatMessage;
import com.capstone.inventoryservice.model.entity.ChatMessageMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageMediaRepository  extends JpaRepository<ChatMessageMedia, Long> {
    List<ChatMessageMedia> findAllByChatMessage(ChatMessage chatMessage);
}
