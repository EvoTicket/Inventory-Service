package com.capstone.inventoryservice.domain.controller;

import com.capstone.inventoryservice.domain.dto.BaseResponse;
import com.capstone.inventoryservice.domain.dto.response.KbItemResponse;
import com.capstone.inventoryservice.domain.service.chatbot.KbItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Admin-only endpoints for managing the AI chatbot Knowledge Base (KB).
 *
 * <p>Security is enforced at URL level via SecurityConfig: /api/chatbot/kb/** → ROLE_ADMIN.
 */
@Slf4j
@Tag(name = "AI Knowledge Base (Admin)", description = "Manage chatbot knowledge items — ingest, list, delete")
@RequestMapping("/api/chatbot/kb")
@RestController
@RequiredArgsConstructor
public class KbItemController {

    private final KbItemService kbItemService;

    // =========================================================
    // GET /api/chatbot/kb/items
    // =========================================================

    @Operation(summary = "List all KB items", description = "Returns all ingested KB items ordered by last updated")
    @GetMapping("/items")
    public ResponseEntity<BaseResponse<List<KbItemResponse>>> listKbItems() {
        return ResponseEntity.ok(BaseResponse.ok(kbItemService.listKbItems()));
    }

    // =========================================================
    // GET /api/chatbot/kb/items/{source}
    // =========================================================

    @Operation(summary = "Get KB item by source", description = "Returns a single KB item by its source slug")
    @GetMapping("/items/{source}")
    public ResponseEntity<BaseResponse<KbItemResponse>> getKbItem(@PathVariable String source) {
        return ResponseEntity.ok(BaseResponse.ok(kbItemService.getKbItem(source)));
    }

    // =========================================================
    // POST /api/chatbot/kb/ingest
    // =========================================================

    @Operation(
        summary = "Upload & ingest a file into KB",
        description = """
            Ingests a file (PDF, DOCX, TXT, MD) into the AI vector store under the given source slug.
            If a KB item with this source already exists, all its old chunks are deleted first
            to prevent knowledge conflicts (e.g. fee policy 2% → 5%).
            """
    )
    @PostMapping(value = "/ingest", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BaseResponse<KbItemResponse>> ingestFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("source") String source,
            @RequestParam("title") String title,
            @RequestParam(value = "category", defaultValue = "General") String category,
            @RequestParam(value = "status", defaultValue = "Published") String status
    ) {
        log.info("[KbController] Ingest request: source='{}', file='{}'", source, file.getOriginalFilename());
        KbItemResponse result = kbItemService.ingestFile(file, source, title, category, status);
        return ResponseEntity.ok(BaseResponse.created(
                "Đã nạp " + result.getChunkCount() + " chunks thành công", result));
    }

    // =========================================================
    // DELETE /api/chatbot/kb/items/{source}
    // =========================================================

    @Operation(summary = "Delete KB item", description = "Removes a KB item and all its associated vector chunks")
    @DeleteMapping("/items/{source}")
    public ResponseEntity<BaseResponse<Void>> deleteKbItem(@PathVariable String source) {
        kbItemService.deleteKbItem(source);
        return ResponseEntity.ok(BaseResponse.noContent("KB item '" + source + "' đã được xóa"));
    }

    // =========================================================
    // PATCH /api/chatbot/kb/items/{source}/status
    // =========================================================

    @Operation(summary = "Update KB item status", description = "Toggle Published/Draft status of a KB item")
    @PatchMapping("/items/{source}/status")
    public ResponseEntity<BaseResponse<KbItemResponse>> updateStatus(
            @PathVariable String source,
            @RequestBody Map<String, String> body
    ) {
        String newStatus = body.getOrDefault("status", "Published");
        return ResponseEntity.ok(BaseResponse.ok(kbItemService.updateStatus(source, newStatus)));
    }

    // =========================================================
    // GET /api/chatbot/kb/exists/{source}  (used by FE to check conflicts)
    // =========================================================

    @Operation(summary = "Check if source exists", description = "Used by frontend to show replace-warning before upload")
    @GetMapping("/exists/{source}")
    public ResponseEntity<BaseResponse<Boolean>> checkSourceExists(@PathVariable String source) {
        boolean exists = kbItemService.listKbItems().stream()
                .anyMatch(item -> item.getSource().equals(source));
        return ResponseEntity.ok(BaseResponse.ok(exists));
    }
}
