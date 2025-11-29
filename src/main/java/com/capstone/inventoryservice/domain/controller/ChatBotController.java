package com.capstone.inventoryservice.domain.controller;

import com.capstone.inventoryservice.domain.dto.BaseResponse;
import com.capstone.inventoryservice.domain.dto.response.ChatBotResponse;
import com.capstone.inventoryservice.domain.dto.response.ChatMessageResponse;
import com.capstone.inventoryservice.domain.service.ChatBotService;
import com.capstone.inventoryservice.domain.service.ChatMessageService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


@RequestMapping("/api/chatbot")
@RestController
@RequiredArgsConstructor
public class ChatBotController {
    private final ChatBotService chatBotService;
    private final ChatMessageService chatMessageService;

    @PostMapping(value = "/ask", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BaseResponse<ChatBotResponse>> smartChat(
            @RequestParam String question,

            @Parameter(
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)
            )
            @RequestParam(value = "files", required = false) List<MultipartFile> files
    ) {
        String answer = chatBotService.chatWithSmartQuery(question, files);
        BaseResponse<ChatBotResponse> response = BaseResponse.ok(
                ChatBotResponse.builder()
                .answer(answer)
                .build()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    public ResponseEntity<BaseResponse<List<ChatMessageResponse>>> chatMessages(){
        return ResponseEntity.ok(BaseResponse.ok(chatMessageService.getUserChatHistory()));
    }
}
