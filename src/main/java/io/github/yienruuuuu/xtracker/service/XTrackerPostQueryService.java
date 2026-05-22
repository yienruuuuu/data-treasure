package io.github.yienruuuuu.xtracker.service;

import io.github.yienruuuuu.common.error.SysCode;
import io.github.yienruuuuu.common.exception.BadRequestApiException;
import io.github.yienruuuuu.common.exception.NotFoundApiException;
import io.github.yienruuuuu.xtracker.bean.dto.XTrackerPostCountResponse;
import io.github.yienruuuuu.xtracker.bean.po.XTrackerCrawledPersonEntity;
import io.github.yienruuuuu.xtracker.dao.XTrackerCrawledPersonDao;
import io.github.yienruuuuu.xtracker.dao.XTrackerCrawledPostDao;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class XTrackerPostQueryService {

    private static final String DEFAULT_PLATFORM = "X";

    private final XTrackerCrawledPersonDao personDao;
    private final XTrackerCrawledPostDao postDao;

    public XTrackerPostQueryService(XTrackerCrawledPersonDao personDao, XTrackerCrawledPostDao postDao) {
        this.personDao = personDao;
        this.postDao = postDao;
    }

    public XTrackerPostCountResponse countPosts(String platform, String handle, Instant startAt, Instant endAt) {
        String normalizedPlatform = normalizePlatform(platform);
        String normalizedHandle = normalizeHandle(handle);
        if (startAt == null || endAt == null || !startAt.isBefore(endAt)) {
            throw new BadRequestApiException(SysCode.INVALID_ARGUMENT, "startAt must be before endAt");
        }
        XTrackerCrawledPersonEntity person = personDao.findByPlatformAndHandle(normalizedPlatform, normalizedHandle)
                .orElseThrow(() -> new NotFoundApiException(
                        SysCode.NOT_FOUND,
                        "XTracker person not found: " + normalizedPlatform + "/" + normalizedHandle
                ));
        long count = postDao.countByPersonIdAndPostedAtRange(person.getId(), startAt, endAt);
        return new XTrackerPostCountResponse(normalizedPlatform, normalizedHandle, startAt, endAt, count);
    }

    private String normalizePlatform(String platform) {
        if (platform == null || platform.isBlank()) {
            return DEFAULT_PLATFORM;
        }
        return platform.trim().toUpperCase();
    }

    private String normalizeHandle(String handle) {
        if (handle == null || handle.isBlank()) {
            throw new BadRequestApiException(SysCode.INVALID_ARGUMENT, "XTracker handle is required");
        }
        return handle.trim().replaceFirst("^@", "");
    }
}
