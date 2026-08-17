package dev.worldgenerator.terrain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SatelliteIslandPlanTest {
    @Test
    void finiteWorldsCreateDeterministicOffshoreDestinations() {
        for (int size : new int[] {5_000, 10_000}) {
            for (long seed : new long[] {1L, 12_345L, 0x5C00A11L}) {
                WorldBounds bounds = new WorldBounds(size);
                SatelliteIslandPlan first = new SatelliteIslandPlan(seed, bounds);
                SatelliteIslandPlan repeated = new SatelliteIslandPlan(seed, bounds);
                assertEquals(first.islands(), repeated.islands());
                assertEquals(first.bridges(), repeated.bridges());
                assertEquals(first.islands().size(), first.bridges().size());
                assertTrue(first.islands().size() <= (size == 5_000 ? 2 : 3));

                BaseTerrainSampler terrain = new BaseTerrainSampler(seed, bounds);
                for (SatelliteIslandPlan.Island island : first.islands()) {
                    assertTrue(Math.hypot(island.centerX(), island.centerZ())
                                    + Math.max(island.radialLength(), island.tangentialWidth())
                                    <= size / 2.0,
                            "satellite island can be clipped by the finite world border");
                    TerrainSample center = terrain.sample(
                            (int) Math.round(island.centerX()),
                            (int) Math.round(island.centerZ()));
                    assertTrue(center.height() >= TerrainSampler.SEA_LEVEL + 3,
                            "satellite island center is submerged");
                    double channelRadius = Math.hypot(island.centerX(), island.centerZ())
                            - island.radialLength() * 1.10;
                    int channelX = (int) Math.round(Math.cos(island.angle()) * channelRadius);
                    int channelZ = (int) Math.round(Math.sin(island.angle()) * channelRadius);
                    assertTrue(terrain.sample(channelX, channelZ).height()
                                    < TerrainSampler.SEA_LEVEL,
                            "satellite island merged into the main island");
                }
            }
        }
    }

    @Test
    void bridgesHaveTwoDeckSpansAndAnOpenWaterGap() {
        SatelliteIslandPlan plan = new SatelliteIslandPlan(
                12_345L, new WorldBounds(5_000));
        assertFalse(plan.bridges().isEmpty());
        for (SatelliteIslandPlan.BrokenBridge bridge : plan.bridges()) {
            assertTrue(bridge.deckY() >= TerrainSampler.SEA_LEVEL + 9
                            && bridge.deckY() <= TerrainSampler.SEA_LEVEL + 11,
                    "bridge deck is outside the low road-bridge clearance range");
            assertTrue(sampleAt(bridge, bridge.firstBreak() * 0.5).deck());
            assertFalse(sampleAt(bridge,
                    (bridge.firstBreak() + bridge.secondBreak()) * 0.5).deck());
            assertTrue(sampleAt(bridge, (bridge.secondBreak() + 1.0) * 0.5).deck());
        }
    }

    @Test
    void eachBridgeHasGradedRoadConnectionsOnBothSides() {
        long seed = 12_345L;
        WorldBounds bounds = new WorldBounds(5_000);
        SatelliteIslandPlan islands = new SatelliteIslandPlan(seed, bounds);
        TerrainSampler terrain = new TerrainSampler(seed, bounds);
        assertEquals(islands.bridges().size() * 2,
                terrain.plan().bridgeApproaches().size());
        for (SatelliteIslandPlan.BrokenBridge bridge : islands.bridges()) {
            for (double[] endpoint : new double[][] {
                    {bridge.startX(), bridge.startZ()},
                    {bridge.endX(), bridge.endZ()}}) {
                TerrainSample approach = terrain.sample(
                        (int) Math.round(endpoint[0]), (int) Math.round(endpoint[1]));
                assertEquals(bridge.deckY(), approach.height(),
                        "road approach does not meet the bridge deck");
                assertTrue(approach.roadStrength() > 0.90,
                        "bridge endpoint is not connected to a road");
            }
        }
        for (var road : terrain.plan().bridgeApproaches()) {
            for (int index = 1; index < road.centerline().size(); index++) {
                var before = road.centerline().get(index - 1);
                var after = road.centerline().get(index);
                double grade = Math.abs(after.y() - before.y())
                        / Math.max(1.0, before.horizontalDistanceTo(after));
                assertTrue(grade <= 0.106, "bridge approach is too steep: " + grade);
            }
        }
    }

    @Test
    void unlimitedWorldsDoNotReceiveFiniteMapFeatures() {
        SatelliteIslandPlan plan = new SatelliteIslandPlan(123L, WorldBounds.UNLIMITED);
        assertTrue(plan.islands().isEmpty());
        assertTrue(plan.bridges().isEmpty());
    }

    private static SatelliteIslandPlan.BridgeColumn sampleAt(
            SatelliteIslandPlan.BrokenBridge bridge, double t) {
        int x = (int) Math.round(bridge.startX()
                + (bridge.endX() - bridge.startX()) * t);
        int z = (int) Math.round(bridge.startZ()
                + (bridge.endZ() - bridge.startZ()) * t);
        return bridge.sample(x, z);
    }
}
