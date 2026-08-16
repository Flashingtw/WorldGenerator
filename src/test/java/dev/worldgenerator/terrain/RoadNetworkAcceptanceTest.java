package dev.worldgenerator.terrain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.worldgenerator.map.MapPoi;
import dev.worldgenerator.map.RoadKind;
import dev.worldgenerator.map.RoadNode;
import dev.worldgenerator.map.RoadSegment;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RoadNetworkAcceptanceTest {
    private static final long[] SEEDS = {1L, 12_345L, 0x5C00A11L};

    @Test
    void finiteRoadNetworksAreConnectedHierarchicalAndTerrainAware() {
        for (int size : new int[] {5_000, 10_000}) {
            for (long seed : SEEDS) {
                WorldBounds bounds = new WorldBounds(size);
                BaseTerrainSampler base = new BaseTerrainSampler(seed, bounds);
                TerrainSampler terrain = new TerrainSampler(seed, bounds);
                var plan = terrain.plan();
                String context = "size=" + size + " seed=" + seed;
                assertTrue(plan.roads().stream().anyMatch(road -> road.kind() == RoadKind.TRUNK),
                        "missing trunk road " + context);
                assertTrue(plan.roads().stream().anyMatch(road -> road.kind() == RoadKind.BRANCH),
                        "missing branch road " + context);
                assertEquals(plan.pointsOfInterest().size(), connectedPois(plan).size(),
                        "disconnected road graph " + context);

                for (MapPoi poi : plan.pointsOfInterest()) {
                    assertTrue(plan.shape(poi.x(), poi.z(), poi.y()).roadStrength() < 0.01,
                            "road entered POI core " + poi + " " + context);
                }
                for (RoadSegment road : plan.roads()) {
                    verifyRoute(base, terrain.hydrology(), road, context);
                }
            }
        }
    }

    @Test
    void hierarchyHasPedestrianAndVehicleScaleWidths() {
        assertTrue(RoadKind.TRUNK.coreRadius() > RoadKind.BRANCH.coreRadius());
        assertTrue(RoadKind.BRANCH.coreRadius() > RoadKind.ACCESS.coreRadius());
        assertTrue(RoadKind.TRUNK.coreRadius() * 2.0 >= 12.0);
        assertTrue(RoadKind.ACCESS.coreRadius() * 2.0 >= 7.0);
    }

    private static Set<MapPoi> connectedPois(dev.worldgenerator.map.AdventureMapPlan plan) {
        Set<MapPoi> visited = new HashSet<>();
        ArrayDeque<MapPoi> pending = new ArrayDeque<>();
        pending.add(plan.pointsOfInterest().get(0));
        visited.add(plan.pointsOfInterest().get(0));
        while (!pending.isEmpty()) {
            MapPoi current = pending.removeFirst();
            for (RoadSegment road : plan.roads()) {
                MapPoi next = road.from().equals(current) ? road.to()
                        : road.to().equals(current) ? road.from() : null;
                if (next != null && visited.add(next)) pending.addLast(next);
            }
        }
        return visited;
    }

    private static void verifyRoute(
            BaseTerrainSampler base, HydrologyPlan hydrology,
            RoadSegment road, String context) {
        double routeLength = 0.0;
        double continuousWater = 0.0;
        for (int index = 1; index < road.centerline().size(); index++) {
            RoadNode previous = road.centerline().get(index - 1);
            RoadNode node = road.centerline().get(index);
            double segmentLength = previous.horizontalDistanceTo(node);
            routeLength += segmentLength;
            double grade = Math.abs(node.y() - previous.y()) / Math.max(1.0, segmentLength);
            assertTrue(grade <= 0.106,
                    "excess road grade=" + grade + " " + road + " " + context);
            int samples = Math.max(1, (int) Math.ceil(segmentLength / 32.0));
            for (int step = 0; step <= samples; step++) {
                double amount = step / (double) samples;
                int x = (int) Math.round(previous.x() + (node.x() - previous.x()) * amount);
                int z = (int) Math.round(previous.z() + (node.z() - previous.z()) * amount);
                TerrainSample sample = base.sample(x, z);
                HydrologySample hydrated = hydrology.shape(x, z, sample);
                if (hydrated.waterLevel() > hydrated.height()) {
                    continuousWater += segmentLength / samples;
                } else {
                    continuousWater = 0.0;
                }
                assertTrue(continuousWater <= 96.0,
                        "road followed water instead of reserving a crossing at "
                                + x + "," + z + " " + road + " " + context);
                assertTrue(sample.mountainStrength() < 0.82,
                        "road crossed mountain core at " + x + "," + z
                                + " " + road + " " + context);
            }
        }
        double direct = Math.hypot(
                road.to().x() - road.from().x(), road.to().z() - road.from().z());
        assertTrue(routeLength <= direct * 2.8,
                "road detour is excessive: route=" + routeLength
                        + " direct=" + direct + " " + road + " " + context);
    }
}
