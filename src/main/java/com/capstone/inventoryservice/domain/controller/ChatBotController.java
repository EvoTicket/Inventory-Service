package com.capstone.inventoryservice.domain.controller;

import com.capstone.inventoryservice.domain.dto.BaseResponse;
import com.capstone.inventoryservice.domain.dto.response.ChatBotResponse;
import com.capstone.inventoryservice.domain.service.ChatBotService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


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
            @RequestParam(value = "file", required = false) MultipartFile file
    ) {
        String answer = chatBotService.chatWithSmartQuery(question, file);
        BaseResponse<ChatBotResponse> response = BaseResponse.ok(
                ChatBotResponse.builder()
                .answer(answer)
                .build()
        );
        return ResponseEntity.ok(response);
    }
}
