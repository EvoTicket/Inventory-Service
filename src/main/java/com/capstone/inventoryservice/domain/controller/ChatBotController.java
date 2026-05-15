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
import reactor.core.publisher.Flux;

import java.util.List;

@RequestMapping("/api/chatbot")
@RestController
@RequiredArgsConstructor
public class ChatBotController {
    private final ChatBotService chatBotService;

    @PostMapping(value = "/ask", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> smartChat(
            @RequestParam String question,

            @Parameter(
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)
            )
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            @RequestParam(value = "useRag", required = false, defaultValue = "false") boolean useRag
    ) {
        return chatBotService.chat(question, files, useRag);
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
