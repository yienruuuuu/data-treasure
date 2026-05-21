package io.github.yienruuuuu.scheduler.dao;

import io.github.yienruuuuu.scheduler.bean.po.ArenaTextOverallItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ArenaTextOverallItemDao extends JpaRepository<ArenaTextOverallItemEntity, UUID> {
}
