package se.westcoastcode.features;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.UUIDComparator;
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;

import java.util.UUID;

public final class UUIDFeatures {
    private static final TimeBasedEpochGenerator GENERATOR = Generators.timeBasedEpochGenerator();
    private static final UUIDComparator COMPARATOR = new UUIDComparator();

    /**
     * Create a new uuid
     *
     * @return A new v7 uuid
     */
    public static UUID newUUID() {
        return GENERATOR.generate();
    }

    /**
     * Parse a string into a UUID
     *
     * @param uuid The uuid
     * @return The uuid
     */
    public static UUID parseUUID(final String uuid) {
        return UUID.fromString(uuid);
    }

    /**
     * Compare two uuids
     *
     * @param uuid1 The first UUID
     * @param uuid2 The second UUID
     * @return true if both UUIDS are identical
     */
    public static boolean compareUUID(final UUID uuid1, final UUID uuid2) {
        return COMPARATOR.compare(uuid1, uuid2) == 0;
    }
}
