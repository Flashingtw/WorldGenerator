package dev.worldgenerator.biome;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BiomeSamplerTest {
    @Test
    void seed123ContainsVariedBiomes() {
        BiomeSampler sampler = new BiomeSampler(123L);
        Set<BiomeKind> biomes = EnumSet.noneOf(BiomeKind.class);
        for (int x = -8_000; x <= 8_000; x += 400) {
            for (int z = -8_000; z <= 8_000; z += 400) {
                biomes.add(sampler.sample(x, 70, z));
            }
        }
        assertTrue(biomes.size() >= 12, "expected biome variety, got " + biomes);
    }

    @Test
    void noSingleBiomeOccupiesMostOfALongTransect() {
        BiomeSampler sampler = new BiomeSampler(123L);
        BiomeKind previous = null;
        int run = 0;
        int longestRun = 0;
        for (int x = -12_000; x <= 12_000; x += 100) {
            BiomeKind current = sampler.sample(x, 70, 10_000);
            run = current == previous ? run + 1 : 1;
            longestRun = Math.max(longestRun, run);
            previous = current;
        }
        assertTrue(longestRun * 100 <= 4_000, "single biome run was " + longestRun * 100 + " blocks");
    }
}
