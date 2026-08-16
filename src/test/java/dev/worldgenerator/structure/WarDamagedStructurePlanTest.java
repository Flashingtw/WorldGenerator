package dev.worldgenerator.structure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.worldgenerator.map.AdventureMapPlan;
import dev.worldgenerator.map.MapPoi;
import dev.worldgenerator.map.PoiType;
import dev.worldgenerator.map.RoadSegment;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.EnumMap;
import org.junit.jupiter.api.Test;

class WarDamagedStructurePlanTest {
    @Test
    void poiSizesSelectTheThreeBuildingArchetypes() {
        AdventureMapPlan map = new AdventureMapPlan(List.of(
                new MapPoi(-400, 72, 0, PoiType.SMALL),
                new MapPoi(0, 73, 0, PoiType.MEDIUM),
                new MapPoi(400, 74, 0, PoiType.LARGE)), List.of());
        WarDamagedStructurePlan plan = WarDamagedStructurePlan.create(123L, map);
        assertEquals(List.of(
                        StructureType.GAS_STATION,
                        StructureType.WAREHOUSE,
                        StructureType.MILITARY_COMPOUND),
                plan.placements().stream().map(StructurePlacement::type).toList());
    }

    @Test
    void entrancesFaceTheNearestConnectedRoad() {
        MapPoi west = new MapPoi(0, 72, 0, PoiType.SMALL);
        MapPoi east = new MapPoi(300, 72, 0, PoiType.SMALL);
        AdventureMapPlan map = new AdventureMapPlan(
                List.of(west, east), List.of(new RoadSegment(west, east)));
        var placements = WarDamagedStructurePlan.create(7L, map).placements();
        assertEquals(1, placements.get(0).rotation());
        assertEquals(3, placements.get(1).rotation());
    }

    @Test
    void blueprintsContainArchitectureAndWarDamage() {
        assertContainsExpectedMaterials(PoiType.SMALL,
                StructureMaterial.GLASS, StructureMaterial.WARNING);
        assertContainsExpectedMaterials(PoiType.MEDIUM,
                StructureMaterial.BRICK, StructureMaterial.RUSTED_METAL);
        assertContainsExpectedMaterials(PoiType.LARGE,
                StructureMaterial.IRON_BARS, StructureMaterial.OLIVE_PANEL);
    }

    @Test
    void generationIsDeterministicButDamageVariesBySeed() {
        AdventureMapPlan map = onePoi(PoiType.MEDIUM);
        WarDamagedStructurePlan first = WarDamagedStructurePlan.create(100L, map);
        WarDamagedStructurePlan second = WarDamagedStructurePlan.create(100L, map);
        WarDamagedStructurePlan different = WarDamagedStructurePlan.create(101L, map);
        assertEquals(first.placements(), second.placements());
        assertEquals(allBlocks(first), allBlocks(second));
        assertNotEquals(allBlocks(first), allBlocks(different));
    }

    @Test
    void everyEditIsOwnedByExactlyOneRequestedChunk() {
        WarDamagedStructurePlan plan = WarDamagedStructurePlan.create(456L, onePoi(PoiType.LARGE));
        Set<String> coordinates = new HashSet<>();
        int total = 0;
        for (int chunkX = -6; chunkX <= 6; chunkX++) {
            for (int chunkZ = -6; chunkZ <= 6; chunkZ++) {
                for (StructureBlock block : plan.blocksInChunk(chunkX, chunkZ)) {
                    assertEquals(chunkX, Math.floorDiv(block.x(), 16));
                    assertEquals(chunkZ, Math.floorDiv(block.z(), 16));
                    assertTrue(coordinates.add(block.x() + ":" + block.y() + ":" + block.z()));
                    total++;
                }
            }
        }
        assertTrue(total > 25_000, "military compound should span many populated chunks");
    }

