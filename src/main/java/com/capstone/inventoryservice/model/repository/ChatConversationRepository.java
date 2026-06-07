package com.capstone.inventoryservice.model.repository;

import com.capstone.inventoryservice.model.entity.ChatConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatConversationRepository extends JpaRepository<ChatConversation, String> {
    List<ChatConversation> findByUserIdOrderByUpdatedAtDesc(Long userId);
}
