package com.capstone.inventoryservice.domain.service.chatbot;

import com.capstone.inventoryservice.domain.dto.response.KbItemResponse;
import com.capstone.inventoryservice.exception.AppException;
import com.capstone.inventoryservice.exception.ErrorCode;
import com.capstone.inventoryservice.model.entity.KbItem;
import com.capstone.inventoryservice.model.repository.KbItemRepository;
import com.capstone.inventoryservice.security.JwtUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Manages the Knowledge Base (KB) lifecycle for the AI chatbot.
 *
 * <p><b>Conflict resolution strategy</b>: Each KB item is identified by a unique {@code source} slug
 * (e.g. {@code "fee_policy"}). Every vector chunk ingested is tagged with this source in its metadata.
 * When re-uploading a file for the same source, we:
 * <ol>
 *   <li>Delete all old vector chunks by their stored document IDs</li>
 *   <li>Ingest the new file's chunks with the same source tag</li>
 *   <li>Update the {@link KbItem} registry entry</li>
 * </ol>
 * This guarantees the AI never sees conflicting versions of the same policy.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KbItemService {

    private final VectorStore vectorStore;
    private final KbItemRepository kbItemRepository;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    // =========================================================
    // List KB Items
    // =========================================================

    public List<KbItemResponse> listKbItems() {
        return kbItemRepository.findAllByOrderByUpdatedAtDesc()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public KbItemResponse getKbItem(String source) {
        KbItem item = kbItemRepository.findBySource(source)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "KB item not found: " + source));
        return toResponse(item);
    }

    // =========================================================
    // Ingest — Upload & Replace
    // =========================================================

    /**
     * Ingests a file into the VectorStore under the given {@code source} namespace.
     * <p>
     * If a KB item with this source already exists, all its old vector chunks are deleted first
     * before new chunks are added — preventing any knowledge conflict.
     *
     * @param file     the uploaded file (PDF, DOCX, TXT, MD, etc.)
     * @param source   unique slug identifying this KB item (e.g. "fee_policy")
     * @param title    display title shown in admin UI
     * @param category grouping category (e.g. "Policy", "FAQ", "Guide")
     * @param status   "Published" or "Draft"
     * @return the updated/created KbItemResponse
     */
    public KbItemResponse ingestFile(MultipartFile file, String source, String title,
                                     String category, String status) {
        String actor = resolveActor();
        log.info("[KB] Starting ingest: source='{}', file='{}', actor='{}'",
                source, file.getOriginalFilename(), actor);

        // Step 1: Delete existing chunks for this source (if any) — conflict prevention
        Optional<KbItem> existing = kbItemRepository.findBySource(source);
        if (existing.isPresent()) {
            deleteVectorChunks(existing.get());
            log.info("[KB] Deleted old chunks for source='{}' before re-ingesting", source);
        }

        // Step 2: Read & split the new file
        Resource resource = buildResource(file);
        TextSplitter splitter = new TokenTextSplitter();
        List<Document> docs;
        try {
            TikaDocumentReader reader = new TikaDocumentReader(resource);
            docs = splitter.split(reader.read());
        } catch (Exception e) {
            log.error("[KB] Failed to read file '{}': {}", file.getOriginalFilename(), e.getMessage(), e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "Cannot parse file: " + e.getMessage());
        }

        if (docs.isEmpty()) {
            throw new AppException(ErrorCode.BAD_REQUEST, "File rỗng hoặc không đọc được nội dung");
        }

        // Step 3: Tag each chunk with source metadata
        docs.forEach(doc -> {
            doc.getMetadata().put("source", source);
            doc.getMetadata().put("title", title);
            doc.getMetadata().put("category", category);
            doc.getMetadata().put("filename", file.getOriginalFilename());
        });

        // Step 4: Ingest into VectorStore
        vectorStore.accept(docs);
        log.info("[KB] Ingested {} chunks for source='{}'", docs.size(), source);

        // Step 5: Collect document IDs for future deletion
        List<String> docIds = docs.stream()
                .map(Document::getId)
                .collect(Collectors.toList());

        // Step 6: Save/update KbItem registry
        KbItem item = existing.map(e -> {
            e.setTitle(title);
            e.setCategory(category);
            e.setFilename(file.getOriginalFilename());
            e.setChunkCount(docs.size());
            e.setStatus(status != null ? status : "Published");
            e.setUpdatedBy(actor);
            e.setUpdatedAt(LocalDateTime.now());
            e.setDocumentIds(serializeIds(docIds));
            return e;
        }).orElseGet(() -> KbItem.builder()
                .source(source)
                .title(title)
                .category(category)
                .filename(file.getOriginalFilename())
                .chunkCount(docs.size())
                .status(status != null ? status : "Published")
                .updatedBy(actor)
                .updatedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .documentIds(serializeIds(docIds))
                .build());

        kbItemRepository.save(item);
        log.info("[KB] Saved KbItem registry entry for source='{}'", source);
        return toResponse(item);
    }

    // =========================================================
    // Delete KB Item
    // =========================================================

    /**
     * Deletes a KB item and all its associated vector chunks from the VectorStore.
     *
     * @param source the source slug of the KB item to delete
     */
    public void deleteKbItem(String source) {
        KbItem item = kbItemRepository.findBySource(source)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "KB item not found: " + source));

        // Remove chunks from VectorStore
        deleteVectorChunks(item);

        // Remove registry entry
        kbItemRepository.delete(item);
        log.info("[KB] Deleted KB item and vector chunks for source='{}'", source);
    }

    // =========================================================
    // Update Status (Published / Draft)
    // =========================================================

    public KbItemResponse updateStatus(String source, String newStatus) {
        KbItem item = kbItemRepository.findBySource(source)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "KB item not found: " + source));
        item.setStatus(newStatus);
        item.setUpdatedBy(resolveActor());
        item.setUpdatedAt(LocalDateTime.now());
        kbItemRepository.save(item);
        return toResponse(item);
    }

    // =========================================================
    // Internal helpers
    // =========================================================

    private void deleteVectorChunks(KbItem item) {
        if (item.getDocumentIds() == null || item.getDocumentIds().isBlank()) return;
        try {
            List<String> ids = objectMapper.readValue(item.getDocumentIds(), new TypeReference<>() {});
            if (!ids.isEmpty()) {
                vectorStore.delete(ids);
                log.info("[KB] Deleted {} vector chunks for source='{}'", ids.size(), item.getSource());
            }
        } catch (Exception e) {
            log.error("[KB] Failed to delete vector chunks for source='{}': {}", item.getSource(), e.getMessage(), e);
        }
    }

    private Resource buildResource(MultipartFile file) {
        try {
            return new InputStreamResource(file.getInputStream()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };
        } catch (Exception e) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "Cannot read file: " + e.getMessage());
        }
    }

    private String serializeIds(List<String> ids) {
        try {
            return objectMapper.writeValueAsString(ids);
        } catch (Exception e) {
            log.error("[KB] Failed to serialize document IDs", e);
            return "[]";
        }
    }

    private String resolveActor() {
        try {
            var data = jwtUtil.getDataFromAuth();
            return data != null && data.userId() != null ? "@admin#" + data.userId() : "@system";
        } catch (Exception e) {
            return "@system";
        }
    }

    private KbItemResponse toResponse(KbItem item) {
        return KbItemResponse.builder()
                .id(item.getId())
                .source(item.getSource())
                .title(item.getTitle())
                .category(item.getCategory())
                .filename(item.getFilename())
                .chunkCount(item.getChunkCount())
                .status(item.getStatus())
                .updatedBy(item.getUpdatedBy())
                .updatedAt(item.getUpdatedAt())
                .createdAt(item.getCreatedAt())
                .build();
    }
}
