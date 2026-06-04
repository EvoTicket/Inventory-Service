package com.capstone.inventoryservice.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Tracks each Knowledge Base item ingested into the VectorStore.
 * The `source` field acts as a unique namespace slug (e.g. "fee_policy").
 * When re-ingesting the same source, all old vector chunks are deleted first → no conflict.
 */
@Entity
@Table(name = "kb_item", schema = "inventory_service")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KbItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique slug used to namespace vector chunks. Example: "fee_policy", "checkin_guide" */
    @Column(nullable = false, unique = true, length = 100)
    private String source;

    /** Display name shown in the admin UI */
    @Column(nullable = false, length = 255)
    private String title;

    /** Category grouping, e.g. "Policy", "FAQ", "Guide", "Payment" */
    @Column(length = 100)
    private String category;

    /** Original filename uploaded */
    @Column(length = 255)
    private String filename;

    /** Number of vector chunks stored in VectorStore */
    @Column(nullable = false)
    private Integer chunkCount;

    /** "Published" or "Draft" */
    @Column(nullable = false, length = 50)
    private String status;

    /** Admin username who last updated this item */
    @Column(length = 100)
    private String updatedBy;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * The IDs of all vector_store documents belonging to this source.
     * Stored as a serialized JSON list to enable precise deletion.
     */
    @Column(columnDefinition = "TEXT")
    private String documentIds;
}
