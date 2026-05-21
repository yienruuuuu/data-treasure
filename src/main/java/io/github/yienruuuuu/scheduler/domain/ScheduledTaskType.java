package io.github.yienruuuuu.scheduler.domain;

import java.util.Arrays;
import java.util.Optional;

/**
 * Enum-managed task types supported by the scheduler.
 *
 * <p>The database stores {@link #name()} as a string so persisted rows remain
 * simple while Java code avoids hand-written task type constants.</p>
 */
public enum ScheduledTaskType {
    DATA_RESEARCH;

    public static Optional<ScheduledTaskType> findByName(String name) {
        return Arrays.stream(values())
                .filter(type -> type.name().equals(name))
                .findFirst();
    }
}
