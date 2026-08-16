package dev.worldgenerator.terrain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WorldBoundsTest {
    @Test
    void finiteBoundsContainExactlyRequestedSquare() {
        WorldBounds bounds = new WorldBounds(5_000);
        assertTrue(bounds.contains(-2_500, -2_500));
        assertTrue(bounds.contains(2_499, 2_499));
        assertFalse(bounds.contains(2_500, 0));
        assertFalse(bounds.contains(0, -2_501));
    }

    @Test
    void unlimitedBoundsContainDistantCoordinates() {
        assertTrue(WorldBounds.UNLIMITED.contains(30_000_000, -30_000_000));
    }
}
