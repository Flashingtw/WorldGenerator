package dev.worldgenerator.terrain;

import dev.worldgenerator.biome.BiomeKind;
import dev.worldgenerator.biome.BiomeSampler;
import dev.worldgenerator.biome.SurfaceKind;
import dev.worldgenerator.biome.SurfaceSample;
import dev.worldgenerator.biome.SurfaceSampler;
import dev.worldgenerator.biome.WorldGenBiomeProvider;
import dev.worldgenerator.structure.StructureBlock;
import dev.worldgenerator.structure.StructureMaterial;
import dev.worldgenerator.structure.WarDamagedStructurePlan;
import java.util.Random;
import org.bukkit.HeightMap;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

public final class WorldGenChunkGenerator extends ChunkGenerator {
    private static final GenerationPolicy POLICY = GenerationPolicy.BARREN;
    private final TerrainSampler terrain;
    private final BiomeSampler biomes;
    private final SurfaceSampler surfaces;
    private final WarDamagedStructurePlan structures;
    private final SatelliteIslandPlan satelliteIslands;
    private final long seed;
    private final WorldBounds bounds;

    public WorldGenChunkGenerator(long seed) {
        this(seed, WorldBounds.UNLIMITED);
    }

    public WorldGenChunkGenerator(long seed, WorldBounds bounds) {
        this.seed = seed;
        this.bounds = bounds;
        this.terrain = new TerrainSampler(seed, bounds);
        this.biomes = new BiomeSampler(seed, bounds);
        this.surfaces = new SurfaceSampler(seed);
        this.structures = WarDamagedStructurePlan.create(seed, terrain.plan());
        this.satelliteIslands = new SatelliteIslandPlan(seed, bounds);
    }

