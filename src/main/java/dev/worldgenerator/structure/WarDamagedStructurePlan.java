package dev.worldgenerator.structure;

import dev.worldgenerator.map.AdventureMapPlan;
import dev.worldgenerator.map.MapPoi;
import dev.worldgenerator.map.PoiType;
import dev.worldgenerator.map.RoadSegment;
import dev.worldgenerator.map.RoadNode;
import java.util.ArrayList;
import java.util.List;

/** Deep module that turns a gameplay layout into deterministic, chunk-safe ruins. */
public final class WarDamagedStructurePlan {
    private final List<StructurePlacement> placements;

    private WarDamagedStructurePlan(List<StructurePlacement> placements) {
        this.placements = List.copyOf(placements);
    }

    public static WarDamagedStructurePlan create(long seed, AdventureMapPlan map) {
        List<StructurePlacement> result = new ArrayList<>();
        for (MapPoi poi : map.pointsOfInterest()) {
            StructureType type = switch (poi.type()) {
                case SMALL -> StructureType.GAS_STATION;
                case MEDIUM -> StructureType.WAREHOUSE;
                case LARGE -> null;
            };
            // v0.6.0's compound was rejected visually. Large sites deliberately remain
            // empty until the authored blueprint module supplies an accepted replacement.
            if (type == null) continue;
            int rotation = roadFacing(poi, map.roads(), seed);
            long damageSeed = hash(seed, poi.x(), poi.z(), 0x5A17);
            result.add(new StructurePlacement(
                    poi.x(), poi.y(), poi.z(), type, rotation,
                    damageSeed, condition(damageSeed)));
        }
        return new WarDamagedStructurePlan(result);
    }

    public List<StructurePlacement> placements() {
        return placements;
    }

    /** Returns only the edits belonging to one chunk, including deliberate air carving. */
    public List<StructureBlock> blocksInChunk(int chunkX, int chunkZ) {
        int minX = chunkX << 4;
        int minZ = chunkZ << 4;
        int maxX = minX + 15;
        int maxZ = minZ + 15;
        List<StructureBlock> blocks = new ArrayList<>();
        for (StructurePlacement placement : placements) {
            int reach = placement.horizontalReach();
            if (maxX < placement.centerX() - reach || minX > placement.centerX() + reach
                    || maxZ < placement.centerZ() - reach || minZ > placement.centerZ() + reach) continue;
            for (int worldX = minX; worldX <= maxX; worldX++) {
                for (int worldZ = minZ; worldZ <= maxZ; worldZ++) {
                    int localX = placement.localX(worldX, worldZ);
                    int localZ = placement.localZ(worldX, worldZ);
                    if (Math.abs(localX) > placement.type().width() / 2
                            || Math.abs(localZ) > placement.type().depth() / 2) continue;
                    for (int dy = 0; dy <= placement.type().height(); dy++) {
                        StructureMaterial material = WarDamagedBlueprint.materialAt(
                                placement, localX, dy, localZ);
                        if (material != null) {
                            boolean connectable = material == StructureMaterial.IRON_BARS
                                    || material == StructureMaterial.GLASS;
                            int connections = connectable
                                    ? connectionsAt(placement, worldX, placement.baseY() + dy, worldZ)
                                    : 0;
                            if (material == StructureMaterial.IRON_BARS && connections == 0) {
                                material = StructureMaterial.RUSTED_METAL;
                            }
                            if (material == StructureMaterial.GLASS && connections == 0) {
                                material = StructureMaterial.CLEAR;
                            }
                            int facing = Math.floorMod(placement.rotation(), 4);
                            blocks.add(new StructureBlock(
                                    worldX, placement.baseY() + dy, worldZ, material, connections, facing));
                        }
                    }
                }
            }
        }
        return blocks;
    }

    private static int connectionsAt(StructurePlacement placement, int x, int y, int z) {
        int connections = 0;
        if (connectsToBars(placement, x, y, z - 1)) connections |= StructureBlock.NORTH;
        if (connectsToBars(placement, x + 1, y, z)) connections |= StructureBlock.EAST;
        if (connectsToBars(placement, x, y, z + 1)) connections |= StructureBlock.SOUTH;
        if (connectsToBars(placement, x - 1, y, z)) connections |= StructureBlock.WEST;
        return connections;
    }

    private static boolean connectsToBars(StructurePlacement placement, int x, int y, int z) {
        int localX = placement.localX(x, z);
        int localZ = placement.localZ(x, z);
        int dy = y - placement.baseY();
        if (Math.abs(localX) > placement.type().width() / 2
                || Math.abs(localZ) > placement.type().depth() / 2
                || dy < 0 || dy > placement.type().height()) return false;
        StructureMaterial material = WarDamagedBlueprint.materialAt(placement, localX, dy, localZ);
        return material != null && material != StructureMaterial.CLEAR
                && material != StructureMaterial.PAD && material != StructureMaterial.CRACKED_PAD;
    }

    private static int roadFacing(MapPoi poi, List<RoadSegment> roads, long seed) {
        RoadNode nearestEntrance = null;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (RoadSegment road : roads) {
            RoadNode entrance = null;
            if (road.from().equals(poi)) entrance = road.centerline().get(0);
            if (road.to().equals(poi)) {
                entrance = road.centerline().get(road.centerline().size() - 1);
            }
            if (entrance == null) continue;
            double distance = Math.hypot(entrance.x() - poi.x(), entrance.z() - poi.z());
            if (distance < 1.0) {
                MapPoi other = road.from().equals(poi) ? road.to() : road.from();
                entrance = new RoadNode(other.x(), other.y(), other.z());
                distance = Math.hypot(entrance.x() - poi.x(), entrance.z() - poi.z());
            }
            if (distance < bestDistance) {
                bestDistance = distance;
                nearestEntrance = entrance;
            }
        }
        if (nearestEntrance == null) {
            return (int) Math.floorMod(hash(seed, poi.x(), poi.z(), 7), 4);
        }
        double dx = nearestEntrance.x() - poi.x();
        double dz = nearestEntrance.z() - poi.z();
        if (Math.abs(dx) > Math.abs(dz)) return dx > 0 ? 1 : 3;
        return dz > 0 ? 2 : 0;
    }

    private static StructureCondition condition(long value) {
        int roll = (int) Math.floorMod(value, 100);
        if (roll < 20) return StructureCondition.INTACT;
        if (roll < 70) return StructureCondition.WEATHERED;
        if (roll < 95) return StructureCondition.DAMAGED;
        return StructureCondition.RUINED;
    }

    private static long hash(long seed, int x, int z, int salt) {
        long value = seed ^ ((long) x * 0x9E3779B97F4A7C15L)
                ^ ((long) z * 0xC2B2AE3D27D4EB4FL) ^ salt;
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }
}
