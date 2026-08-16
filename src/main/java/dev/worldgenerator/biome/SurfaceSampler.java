package dev.worldgenerator.biome;

import dev.worldgenerator.terrain.PerlinNoise2D;
import dev.worldgenerator.terrain.TerrainSample;
import dev.worldgenerator.terrain.TerrainSampler;

/** Blends biome topsoil through broad Perlin patches without enabling vanilla trees. */
public final class SurfaceSampler {
    private final ClimateRegionSampler climate;
    private final PerlinNoise2D broadPatches;
    private final PerlinNoise2D finePatches;
    private final PerlinNoise2D geology;
    private final PerlinNoise2D groundCover;

    public SurfaceSampler(long seed) {
        climate = new ClimateRegionSampler(seed);
        broadPatches = new PerlinNoise2D(seed ^ 0x6A09E667F3BCC909L);
        finePatches = new PerlinNoise2D(seed ^ 0xBB67AE8584CAA73BL);
        geology = new PerlinNoise2D(seed ^ 0x510E527FADE682D1L);
        groundCover = new PerlinNoise2D(seed ^ 0x3C6EF372FE94F82BL);
    }

    public SurfaceSample sample(int x, int z, BiomeKind biome, TerrainSample terrain) {
        if (terrain.waterfallStrength() > 0.12) {
            return new SurfaceSample(SurfaceKind.STONE, false);
        }
        if (terrain.riverStrength() > 0.12 || terrain.lakeStrength() > 0.12
                || terrain.shoreStrength() > 0.16) {
            return new SurfaceSample(SurfaceKind.GRAVEL, false);
        }
        if (biome.sandySurface() && terrain.height() <= TerrainSampler.SEA_LEVEL + 2) {
            return new SurfaceSample(SurfaceKind.SAND, false);
        }
        if (biome.stonySurface()) return new SurfaceSample(SurfaceKind.STONE, false);

        ClimateSample climateSample = climate.sample(x, z);
        double temperature = climateSample.temperature()
                - Math.max(0, terrain.height() - 90) / 95.0;
        double aridity = 0.18;
        aridity += broadPatches.fractal(x / 1_350.0, z / 1_350.0, 3, 2.0, 0.52) * 2.10;
        aridity += (temperature - climateSample.humidity()) * 0.20;
        aridity += finePatches.sample(x / 150.0, z / 150.0) * 0.14;

        SurfaceKind kind;
        if (aridity >= 0.62) {
            double sediment = geology.fractal(
                    (x + 2_117) / 760.0, (z - 1_309) / 760.0, 3, 2.0, 0.50);
            if (sediment > -0.08) {
                kind = finePatches.sample((x + 1_913) / 46.0, (z - 733) / 46.0) > 0.20
                        ? SurfaceKind.RED_SAND : SurfaceKind.TERRACOTTA;
            } else {
                kind = SurfaceKind.SAND;
            }
        } else if (aridity >= 0.38) {
            kind = SurfaceKind.COARSE_DIRT;
        } else {
            kind = SurfaceKind.GRASS;
        }

        boolean acceptsCover = kind == SurfaceKind.GRASS
                && terrain.height() > TerrainSampler.SEA_LEVEL + 2
                && terrain.height() < 108
                && terrain.roadStrength() < 0.05
                && terrain.poiStrength() < 0.05
                && !terrain.underwater()
                && terrain.riverStrength() < 0.05
                && terrain.lakeStrength() < 0.05
                && biome != BiomeKind.SNOWY_PLAINS
                && biome != BiomeKind.GROVE;
        boolean shortGrass = acceptsCover
                && groundCover.fractal(x / 8.0, z / 8.0, 2, 2.0, 0.45) > 0.13;
        return new SurfaceSample(kind, shortGrass);
    }

}
