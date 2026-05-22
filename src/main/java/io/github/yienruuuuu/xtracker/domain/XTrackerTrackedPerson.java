package io.github.yienruuuuu.xtracker.domain;

import java.util.Arrays;
import java.util.Optional;

/**
 * Enum-managed XTracker crawler targets used by manual and scheduled flows.
 */
public enum XTrackerTrackedPerson {
    ELON_MUSK("X", "elonmusk");

    private final String platform;
    private final String handle;

    XTrackerTrackedPerson(String platform, String handle) {
        this.platform = platform;
        this.handle = handle;
    }

    public String platform() {
        return platform;
    }

    public String handle() {
        return handle;
    }

    public static Optional<XTrackerTrackedPerson> findByName(String name) {
        return Arrays.stream(values())
                .filter(person -> person.name().equals(name))
                .findFirst();
    }
}