    @Test
    void generatedIronBarsCarryExplicitNeighborConnections() {
        List<StructureBlock> blocks = allBlocks(
                WarDamagedStructurePlan.create(456L, onePoi(PoiType.LARGE)));
        List<StructureBlock> bars = blocks.stream()
                .filter(block -> block.material() == StructureMaterial.IRON_BARS)
                .toList();
        assertFalse(bars.isEmpty());
        assertTrue(bars.stream().allMatch(block -> block.horizontalConnections() != 0),
                "ChunkData does not run neighbor physics; every iron bar must encode its faces");
    }

    @Test
    void sitesDoNotFillTheirEntireRectangularEnvelope() {
        for (PoiType type : PoiType.values()) {
            WarDamagedStructurePlan plan = WarDamagedStructurePlan.create(456L, onePoi(type));
            StructurePlacement placement = plan.placements().getFirst();
            long groundEdits = allBlocks(plan).stream()
                    .filter(block -> block.y() == placement.baseY())
                    .count();
            int rectangularArea = placement.type().width() * placement.type().depth();
            assertTrue(groundEdits < rectangularArea * 0.82,
                    type + " still fills a rectangular pad: " + groundEdits + "/" + rectangularArea);
        }
    }

    @Test
    void pavedAreasAvoidHighFrequencySpeckledFloors() {
        WarDamagedStructurePlan plan = WarDamagedStructurePlan.create(456L, onePoi(PoiType.SMALL));
        StructurePlacement placement = plan.placements().getFirst();
        Map<String, StructureMaterial> ground = new HashMap<>();
        allBlocks(plan).stream()
                .filter(block -> block.y() == placement.baseY())
                .forEach(block -> ground.put(block.x() + ":" + block.z(), block.material()));
        int comparisons = 0;
        int transitions = 0;
        for (StructureBlock block : allBlocks(plan)) {
            if (block.y() != placement.baseY()) continue;
            StructureMaterial east = ground.get((block.x() + 1) + ":" + block.z());
            StructureMaterial south = ground.get(block.x() + ":" + (block.z() + 1));
            if (east != null) { comparisons++; if (east != block.material()) transitions++; }
            if (south != null) { comparisons++; if (south != block.material()) transitions++; }
        }
        assertTrue(transitions / (double) comparisons < 0.14,
                "floor materials change too often: " + transitions + "/" + comparisons);
    }

    @Test
    void generatedGlassCarriesExplicitPaneConnections() {
        List<StructureBlock> glass = allBlocks(
                WarDamagedStructurePlan.create(456L, onePoi(PoiType.SMALL))).stream()
                .filter(block -> block.material() == StructureMaterial.GLASS)
                .toList();
        assertFalse(glass.isEmpty());
        assertTrue(glass.stream().allMatch(block -> block.horizontalConnections() != 0));
    }

    @Test
    void interiorsContainPurposeBuiltStaticDetails() {
        assertInteriorDensity(PoiType.SMALL, 45,
                StructureMaterial.SHELF, StructureMaterial.TABLE, StructureMaterial.MACHINE);
        assertInteriorDensity(PoiType.MEDIUM, 220,
                StructureMaterial.SHELF, StructureMaterial.TABLE, StructureMaterial.CHAIR);
        assertInteriorDensity(PoiType.LARGE, 300,
                StructureMaterial.SHELF, StructureMaterial.CABINET, StructureMaterial.MACHINE);
    }

    @Test
    void warDamageCreatesSubstantialRubbleAndScorchZones() {
        assertDamageDensity(PoiType.SMALL, 20);
        assertDamageDensity(PoiType.MEDIUM, 70);
        assertDamageDensity(PoiType.LARGE, 180);
    }

    @Test
    void mostGeneratedBuildingsRemainIntactOrWeathered() {
        Map<StructureCondition, Integer> counts = new EnumMap<>(StructureCondition.class);
        for (StructureCondition condition : StructureCondition.values()) counts.put(condition, 0);
        for (long seed = 0; seed < 1_000; seed++) {
            StructureCondition condition = WarDamagedStructurePlan.create(seed, onePoi(PoiType.SMALL))
                    .placements().getFirst().condition();
            counts.merge(condition, 1, Integer::sum);
        }
        int preserved = counts.get(StructureCondition.INTACT) + counts.get(StructureCondition.WEATHERED);
        assertTrue(preserved >= 640 && preserved <= 760, "unexpected preservation mix: " + counts);
        assertTrue(counts.get(StructureCondition.RUINED) >= 25
                && counts.get(StructureCondition.RUINED) <= 80, "unexpected ruin mix: " + counts);
    }

