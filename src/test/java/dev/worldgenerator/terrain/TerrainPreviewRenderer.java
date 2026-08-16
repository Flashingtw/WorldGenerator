package dev.worldgenerator.terrain;

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
    }

    private static void render(
            Path output, String name, long seed, WorldBounds bounds, int viewedSize) throws IOException {
        BaseTerrainSampler terrain = new BaseTerrainSampler(seed, bounds);
        BufferedImage relief = new BufferedImage(IMAGE_SIZE, IMAGE_SIZE, BufferedImage.TYPE_INT_RGB);
        BufferedImage heights = new BufferedImage(IMAGE_SIZE, IMAGE_SIZE, BufferedImage.TYPE_INT_RGB);
        for (int pixelX = 0; pixelX < IMAGE_SIZE; pixelX++) {
            int x = coordinate(pixelX, viewedSize);
            for (int pixelZ = 0; pixelZ < IMAGE_SIZE; pixelZ++) {
                int z = coordinate(pixelZ, viewedSize);
                TerrainSample sample = terrain.sample(x, z);
                relief.setRGB(pixelX, pixelZ, reliefColor(sample).getRGB());
                int value = clamp((sample.height() - 36) * 255 / 132);
                heights.setRGB(pixelX, pixelZ, new Color(value, value, value).getRGB());
            }
        }
        ImageIO.write(relief, "png", output.resolve(name + "-relief.png").toFile());
        ImageIO.write(heights, "png", output.resolve(name + "-height.png").toFile());
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

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
