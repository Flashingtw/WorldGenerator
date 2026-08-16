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
    void adjacentSeedsProduceMacroscopicallyDifferentFiniteMaps() {
        BaseTerrainSampler first = new BaseTerrainSampler(12_345L, new WorldBounds(5_000));
        BaseTerrainSampler second = new BaseTerrainSampler(12_346L, new WorldBounds(5_000));
        int samples = 0;
        int visiblyChanged = 0;
        int terrainClassChanged = 0;
        for (int x = -2_400; x <= 2_400; x += 80) {
            for (int z = -2_400; z <= 2_400; z += 80) {
                TerrainSample a = first.sample(x, z);
                TerrainSample b = second.sample(x, z);
                if (Math.abs(a.height() - b.height()) >= 4) visiblyChanged++;
                if (terrainClass(a) != terrainClass(b)) terrainClassChanged++;
                samples++;
            }
        }
        String context = "changed=" + visiblyChanged + "/" + samples
                + " classes=" + terrainClassChanged + "/" + samples;
        assertTrue(visiblyChanged >= samples * 0.25, context);
        assertTrue(terrainClassChanged >= samples * 0.06, context);
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


    private static int terrainClass(TerrainSample sample) {
        if (sample.height() < TerrainSampler.SEA_LEVEL) return 0;
        if (sample.mountainStrength() >= 0.42) return 3;
        if (sample.height() > 90) return 2;
        return 1;
    }
}