    private static void assertContainsExpectedMaterials(
            PoiType type, StructureMaterial first, StructureMaterial second) {
        List<StructureBlock> blocks = allBlocks(planWithCondition(type, StructureCondition.RUINED));
        Set<StructureMaterial> materials = new HashSet<>();
        blocks.forEach(block -> materials.add(block.material()));
        assertTrue(materials.contains(first), "missing " + first + " in " + type);
        assertTrue(materials.contains(second), "missing " + second + " in " + type);
        assertTrue(materials.contains(StructureMaterial.CLEAR), "interiors must be carved");
        assertTrue(materials.contains(StructureMaterial.RUBBLE), "war damage must leave rubble");
        assertFalse(blocks.isEmpty());
    }

    private static AdventureMapPlan onePoi(PoiType type) {
        return new AdventureMapPlan(List.of(new MapPoi(0, 72, 0, type)), List.of());
    }

    private static void assertInteriorDensity(
            PoiType type, int minimum, StructureMaterial... expected) {
        List<StructureBlock> blocks = allBlocks(planWithCondition(type, StructureCondition.WEATHERED));
        Set<StructureMaterial> present = new HashSet<>();
        blocks.forEach(block -> present.add(block.material()));
        for (StructureMaterial material : expected) {
            assertTrue(present.contains(material), type + " missing interior material " + material);
        }
        long details = blocks.stream().filter(block -> block.material() == StructureMaterial.SHELF
                || block.material() == StructureMaterial.COUNTER
                || block.material() == StructureMaterial.PIPE
                || block.material() == StructureMaterial.FURNITURE
                || block.material() == StructureMaterial.TABLE
                || block.material() == StructureMaterial.CHAIR
                || block.material() == StructureMaterial.CABINET
                || block.material() == StructureMaterial.CRATE
                || block.material() == StructureMaterial.MACHINE
                || block.material() == StructureMaterial.BED
                || block.material() == StructureMaterial.LOCKER
                || block.material() == StructureMaterial.SCREEN
                || block.material() == StructureMaterial.LIGHT).count();
        assertTrue(details >= minimum, type + " interior is too empty: " + details);
    }

    private static void assertDamageDensity(PoiType type, int minimum) {
        List<StructureBlock> blocks = allBlocks(planWithCondition(type, StructureCondition.RUINED));
        long damage = blocks.stream().filter(block -> block.material() == StructureMaterial.RUBBLE
                || block.material() == StructureMaterial.SCORCH
                || block.material() == StructureMaterial.RUSTED_METAL).count();
        assertTrue(damage >= minimum, type + " damage is too sparse: " + damage);
    }

    private static WarDamagedStructurePlan planWithCondition(
            PoiType type, StructureCondition condition) {
        for (long seed = 0; seed < 10_000; seed++) {
            WarDamagedStructurePlan plan = WarDamagedStructurePlan.create(seed, onePoi(type));
            if (plan.placements().getFirst().condition() == condition) return plan;
        }
        throw new AssertionError("could not find seed for " + condition);
    }

    private static List<StructureBlock> allBlocks(WarDamagedStructurePlan plan) {
        StructurePlacement placement = plan.placements().getFirst();
        int chunkReach = placement.horizontalReach() / 16 + 2;
        List<StructureBlock> result = new ArrayList<>();
        for (int chunkX = -chunkReach; chunkX <= chunkReach; chunkX++) {
            for (int chunkZ = -chunkReach; chunkZ <= chunkReach; chunkZ++) {
                result.addAll(plan.blocksInChunk(chunkX, chunkZ));
            }
        }
        return result;
    }
}
