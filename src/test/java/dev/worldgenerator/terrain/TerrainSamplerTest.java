package dev.worldgenerator.terrain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TerrainSamplerTest {
    @Test
    void sameSeedAndCoordinatesAreDeterministic() {
        TerrainSampler first = new TerrainSampler(12345L);
        TerrainSampler second = new TerrainSampler(12345L);
        assertEquals(first.sample(9281, -4412), second.sample(9281, -4412));
    }

    @Test
    void differentSeedsProduceDifferentTerrain() {
        TerrainSampler first = new TerrainSampler(1L);
        TerrainSampler second = new TerrainSampler(2L);
        assertNotEquals(first.sample(7000, 9000), second.sample(7000, 9000));
    }

    @Test
    void neighboringColumnsRemainReasonablyContinuous() {
        TerrainSampler sampler = new TerrainSampler(42L);
        for (int x = -500; x < 500; x++) {
            int difference = Math.abs(sampler.sample(x, 137).height() - sampler.sample(x + 1, 137).height());
            assertTrue(difference <= 8, "height jump at x=" + x + " was " + difference);
        }
    }

    @Test
    void terrainContainsBothOceanAndLandAtMacroScale() {
        TerrainSampler sampler = new TerrainSampler(918273645L);
        boolean ocean = false;
        boolean land = false;
        for (int x = -12_000; x <= 12_000; x += 600) {
            for (int z = -12_000; z <= 12_000; z += 600) {
                int height = sampler.sample(x, z).height();
                ocean |= height < TerrainSampler.SEA_LEVEL;
                land |= height > TerrainSampler.SEA_LEVEL + 8;
            }
        }
        assertTrue(ocean, "expected some ocean terrain");
        assertTrue(land, "expected some land terrain");
    }
}
