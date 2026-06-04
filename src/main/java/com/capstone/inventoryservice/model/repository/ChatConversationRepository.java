package com.capstone.inventoryservice.model.repository;

import com.capstone.inventoryservice.model.entity.ChatConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ChatConversationRepository extends JpaRepository<ChatConversation, String> {
    List<ChatConversation> findByUserIdOrderByUpdatedAtDesc(Long userId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM chat_history WHERE conversation_id = :conversationId", nativeQuery = true)
    void deleteMessagesByConversationId(@Param("conversationId") String conversationId);
}
