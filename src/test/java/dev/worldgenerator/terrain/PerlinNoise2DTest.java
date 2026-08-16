package dev.worldgenerator.terrain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PerlinNoise2DTest {
    @Test
    void gradientNoiseIsDeterministicSeededAndContinuous() {
        PerlinNoise2D first = new PerlinNoise2D(123L);
        PerlinNoise2D same = new PerlinNoise2D(123L);
        PerlinNoise2D different = new PerlinNoise2D(456L);
        assertEquals(first.sample(12.34, -56.78), same.sample(12.34, -56.78));
        assertNotEquals(first.sample(12.34, -56.78), different.sample(12.34, -56.78));

        double previous = first.sample(-4.0, 1.75);
        double largestStep = 0.0;
        for (int i = 1; i <= 8_000; i++) {
            double current = first.sample(-4.0 + i / 1_000.0, 1.75);
            largestStep = Math.max(largestStep, Math.abs(current - previous));
            previous = current;
        }
        assertTrue(largestStep < 0.003, "Perlin noise contains a visible step: " + largestStep);
    }
}
