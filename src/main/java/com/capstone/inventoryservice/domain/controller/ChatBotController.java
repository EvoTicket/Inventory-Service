package com.capstone.inventoryservice.domain.controller;

import com.capstone.inventoryservice.domain.dto.BaseResponse;
import com.capstone.inventoryservice.domain.dto.response.ChatBotResponse;
import com.capstone.inventoryservice.domain.service.chatbot.ChatBotService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    @PostMapping(value = "/ask", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BaseResponse<ChatBotResponse>> smartChat(
            @RequestParam String question,

            @Parameter(
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)
            )
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            @RequestParam(value = "useRag", required = false, defaultValue = "false") boolean useRag
    ) {
        String answer = chatBotService.chat(question, files, useRag);
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

            @Parameter(
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)
            )
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            @RequestParam(value = "useRag", required = false, defaultValue = "false") boolean useRag,
            HttpServletResponse response
    ) {
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        response.setHeader("X-Accel-Buffering", "no");

        try {
            PrintWriter writer = response.getWriter();
            chatBotService.chatStream(question, files, useRag)
                    .doOnNext(answerPart -> {
                        writer.write("data:" + answerPart + "\n\n");
                        writer.flush();
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
    public ResponseEntity<BaseResponse<List<Message>>> chatMessages(){
        return ResponseEntity.ok(BaseResponse.ok(chatBotService.getChatMessages()));
    }

    @PostMapping("/clear-history")
    public ResponseEntity<BaseResponse<Boolean>> clearHistory() {
        chatBotService.clearChatHistory();
        return ResponseEntity.ok(BaseResponse.ok(true));
    }
}
