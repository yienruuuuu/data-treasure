package io.github.yienruuuuu.xtracker.dao;

import io.github.yienruuuuu.xtracker.bean.po.XTrackerRawApiSnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface XTrackerRawApiSnapshotDao extends JpaRepository<XTrackerRawApiSnapshotEntity, UUID> {

    Optional<XTrackerRawApiSnapshotEntity> findTopBySourceEndpointAndSourceObjectTypeAndSourceObjectIdOrderByFetchedAtDesc(
            String sourceEndpoint,
            String sourceObjectType,
            String sourceObjectId
    );
}
