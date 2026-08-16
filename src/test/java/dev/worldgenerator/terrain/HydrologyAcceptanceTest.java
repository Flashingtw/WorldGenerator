package dev.worldgenerator.terrain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class HydrologyAcceptanceTest {
    private static final long[] SEEDS = {1L, 12_345L, 0x5C00A11L};

    @Test
    void finiteMapsHaveConnectedDownhillDrainageAndInlineLakes() {
        for (int size : new int[] {5_000, 10_000}) {
            for (long seed : SEEDS) {
                WorldBounds bounds = new WorldBounds(size);
                BaseTerrainSampler base = new BaseTerrainSampler(seed, bounds);
                HydrologyPlan plan = HydrologyPlanner.create(seed, bounds, base);
                String context = "size=" + size + " seed=" + seed;
                assertTrue(plan.rivers().size() >= (size == 5_000 ? 3 : 5),
                        "too few rivers " + context);
                assertTrue(plan.lakes().size() >= (size == 5_000 ? 1 : 2),
                        "too few inline lakes " + context);

                for (HydrologyPlan.River river : plan.rivers()) {
                    List<HydrologyPlan.WaterNode> nodes = river.centerline();
                    assertTrue(nodes.size() >= 3, "short river " + context);
                    assertEquals(TerrainSampler.SEA_LEVEL,
                            (int) nodes.get(nodes.size() - 1).waterLevel(),
                            "river did not reach ocean " + context);
                    for (int index = 1; index < nodes.size(); index++) {
                        assertTrue(nodes.get(index).waterLevel()
                                        <= nodes.get(index - 1).waterLevel(),
                                "water flowed uphill " + context);
                    }
                    HydrologyPlan.WaterNode middle = nodes.get(nodes.size() / 2);
                    HydrologySample channel = plan.shape(
                            (int) middle.x(), (int) middle.z(),
                            base.sample((int) middle.x(), (int) middle.z()));
                    assertTrue(channel.riverStrength() > 0.8,
                            "missing channel at centerline " + context);
                    assertTrue(channel.waterLevel() > channel.height(),
                            "dry river channel " + context);
                }

                for (HydrologyPlan.Lake lake : plan.lakes()) {
                    HydrologySample center = plan.shape(
                            (int) lake.x(), (int) lake.z(),
                            base.sample((int) lake.x(), (int) lake.z()));
                    assertTrue(center.lakeStrength() > 0.8, "missing lake center " + context);
                    assertTrue(center.waterLevel() > center.height(), "dry lake " + context);
                    assertTrue(plan.rivers().stream().anyMatch(river -> river.centerline().stream()
                                    .anyMatch(node -> Math.hypot(
                                            node.x() - lake.x(), node.z() - lake.z()) < 2.0)),
                            "lake is disconnected from drainage " + context);
                }
            }
        }
    }

    @Test
    void hydrologyIsRepeatableButSeedDependent() {
        WorldBounds bounds = new WorldBounds(5_000);
        List<String> first = signature(HydrologyPlanner.create(
                12_345L, bounds, new BaseTerrainSampler(12_345L, bounds)));
        List<String> repeated = signature(HydrologyPlanner.create(
                12_345L, bounds, new BaseTerrainSampler(12_345L, bounds)));
        List<String> different = signature(HydrologyPlanner.create(
                12_346L, bounds, new BaseTerrainSampler(12_346L, bounds)));
        assertEquals(first, repeated);
        assertNotEquals(first, different);
    }

    @Test
    void unlimitedTerrainDoesNotReceiveFiniteArtificialDrainage() {
        HydrologyPlan plan = HydrologyPlanner.create(
                123L, WorldBounds.UNLIMITED,
                new BaseTerrainSampler(123L, WorldBounds.UNLIMITED));
        assertTrue(plan.rivers().isEmpty());
        assertTrue(plan.lakes().isEmpty());
        HydrologySample sample = plan.shape(0, 0, new TerrainSample(75, 0.7, 0.0));
        assertFalse(sample.waterLevel() > sample.height());
    }

    private static List<String> signature(HydrologyPlan plan) {
        List<String> result = new ArrayList<>();
        for (HydrologyPlan.River river : plan.rivers()) {
            result.add(river.centerline().toString());
        }
        result.add(plan.lakes().toString());
        return result;
    }
}
