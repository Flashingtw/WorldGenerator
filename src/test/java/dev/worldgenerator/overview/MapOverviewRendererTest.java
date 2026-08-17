package dev.worldgenerator.overview;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import dev.worldgenerator.terrain.WorldBounds;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MapOverviewRendererTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void overviewIsACompleteDeterministicSeededPng() throws Exception {
        Path first = temporaryDirectory.resolve("first.png");
        Path repeated = temporaryDirectory.resolve("repeated.png");
        Path different = temporaryDirectory.resolve("different.png");
        WorldBounds bounds = new WorldBounds(5_000);
        MapOverviewRenderer.render(12_345L, bounds, 160, first);
        MapOverviewRenderer.render(12_345L, bounds, 160, repeated);
        MapOverviewRenderer.render(12_346L, bounds, 160, different);

        BufferedImage image = ImageIO.read(first.toFile());
        assertEquals(160, image.getWidth());
        assertEquals(160, image.getHeight());
        assertArrayEquals(Files.readAllBytes(first), Files.readAllBytes(repeated));
        assertFalse(java.util.Arrays.equals(
                Files.readAllBytes(first), Files.readAllBytes(different)));
    }
}
