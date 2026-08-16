package dev.worldgenerator.biome;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ClimateRegionSamplerTest {
    @Test
    void climateIsDeterministic() {
        ClimateRegionSampler first = new ClimateRegionSampler(123L);
        ClimateRegionSampler second = new ClimateRegionSampler(123L);
        assertEquals(first.sample(900, -1700), second.sample(900, -1700));
    }

    @Test
    void climateRegionsChangeWithinExplorationDistance() {
        ClimateRegionSampler sampler = new ClimateRegionSampler(123L);
        Set<Long> regions = new HashSet<>();
        for (int x = -6_000; x <= 6_000; x += 300) {
            regions.add(sampler.sample(x, 0).regionId());
        }
        assertTrue(regions.size() >= 8, "expected bounded climate regions, got " + regions.size());
    }

    @Test
    void differentSeedsProduceDifferentClimate() {
        assertNotEquals(
                new ClimateRegionSampler(1L).sample(500, 500),
                new ClimateRegionSampler(2L).sample(500, 500));
    }

    @Test
    void individualClimateRegionsStayWithinLocalExplorationScale() {
        for (long seed : new long[] {1L, 123L, 12_345L}) {
            ClimateRegionSampler sampler = new ClimateRegionSampler(seed);
            long previous = Long.MIN_VALUE;
            int run = 0;
            int longest = 0;
            for (int x = -12_000; x <= 12_000; x += 100) {
                long current = sampler.sample(x, 2_137).regionId();
                run = current == previous ? run + 1 : 1;
                longest = Math.max(longest, run);
                previous = current;
            }
            assertTrue(longest * 100 <= 1_600,
                    "climate region exceeded 1600 blocks for seed " + seed);
        }
    }

    @Test
    void climateValuesDoNotJumpAtRegionBorders() {
        ClimateRegionSampler sampler = new ClimateRegionSampler(12_345L);
        ClimateSample previous = sampler.sample(-6_000, 317);
        double largestJump = 0.0;
        for (int x = -5_999; x <= 6_000; x++) {
            ClimateSample current = sampler.sample(x, 317);
            largestJump = Math.max(largestJump,
                    Math.max(Math.abs(current.temperature() - previous.temperature()),
                            Math.abs(current.humidity() - previous.humidity())));
            previous = current;
        }
        assertTrue(largestJump < 0.035,
                "adjacent surface climate jumps by " + largestJump);
    }
}
