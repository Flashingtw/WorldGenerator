package dev.worldgenerator.overview;

import dev.worldgenerator.biome.BiomeKind;
import dev.worldgenerator.biome.BiomeSampler;
import dev.worldgenerator.biome.SurfaceKind;
import dev.worldgenerator.biome.SurfaceSample;
import dev.worldgenerator.biome.SurfaceSampler;
import dev.worldgenerator.map.RoadKind;
import dev.worldgenerator.map.RoadNode;
import dev.worldgenerator.map.RoadSegment;
import dev.worldgenerator.terrain.HydrologyPlan;
import dev.worldgenerator.terrain.TerrainSample;
import dev.worldgenerator.terrain.TerrainSampler;
import dev.worldgenerator.terrain.WorldBounds;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import javax.imageio.ImageIO;

/** Produces a complete finite-world PNG directly from the deterministic samplers. */
public final class MapOverviewRenderer {
    public static final int DEFAULT_RESOLUTION = 1_024;

    private MapOverviewRenderer() {
    }

    public static void render(long seed, WorldBounds bounds, Path output) throws IOException {
        render(seed, bounds, DEFAULT_RESOLUTION, output);
    }

    public static void render(
            long seed, WorldBounds bounds, int resolution, Path output) throws IOException {
        if (!bounds.isLimited()) throw new IllegalArgumentException("Overview requires a finite world.");
        if (resolution < 128 || resolution > 2_048) {
            throw new IllegalArgumentException("Overview resolution must be between 128 and 2048.");
        }
        TerrainSampler terrain = new TerrainSampler(seed, bounds);
        BiomeSampler biomes = new BiomeSampler(seed, bounds);
        SurfaceSampler surfaces = new SurfaceSampler(seed);
        BufferedImage image = new BufferedImage(
                resolution, resolution, BufferedImage.TYPE_INT_RGB);
        for (int pixelX = 0; pixelX < resolution; pixelX++) {
            int x = coordinate(pixelX, resolution, bounds.size());
            for (int pixelZ = 0; pixelZ < resolution; pixelZ++) {
                int z = coordinate(pixelZ, resolution, bounds.size());
                TerrainSample sample = terrain.sample(x, z);
                BiomeKind biome = biomes.sample(x, sample.height(), z);
                SurfaceSample surface = surfaces.sample(x, z, biome, sample);
                image.setRGB(pixelX, pixelZ, color(sample, surface).getRGB());
            }
        }

        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        drawRivers(graphics, terrain.hydrology(), bounds.size(), resolution);
        for (RoadSegment road : terrain.plan().roads()) {
            drawRoad(graphics, road, bounds.size(), resolution, true);
        }
        for (RoadSegment road : terrain.plan().roads()) {
            drawRoad(graphics, road, bounds.size(), resolution, false);
        }
        for (var poi : terrain.plan().pointsOfInterest()) {
            int x = pixel(poi.x(), bounds.size(), resolution);
            int z = pixel(poi.z(), bounds.size(), resolution);
            int radius = Math.max(3, (int) Math.round(
                    poi.type().radius() * resolution / (double) bounds.size()));
            graphics.setColor(new Color(45, 43, 39));
            graphics.fillOval(x - radius, z - radius, radius * 2, radius * 2);
            graphics.setColor(new Color(226, 200, 125));
            graphics.drawOval(x - radius, z - radius, radius * 2, radius * 2);
        }
        graphics.setColor(new Color(225, 207, 144));
        graphics.setStroke(new BasicStroke(2.0f));
        graphics.drawRect(1, 1, resolution - 3, resolution - 3);
        graphics.dispose();

        Path absolute = output.toAbsolutePath().normalize();
        Path parent = absolute.getParent();
        if (parent == null) throw new IOException("Overview output has no parent directory.");
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, "overview-", ".png.tmp");
        try {
            if (!ImageIO.write(image, "png", temporary.toFile())) {
                throw new IOException("PNG image writer is unavailable.");
            }
            try {
                Files.move(temporary, absolute,
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, absolute, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void drawRivers(
            Graphics2D graphics, HydrologyPlan plan, int mapSize, int resolution) {
        graphics.setColor(new Color(35, 116, 184));
        for (HydrologyPlan.River river : plan.rivers()) {
            var nodes = river.centerline();
            for (int index = 1; index < nodes.size(); index++) {
                double progress = index / (nodes.size() - 1.0);
                double radius = river.headWidth()
                        + (river.mouthWidth() - river.headWidth()) * progress;
                float width = Math.max(1.5f,
                        (float) (radius * 2.0 * resolution / mapSize));
                graphics.setStroke(new BasicStroke(
                        width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                var from = nodes.get(index - 1);
                var to = nodes.get(index);
                graphics.drawLine(
                        pixel(from.x(), mapSize, resolution),
                        pixel(from.z(), mapSize, resolution),
                        pixel(to.x(), mapSize, resolution),
                        pixel(to.z(), mapSize, resolution));
            }
        }
    }

    private static void drawRoad(
            Graphics2D graphics, RoadSegment road,
            int mapSize, int resolution, boolean shoulder) {
        for (int index = 1; index < road.centerline().size(); index++) {
            RoadNode from = road.centerline().get(index - 1);
            RoadNode to = road.centerline().get(index);
            int middleX = (int) Math.round((from.x() + to.x()) * 0.5);
            int middleZ = (int) Math.round((from.z() + to.z()) * 0.5);
            RoadKind kind = road.kindAt(middleX, middleZ);
            double radius = shoulder ? kind.shoulderRadius() : kind.coreRadius();
            float width = Math.max(1.0f,
                    (float) (radius * 2.0 * resolution / mapSize));
            graphics.setStroke(new BasicStroke(
                    width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            graphics.setColor(shoulder ? new Color(78, 68, 54)
                    : switch (kind) {
                        case TRUNK -> new Color(211, 189, 142);
                        case BRANCH -> new Color(178, 153, 112);
                        case ACCESS -> new Color(148, 126, 95);
                    });
            graphics.drawLine(
                    pixel(from.x(), mapSize, resolution),
                    pixel(from.z(), mapSize, resolution),
                    pixel(to.x(), mapSize, resolution),
                    pixel(to.z(), mapSize, resolution));
        }
    }

    private static Color color(TerrainSample terrain, SurfaceSample surface) {
        if (terrain.underwater()) {
            int depth = Math.max(1, terrain.waterLevel() - terrain.height());
            return terrain.inlandWater()
                    ? new Color(30, clamp(113 - depth), clamp(176 - depth * 2))
                    : new Color(22, clamp(91 - depth), clamp(139 - depth));
        }
        if (terrain.mountainStrength() >= 0.46 || terrain.height() >= 108) {
            int stone = clamp(108 + (terrain.height() - 90) * 2);
            return new Color(stone, stone, clamp(stone - 7));
        }
        Color base = switch (surface.kind()) {
            case GRASS -> new Color(88, 124, 65);
            case COARSE_DIRT -> new Color(117, 91, 62);
            case TERRACOTTA -> new Color(153, 91, 67);
            case RED_SAND -> new Color(184, 103, 61);
            case SAND -> new Color(205, 190, 139);
            case GRAVEL -> new Color(131, 128, 121);
            case STONE -> new Color(122, 122, 118);
        };
        int shade = Math.max(-8, Math.min(18, terrain.height() - 72));
        return new Color(clamp(base.getRed() + shade),
                clamp(base.getGreen() + shade), clamp(base.getBlue() + shade / 2));
    }

    private static int coordinate(int pixel, int resolution, int mapSize) {
        return (int) Math.round((pixel + 0.5) / resolution * mapSize - mapSize / 2.0);
    }

    private static int pixel(double coordinate, int mapSize, int resolution) {
        return (int) Math.round((coordinate + mapSize / 2.0) / mapSize * resolution);
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
