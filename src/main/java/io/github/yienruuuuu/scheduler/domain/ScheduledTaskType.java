package io.github.yienruuuuu.scheduler.domain;

import java.util.Arrays;
import java.util.Optional;

public enum ScheduledTaskType {
    DATA_RESEARCH;

    public static Optional<ScheduledTaskType> findByName(String name) {
        return Arrays.stream(values())
                .filter(type -> type.name().equals(name))
                .findFirst();
    }
}
