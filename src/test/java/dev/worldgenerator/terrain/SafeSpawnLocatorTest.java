package dev.worldgenerator.terrain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SafeSpawnLocatorTest {
    @Test
    void seed123StartsOnModerateLand() {
        SpawnPoint spawn = new SafeSpawnLocator(123L).locate();
        TerrainSample terrain = new TerrainSampler(123L).sample(spawn.x(), spawn.z());
        assertTrue(terrain.height() >= TerrainSampler.SEA_LEVEL + 4, "spawn must be inland");
        assertTrue(terrain.height() <= 108, "spawn must avoid extreme peaks");
        assertEquals(terrain.height() + 1, spawn.y());
    }

    @Test
    void spawnSelectionIsDeterministic() {
        assertEquals(new SafeSpawnLocator(98765L).locate(), new SafeSpawnLocator(98765L).locate());
    }
}
