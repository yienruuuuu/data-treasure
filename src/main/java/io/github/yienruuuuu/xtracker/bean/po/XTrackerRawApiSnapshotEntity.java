package io.github.yienruuuuu.xtracker.bean.po;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "xtracker_raw_api_snapshot")
public class XTrackerRawApiSnapshotEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "source_endpoint", nullable = false, length = 255)
    private String sourceEndpoint;

    @Column(name = "source_object_type", nullable = false, length = 64)
    private String sourceObjectType;

    @Column(name = "source_object_id", nullable = false, length = 255)
    private String sourceObjectId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_params")
    private JsonNode requestParams;

    @Column(name = "http_status", nullable = false)
    private int httpStatus;

    @Column(name = "response_hash", nullable = false, length = 64)
    private String responseHash;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_body", nullable = false)
    private JsonNode responseBody;

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (fetchedAt == null) {
            fetchedAt = now;
        }
        if (createdAt == null) {
            createdAt = now;
        }
    }
}
