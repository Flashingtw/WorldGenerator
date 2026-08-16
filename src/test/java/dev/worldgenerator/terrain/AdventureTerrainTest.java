package dev.worldgenerator.terrain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.worldgenerator.map.MapPoi;
import dev.worldgenerator.map.PoiType;
import dev.worldgenerator.map.RoadKind;
import dev.worldgenerator.map.RoadNode;
import dev.worldgenerator.map.RoadSegment;
import java.util.HashMap;
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
            RoadNode center = road.centerline().get(road.centerline().size() / 2);
            int x = (int) Math.round(center.x());
            int z = (int) Math.round(center.z());
            assertTrue(terrain.sample(x, z).roadStrength() > 0.90);
        }
    }

    @Test
    void roadNetworkHasStableEntrancesAndAVisibleHierarchy() {
        TerrainSampler terrain = new TerrainSampler(SEED, new WorldBounds(5_000));
        var plan = terrain.plan();
        assertTrue(plan.roads().stream().anyMatch(road -> road.kind() == RoadKind.TRUNK));
        assertTrue(plan.roads().stream().anyMatch(road -> road.kind() == RoadKind.BRANCH));

        var entrances = new HashMap<MapPoi, RoadNode>();
        int curved = 0;
        for (RoadSegment road : plan.roads()) {
            RoadNode first = road.centerline().get(0);
            RoadNode last = road.centerline().get(road.centerline().size() - 1);
            assertEquals(RoadKind.ACCESS,
                    road.kindAt((int) Math.round(first.x()), (int) Math.round(first.z())));
            assertEquals(RoadKind.ACCESS,
                    road.kindAt((int) Math.round(last.x()), (int) Math.round(last.z())));
            assertSameEntrance(entrances, road.from(), first);
            assertSameEntrance(entrances, road.to(), last);
            if (maximumDeviationFromChord(road.centerline()) >= 10.0) curved++;
        }
        assertTrue(curved >= plan.roads().size() / 3,
                "too many routes remained straight: curved=" + curved
                        + "/" + plan.roads().size());
    }

    @Test
    void terrainAwareRoutesStayOnLandAndKeepDriveableGrades() {
        BaseTerrainSampler base = new BaseTerrainSampler(SEED, new WorldBounds(5_000));
        TerrainSampler terrain = new TerrainSampler(SEED, new WorldBounds(5_000));
        for (RoadSegment road : terrain.plan().roads()) {
            for (int index = 0; index < road.centerline().size(); index++) {
                RoadNode node = road.centerline().get(index);
                if (index == 0) continue;
                RoadNode previous = road.centerline().get(index - 1);
                double grade = Math.abs(node.y() - previous.y())
                        / Math.max(1.0, node.horizontalDistanceTo(previous));
                assertTrue(grade <= 0.106,
                        "road grade=" + grade + " on " + road);
                int samples = Math.max(1,
                        (int) Math.ceil(node.horizontalDistanceTo(previous) / 24.0));
                for (int step = 0; step <= samples; step++) {
                    double amount = step / (double) samples;
                    int x = (int) Math.round(previous.x() + (node.x() - previous.x()) * amount);
                    int z = (int) Math.round(previous.z() + (node.z() - previous.z()) * amount);
                    TerrainSample sample = base.sample(x, z);
                    assertTrue(sample.height() > TerrainSampler.SEA_LEVEL + 1,
                            "road entered ocean: " + road + " at " + x + "," + z);
                    assertTrue(sample.mountainStrength() < 0.82,
                            "road crossed a mountain core: " + road + " at " + x + "," + z);
                }
            }
        }
    }

    @Test
    void finiteGameplayPlanIsDeterministic() {
        TerrainSampler first = new TerrainSampler(SEED, new WorldBounds(10_000));
        TerrainSampler second = new TerrainSampler(SEED, new WorldBounds(10_000));
        assertEquals(first.plan().pointsOfInterest(), second.plan().pointsOfInterest());
        assertEquals(first.plan().roads(), second.plan().roads());
    }

    @Test
    void gravelRoadReachesSmallAndMediumPoiEntrances() {
        for (PoiType type : new PoiType[] {PoiType.SMALL, PoiType.MEDIUM}) {
            MapPoi site = new MapPoi(0, 72, 0, type);
            MapPoi approach = new MapPoi(300, 72, 0, PoiType.SMALL);
            var plan = new dev.worldgenerator.map.AdventureMapPlan(
                    java.util.List.of(site, approach),
                    java.util.List.of(new RoadSegment(site, approach)));
            int entranceDistance = type == PoiType.SMALL ? 18 : 26;
            assertTrue(plan.shape(entranceDistance, 0, 72).roadStrength() > 0.65,
                    type + " gravel stops before its authored entrance");
        }
    }

    private static void assertSameEntrance(
            HashMap<MapPoi, RoadNode> entrances, MapPoi poi, RoadNode candidate) {
        RoadNode existing = entrances.putIfAbsent(poi, candidate);
        if (existing != null) assertEquals(existing, candidate, "unstable entrance for " + poi);
    }

    private static double maximumDeviationFromChord(java.util.List<RoadNode> nodes) {
        RoadNode from = nodes.get(0);
        RoadNode to = nodes.get(nodes.size() - 1);
        double dx = to.x() - from.x();
        double dz = to.z() - from.z();
        double length = Math.max(1.0, Math.hypot(dx, dz));
        double maximum = 0.0;
        for (RoadNode node : nodes) {
            double cross = Math.abs((node.x() - from.x()) * dz
                    - (node.z() - from.z()) * dx);
            maximum = Math.max(maximum, cross / length);
        }
        return maximum;
    }
}
