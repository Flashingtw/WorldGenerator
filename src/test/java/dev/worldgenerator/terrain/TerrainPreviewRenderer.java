package dev.worldgenerator.terrain;

import dev.worldgenerator.biome.BiomeKind;
import dev.worldgenerator.biome.BiomeSampler;
import dev.worldgenerator.biome.SurfaceKind;
import dev.worldgenerator.biome.SurfaceSample;
import dev.worldgenerator.biome.SurfaceSampler;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Manual visual-QA renderer kept beside the terrain acceptance tests. */
public final class TerrainPreviewRenderer {
    private static final int IMAGE_SIZE = 720;

    private TerrainPreviewRenderer() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) throw new IllegalArgumentException("Expected output directory");
        Path output = Path.of(args[0]);
        Files.createDirectories(output);
        render(output, "finite-5000-seed1", 1L, new WorldBounds(5_000), 5_000);
        render(output, "finite-5000-seed12345", 12_345L, new WorldBounds(5_000), 5_000);
        render(output, "finite-10000-seed965", 0x5C00A11L, new WorldBounds(10_000), 10_000);
        render(output, "unlimited-seed12345", 12_345L, WorldBounds.UNLIMITED, 24_000);
        renderSlopeCloseup(output, "slope-seed12345-x-720-z-400",
                12_345L, -720, -400);
    }

    private static void renderSlopeCloseup(
            Path output, String name, long seed, int centerX, int centerZ) throws IOException {
        BaseTerrainSampler terrain = new BaseTerrainSampler(seed, new WorldBounds(5_000));
        int size = 512;
        int[][] heights = new int[size + 4][size + 4];
        for (int x = 0; x < size + 4; x++) {
            for (int z = 0; z < size + 4; z++) {
                heights[x][z] = terrain.sample(
                        centerX + x - size / 2 - 2,
                        centerZ + z - size / 2 - 2).height();
            }
        }
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                int height = heights[x + 2][z + 2];
                int diagonalSlope = heights[x][z] - heights[x + 4][z + 4];
                int contour = height != heights[x + 3][z + 2]
                        || height != heights[x + 2][z + 3] ? -16 : 5;
                int light = clamp(104 + (height - 70) + diagonalSlope * 7 + contour);
                image.setRGB(x, z, new Color(
                        clamp(light - 28), light, clamp(light - 42)).getRGB());
            }
        }
        ImageIO.write(image, "png", output.resolve(name + ".png").toFile());
    }

    private static void render(
            Path output, String name, long seed, WorldBounds bounds, int viewedSize) throws IOException {
        BaseTerrainSampler terrain = new BaseTerrainSampler(seed, bounds);
        BiomeSampler biomes = new BiomeSampler(seed, bounds);
        SurfaceSampler surfaces = new SurfaceSampler(seed);
        BufferedImage relief = new BufferedImage(IMAGE_SIZE, IMAGE_SIZE, BufferedImage.TYPE_INT_RGB);
        BufferedImage heights = new BufferedImage(IMAGE_SIZE, IMAGE_SIZE, BufferedImage.TYPE_INT_RGB);
        BufferedImage surfaceMap = new BufferedImage(IMAGE_SIZE, IMAGE_SIZE, BufferedImage.TYPE_INT_RGB);
        for (int pixelX = 0; pixelX < IMAGE_SIZE; pixelX++) {
            int x = coordinate(pixelX, viewedSize);
            for (int pixelZ = 0; pixelZ < IMAGE_SIZE; pixelZ++) {
                int z = coordinate(pixelZ, viewedSize);
                TerrainSample sample = terrain.sample(x, z);
                BiomeKind biome = biomes.sample(x, sample.height(), z);
                SurfaceSample surface = surfaces.sample(x, z, biome, sample);
                relief.setRGB(pixelX, pixelZ, reliefColor(sample).getRGB());
                surfaceMap.setRGB(pixelX, pixelZ, surfaceColor(sample, surface).getRGB());
                int value = clamp((sample.height() - 36) * 255 / 132);
                heights.setRGB(pixelX, pixelZ, new Color(value, value, value).getRGB());
            }
        }
        ImageIO.write(relief, "png", output.resolve(name + "-relief.png").toFile());
        ImageIO.write(heights, "png", output.resolve(name + "-height.png").toFile());
        ImageIO.write(surfaceMap, "png", output.resolve(name + "-surface.png").toFile());
    }

    private static int coordinate(int pixel, int viewedSize) {
        return (int) Math.round((pixel + 0.5) / IMAGE_SIZE * viewedSize - viewedSize / 2.0);
    }

    private static Color reliefColor(TerrainSample sample) {
        int height = sample.height();
        if (height < TerrainSampler.SEA_LEVEL) {
            int depth = TerrainSampler.SEA_LEVEL - height;
            return new Color(24, clamp(91 - depth), clamp(132 - depth));
        }
        if (height <= TerrainSampler.SEA_LEVEL + 2) return new Color(194, 177, 126);
        if (sample.mountainStrength() >= 0.46) {
            int stone = clamp(104 + (height - 90) * 2);
            return new Color(stone, stone, clamp(stone - 5));
        }
        if (height >= 96) return new Color(112, 105, 74);
        int shade = clamp(height - 66);
        return new Color(83 + shade, 111 + shade, 68 + shade / 2);
    }

    private static Color surfaceColor(TerrainSample terrain, SurfaceSample surface) {
        if (terrain.height() < TerrainSampler.SEA_LEVEL) {
            int depth = TerrainSampler.SEA_LEVEL - terrain.height();
            return new Color(22, clamp(93 - depth), clamp(143 - depth));
        }
        Color base = switch (surface.kind()) {
            case GRASS -> new Color(91, 126, 63);
            case COARSE_DIRT -> new Color(119, 91, 60);
            case TERRACOTTA -> new Color(154, 91, 65);
            case RED_SAND -> new Color(183, 101, 57);
            case SAND -> new Color(205, 190, 137);
            case STONE -> new Color(123, 123, 119);
        };
        if (!surface.shortGrass() || surface.kind() != SurfaceKind.GRASS) return base;
        return new Color(clamp(base.getRed() + 12), clamp(base.getGreen() + 18), base.getBlue());
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
