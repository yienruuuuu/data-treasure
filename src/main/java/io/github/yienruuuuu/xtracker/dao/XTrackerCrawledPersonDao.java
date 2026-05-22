package io.github.yienruuuuu.xtracker.dao;

import io.github.yienruuuuu.xtracker.bean.po.XTrackerCrawledPersonEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface XTrackerCrawledPersonDao extends JpaRepository<XTrackerCrawledPersonEntity, UUID> {

    Optional<XTrackerCrawledPersonEntity> findByPlatformAndHandle(String platform, String handle);

    List<XTrackerCrawledPersonEntity> findByPlatform(String platform);
}
