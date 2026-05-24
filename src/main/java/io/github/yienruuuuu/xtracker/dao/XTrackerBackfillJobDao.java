package io.github.yienruuuuu.xtracker.dao;

import io.github.yienruuuuu.xtracker.bean.po.XTrackerBackfillJobEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface XTrackerBackfillJobDao extends JpaRepository<XTrackerBackfillJobEntity, UUID> {
}
