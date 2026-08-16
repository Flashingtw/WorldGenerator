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
}
