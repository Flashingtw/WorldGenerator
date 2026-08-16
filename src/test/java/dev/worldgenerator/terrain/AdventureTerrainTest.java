package dev.worldgenerator.terrain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.worldgenerator.map.MapPoi;
import dev.worldgenerator.map.RoadSegment;
import org.junit.jupiter.api.Test;

class AdventureTerrainTest {
    private static final long SEED = 0x5C00A11L;

    @Test
    void finiteMapIsAnIslandWithUsableInteriorAndMountainRanges() {
        TerrainSampler terrain = new TerrainSampler(SEED, new WorldBounds(5_000));
        assertTrue(terrain.sample(0, 0).height() > TerrainSampler.SEA_LEVEL + 5);
        assertTrue(terrain.sample(2_350, 0).height() < TerrainSampler.SEA_LEVEL);

        int highest = Integer.MIN_VALUE;
        int land = 0;
        int mountain = 0;
        for (int x = -2_200; x <= 2_200; x += 100) {
            for (int z = -2_200; z <= 2_200; z += 100) {
                TerrainSample sample = terrain.sample(x, z);
                highest = Math.max(highest, sample.height());
                if (sample.height() > TerrainSampler.SEA_LEVEL + 3) land++;
                if (sample.mountainStrength() > 0.55) mountain++;
            }
        }
        assertTrue(highest >= 118, "expected a prominent mountain, highest=" + highest);
        assertTrue(land > 700, "expected broad playable land, columns=" + land);
        assertTrue(mountain > 25, "expected coherent mountain ranges, columns=" + mountain);
    }

    @Test
    void plannedPoisAreFlatAndRoadNetworkIsConnected() {
        TerrainSampler terrain = new TerrainSampler(SEED, new WorldBounds(5_000));
        var plan = terrain.plan();
        assertTrue(plan.pointsOfInterest().size() >= 14);
        assertTrue(plan.roads().size() >= plan.pointsOfInterest().size() - 1);

        for (MapPoi poi : plan.pointsOfInterest()) {
            TerrainSample sample = terrain.sample(poi.x(), poi.z());
            assertEquals(poi.y(), sample.height());
            assertTrue(sample.poiStrength() > 0.95);
            assertTrue(sample.roadStrength() < 0.01,
                    "external gravel road must stop before POI interior at " + poi);
        }
        for (RoadSegment road : plan.roads()) {
            int x = (road.from().x() + road.to().x()) / 2;
            int z = (road.from().z() + road.to().z()) / 2;
            assertTrue(terrain.sample(x, z).roadStrength() > 0.90);
        }
    }

    @Test
    void finiteGameplayPlanIsDeterministic() {
        TerrainSampler first = new TerrainSampler(SEED, new WorldBounds(10_000));
        TerrainSampler second = new TerrainSampler(SEED, new WorldBounds(10_000));
        assertEquals(first.plan().pointsOfInterest(), second.plan().pointsOfInterest());
        assertEquals(first.plan().roads(), second.plan().roads());
    }
}