    @Override
    public void generateNoise(
            @NotNull WorldInfo worldInfo,
            @NotNull Random random,
            int chunkX,
            int chunkZ,
            @NotNull ChunkData chunkData) {
        int minY = chunkData.getMinHeight();
        int maxY = chunkData.getMaxHeight();
        int originX = chunkX << 4;
        int originZ = chunkZ << 4;

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                if (!bounds.contains(originX + localX, originZ + localZ)) continue;
                TerrainSample sample = terrain.sample(originX + localX, originZ + localZ);
                int surfaceY = Math.max(minY + 1, Math.min(maxY - 1, sample.height()));
                BiomeKind biome = biomes.sample(originX + localX, surfaceY, originZ + localZ);
                SurfaceSample surface = surfaces.sample(
                        originX + localX, originZ + localZ, biome, sample);
                setColumn(chunkData, localX, localZ, minY, surfaceY, surface, sample);
            }
        }
        placeStructures(chunkData, chunkX, chunkZ);
        placeBrokenBridges(chunkData, chunkX, chunkZ);
    }

    private void placeBrokenBridges(ChunkData data, int chunkX, int chunkZ) {
        int originX = chunkX << 4;
        int originZ = chunkZ << 4;
        for (int localX = 0; localX < 16; localX++) {
            int x = originX + localX;
            for (int localZ = 0; localZ < 16; localZ++) {
                int z = originZ + localZ;
                for (SatelliteIslandPlan.BrokenBridge bridge : satelliteIslands.bridges()) {
                    SatelliteIslandPlan.BridgeColumn column = bridge.sample(x, z);
                    if (!column.deck()) continue;
                    int y = bridge.deckY();
                    if (y - 2 < data.getMinHeight() || y + 1 >= data.getMaxHeight()) continue;
                    Material foundation = column.damaged()
                            ? Material.CRACKED_STONE_BRICKS : Material.STONE_BRICKS;
                    if (column.pier()) {
                        int floor = Math.max(data.getMinHeight() + 1,
                                Math.min(y - 3, terrain.sample(x, z).height() + 1));
                        for (int supportY = floor; supportY < y - 1; supportY++) {
                            data.setBlock(localX, supportY, localZ, foundation);
                        }
                    }
                    if (column.pierCap()) {
                        data.setBlock(localX, y - 3, localZ, foundation);
                    }
                    data.setBlock(localX, y - 2, localZ, foundation);
                    data.setBlock(localX, y - 1, localZ,
                            column.damaged() ? Material.TUFF_BRICKS : Material.POLISHED_ANDESITE);
                    data.setBlock(localX, y, localZ,
                            column.damaged() ? Material.CRACKED_STONE_BRICKS
                                    : Material.GRAY_CONCRETE);
                    if (column.edge() && !column.damaged()) {
                        data.setBlock(localX, y + 1, localZ, Material.STONE_BRICK_SLAB);
                    }
                }
            }
        }
    }

    private void placeStructures(ChunkData data, int chunkX, int chunkZ) {
        int originX = chunkX << 4;
        int originZ = chunkZ << 4;
        for (StructureBlock block : structures.blocksInChunk(chunkX, chunkZ)) {
            if (block.y() < data.getMinHeight() || block.y() >= data.getMaxHeight()) continue;
            int localX = block.x() - originX;
            int localZ = block.z() - originZ;
            if (block.material() == StructureMaterial.IRON_BARS) {
                data.setBlock(localX, block.y(), localZ, connectedIronBars(block.horizontalConnections()));
            } else if (block.material() == StructureMaterial.GLASS) {
                data.setBlock(localX, block.y(), localZ,
                        connectedPane(block.horizontalConnections()));
            } else if (block.material() == StructureMaterial.CHAIR) {
                data.setBlock(localX, block.y(), localZ, chair(block.facing()));
            } else if (block.material() == StructureMaterial.TABLE
                    || block.material() == StructureMaterial.COUNTER) {
                data.setBlock(localX, block.y(), localZ,
                        slab(block.material() == StructureMaterial.COUNTER));
            } else if (block.material() == StructureMaterial.MACHINE) {
                data.setBlock(localX, block.y(), localZ, machine(block.facing()));
            } else {
                data.setBlock(localX, block.y(), localZ, material(block.material()));
            }
        }
    }

    private static MultipleFacing connectedIronBars(int connections) {
        MultipleFacing bars = (MultipleFacing) Material.IRON_BARS.createBlockData();
        bars.setFace(BlockFace.NORTH, (connections & StructureBlock.NORTH) != 0);
        bars.setFace(BlockFace.EAST, (connections & StructureBlock.EAST) != 0);
        bars.setFace(BlockFace.SOUTH, (connections & StructureBlock.SOUTH) != 0);
        bars.setFace(BlockFace.WEST, (connections & StructureBlock.WEST) != 0);
        return bars;
    }

    private static MultipleFacing connectedPane(int connections) {
        MultipleFacing pane = (MultipleFacing) Material.LIGHT_GRAY_STAINED_GLASS_PANE.createBlockData();
        pane.setFace(BlockFace.NORTH, (connections & StructureBlock.NORTH) != 0);
        pane.setFace(BlockFace.EAST, (connections & StructureBlock.EAST) != 0);
        pane.setFace(BlockFace.SOUTH, (connections & StructureBlock.SOUTH) != 0);
        pane.setFace(BlockFace.WEST, (connections & StructureBlock.WEST) != 0);
        return pane;
    }

    private static Stairs chair(int facing) {
        Stairs stairs = (Stairs) Material.SPRUCE_STAIRS.createBlockData();
        stairs.setFacing(blockFace(facing));
        return stairs;
    }

    private static Slab slab(boolean stone) {
        Slab slab = (Slab) (stone ? Material.SMOOTH_STONE_SLAB : Material.SPRUCE_SLAB).createBlockData();
        slab.setType(Slab.Type.TOP);
        return slab;
    }

    private static Directional machine(int facing) {
        Directional machine = (Directional) Material.BLAST_FURNACE.createBlockData();
        machine.setFacing(blockFace(facing));
        return machine;
    }

    private static BlockFace blockFace(int facing) {
        return switch (Math.floorMod(facing, 4)) {
            case 1 -> BlockFace.EAST;
            case 2 -> BlockFace.SOUTH;
            case 3 -> BlockFace.WEST;
            default -> BlockFace.NORTH;
        };
    }

    private static Material material(StructureMaterial material) {
        return switch (material) {
            case CLEAR -> Material.AIR;
            case PAD -> Material.POLISHED_ANDESITE;
            case CRACKED_PAD -> Material.CRACKED_STONE_BRICKS;
            case CONCRETE -> Material.LIGHT_GRAY_CONCRETE;
            case CRACKED_CONCRETE -> Material.TUFF_BRICKS;
            case BRICK -> Material.BRICKS;
            case METAL -> Material.IRON_BLOCK;
            case RUSTED_METAL -> Material.EXPOSED_COPPER;
            case DARK_ROOF -> Material.POLISHED_DEEPSLATE;
            case GLASS -> Material.LIGHT_GRAY_STAINED_GLASS_PANE;
            case IRON_BARS -> Material.IRON_BARS;
            case WARNING -> Material.YELLOW_CONCRETE;
            case WOOD -> Material.SPRUCE_PLANKS;
            case RUBBLE -> Material.COBBLESTONE;
            case SHELF -> Material.DARK_OAK_PLANKS;
            case COUNTER -> Material.SMOOTH_STONE;
            case PIPE -> Material.WEATHERED_COPPER;
            case FURNITURE -> Material.STRIPPED_SPRUCE_WOOD;
            case SCORCH -> Material.BLACKSTONE;
            case TABLE -> Material.SPRUCE_SLAB;
            case CHAIR -> Material.SPRUCE_STAIRS;
            case CABINET -> Material.BARREL;
            case CRATE -> Material.STRIPPED_DARK_OAK_WOOD;
            case MACHINE -> Material.BLAST_FURNACE;
            case OLIVE_PANEL -> Material.GREEN_TERRACOTTA;
            case WHITE_PANEL -> Material.WHITE_CONCRETE;
            case FLOOR -> Material.SMOOTH_STONE;
            case BED -> Material.LIGHT_GRAY_WOOL;
            case LOCKER -> Material.IRON_BLOCK;
            case SCREEN -> Material.BLACK_STAINED_GLASS;
            case LIGHT -> Material.SEA_LANTERN;
            case HESCO -> Material.SMOOTH_SANDSTONE;
        };
    }

    private static void setColumn(
            ChunkData data, int x, int z, int minY, int surfaceY, SurfaceSample surface,
            TerrainSample sample) {
        data.setBlock(x, minY, z, Material.BEDROCK);
        for (int y = minY + 1; y <= surfaceY; y++) {
            Material material;
            int depth = surfaceY - y;
            if (sample.roadStrength() > 0.25) {
                material = depth == 0 ? Material.GRAVEL : depth < 3 ? Material.COARSE_DIRT : Material.STONE;
            } else {
                material = surfaceMaterial(surface.kind(), depth);
            }
            data.setBlock(x, y, z, material);
        }
        for (int y = surfaceY + 1; y <= sample.waterLevel() && y < data.getMaxHeight(); y++) {
            data.setBlock(x, y, z, Material.WATER);
        }
        if (surface.shortGrass() && surfaceY + 1 < data.getMaxHeight()) {
            data.setBlock(x, surfaceY + 1, z, Material.SHORT_GRASS);
        }
    }

    private static Material surfaceMaterial(SurfaceKind kind, int depth) {
        return switch (kind) {
            case GRASS -> depth == 0 ? Material.GRASS_BLOCK : depth < 4 ? Material.DIRT : Material.STONE;
            case COARSE_DIRT -> depth == 0 ? Material.COARSE_DIRT : depth < 4 ? Material.DIRT : Material.STONE;
            case TERRACOTTA -> depth < 5 ? Material.TERRACOTTA : Material.STONE;
            case RED_SAND -> depth == 0 ? Material.RED_SAND
                    : depth < 4 ? Material.RED_SANDSTONE : Material.STONE;
            case SAND -> depth == 0 ? Material.SAND : depth < 4 ? Material.SANDSTONE : Material.STONE;
            case GRAVEL -> depth == 0 ? Material.GRAVEL
                    : depth < 3 ? Material.COARSE_DIRT : Material.STONE;
            case STONE -> depth < 4 ? Material.STONE : Material.DEEPSLATE;
        };
    }

    @Override
    public @NotNull BiomeProvider getDefaultBiomeProvider(@NotNull WorldInfo worldInfo) {
        return new WorldGenBiomeProvider(seed, bounds);
    }

    @Override
    public int getBaseHeight(
            @NotNull WorldInfo worldInfo,
            @NotNull Random random,
            int x,
            int z,
            @NotNull HeightMap heightMap) {
        return bounds.contains(x, z) ? terrain.sample(x, z).height() + 1 : worldInfo.getMinHeight();
    }

    @Override public boolean shouldGenerateNoise() { return false; }
    @Override public boolean shouldGenerateSurface() { return false; }
    @Override public boolean shouldGenerateCaves() { return false; }
    @Override public boolean shouldGenerateStructures() { return false; }
    @Override public boolean shouldGenerateDecorations() { return POLICY.vanillaDecorations(); }
    @Override public boolean shouldGenerateMobs() { return POLICY.initialMobs(); }
}
