package io.github.yienruuuuu.scheduler.bean.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "arena_text_overall_item")
public class ArenaTextOverallItemEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snapshot_id", nullable = false)
    private ArenaTextOverallSnapshotEntity snapshot;

    @Column(name = "rank", nullable = false)
    private int rank;

    @Column(name = "rank_spread_min")
    private Integer rankSpreadMin;

    @Column(name = "rank_spread_max")
    private Integer rankSpreadMax;

    @Column(name = "model_name", nullable = false, length = 255)
    private String modelName;

    @Column(name = "provider_name", length = 255)
    private String providerName;

    @Column(name = "license_type", length = 255)
    private String licenseType;

    @Column(name = "score", nullable = false)
    private int score;

    @Column(name = "score_ci")
    private Integer scoreCi;

    @Column(name = "model_votes")
    private Integer modelVotes;

    @Column(name = "input_price_per_m", precision = 12, scale = 4)
    private BigDecimal inputPricePerM;

    @Column(name = "output_price_per_m", precision = 12, scale = 4)
    private BigDecimal outputPricePerM;

    @Column(name = "context_length_text", length = 64)
    private String contextLengthText;

    @Column(name = "is_preliminary", nullable = false)
    private boolean preliminary;

    @Column(name = "model_url")
    private String modelUrl;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
