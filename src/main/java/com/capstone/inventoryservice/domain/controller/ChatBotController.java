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
import org.springframework.http.codec.multipart.FilePart;
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
    public ResponseEntity<Flux<String>> smartChat(
            @RequestPart
            @Parameter(description = "Câu hỏi của người dùng")
            String question,

            @RequestPart(required = false)
            @Parameter(description = "Tệp đính kèm", content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE))
            List<FilePart> files
    ) {
        Flux<String> chatFlux = chatBotService.chat(question, files, false);

        // Tạo một đoạn padding (comment SSE) khoảng 2KB để "phá" bộ đệm của Proxy (Nginx/Cloudflare)
        // Dấu ":" ở đầu dòng là comment trong SSE, trình duyệt sẽ bỏ qua nội dung này.
        String padding = ":" + " ".repeat(2048) + "\n\n";
        Flux<String> paddedFlux = Flux.concat(Flux.just(padding), chatFlux);

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .header("X-Accel-Buffering", "no")
                .header("Cache-Control", "no-cache, no-transform")
                .header("Connection", "keep-alive")
                .body(paddedFlux);
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
