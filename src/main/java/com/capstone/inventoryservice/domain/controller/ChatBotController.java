package com.capstone.inventoryservice.domain.controller;

import com.capstone.inventoryservice.domain.dto.BaseResponse;
import com.capstone.inventoryservice.domain.dto.response.ChatBotResponse;
import com.capstone.inventoryservice.domain.service.chatbot.ChatBotService;
import com.capstone.inventoryservice.domain.service.chatbot.SqlExecutorService;
import com.capstone.inventoryservice.domain.service.chatbot.SqlGeneratorService;
import com.capstone.inventoryservice.security.JwtUtil;
import com.capstone.inventoryservice.model.entity.ChatConversation;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.PrintWriter;
import java.util.List;

@Slf4j
@RequestMapping("/api/chatbot")
@RestController
@RequiredArgsConstructor
public class ChatBotController {
    private final ChatBotService chatBotService;
    private final SqlGeneratorService sqlGeneratorService;
    private final SqlExecutorService sqlExecutorService;
    private final JwtUtil jwtUtil;

    @PostMapping(value = "/ask")
    public ResponseEntity<BaseResponse<ChatBotResponse>> smartChat(
            @RequestParam String question,
            @RequestParam(value = "conversationId", required = false) String conversationId
    ) {
        String answer = chatBotService.chat(conversationId, question);
        BaseResponse<ChatBotResponse> response = BaseResponse.ok(
                ChatBotResponse.builder()
                        .answer(answer)
                        .build()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/stream-ask")
    public void streamChat(
            @RequestParam String question,
            @RequestParam(value = "conversationId", required = false) String conversationId,
            HttpServletResponse response
    ) {
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        response.setHeader("X-Accel-Buffering", "no");

        try {
            PrintWriter writer = response.getWriter();
            chatBotService.chatStream(conversationId, question)
                    .doOnNext(answerPart -> {
                        if (answerPart != null) {
                            String[] lines = answerPart.split("\n", -1);
                            for (String line : lines) {
                                writer.write("data: " + line + "\n");
                            }
                            writer.write("\n");
                            writer.flush();
                        }
                    })
                    .doOnError(error -> {
                        log.error("Error streaming chatbot response", error);
                    })
                    .doOnComplete(() -> {
                        try {
                            writer.close();
                        } catch (Exception e) {
                            // ignore
                        }
                    })
                    .blockLast();
        } catch (Exception e) {
            log.error("Failed to stream chat", e);
        }
    }

    @GetMapping("/history")
    public ResponseEntity<BaseResponse<List<Message>>> chatMessages(
            @RequestParam(value = "conversationId", required = false) String conversationId
    ){
        return ResponseEntity.ok(BaseResponse.ok(chatBotService.getChatMessages(conversationId)));
    }

    @PostMapping("/clear-history")
    public ResponseEntity<BaseResponse<Boolean>> clearHistory(
            @RequestParam(value = "conversationId", required = false) String conversationId
    ) {
        chatBotService.clearChatHistory(conversationId);
        return ResponseEntity.ok(BaseResponse.ok(true));
    }

    @GetMapping("/conversations")
    public ResponseEntity<BaseResponse<List<ChatConversation>>> getConversations() {
        Long userId = jwtUtil.getDataFromAuth().userId();
        return ResponseEntity.ok(BaseResponse.ok(chatBotService.listConversations(userId)));
    }

    @PostMapping("/conversations")
    public ResponseEntity<BaseResponse<ChatConversation>> createNewConversation() {
        Long userId = jwtUtil.getDataFromAuth().userId();
        return ResponseEntity.ok(BaseResponse.ok(chatBotService.createConversation(userId)));
    }

    @DeleteMapping("/conversations/{id}")
    public ResponseEntity<BaseResponse<Boolean>> deleteConversation(@PathVariable String id) {
        Long userId = jwtUtil.getDataFromAuth().userId();
        chatBotService.deleteConversation(userId, id);
        return ResponseEntity.ok(BaseResponse.ok(true));
    }

    @PatchMapping("/conversations/{id}/title")
    public ResponseEntity<BaseResponse<ChatConversation>> renameConversation(
            @PathVariable String id,
            @RequestParam String title
    ) {
        Long userId = jwtUtil.getDataFromAuth().userId();
        return ResponseEntity.ok(BaseResponse.ok(chatBotService.renameConversation(userId, id, title)));
    }

    @PostMapping("/query")
    public ResponseEntity<BaseResponse<List<?>>> executeSqlQuery(
            @RequestParam String question
    ) {
        String sqlQuery = sqlGeneratorService.generate(question);
        List<?> results = sqlExecutorService.execute(sqlQuery);
        return ResponseEntity.ok(BaseResponse.ok(results));
    }
}
