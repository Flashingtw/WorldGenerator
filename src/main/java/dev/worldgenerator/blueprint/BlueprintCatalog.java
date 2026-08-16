package dev.worldgenerator.blueprint;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Loads the small approved set of authored blueprints bundled with the plugin. */
public final class BlueprintCatalog {
    public static final String ROTATION_LAB = "rotation_lab";
    private static final List<String> IDS = List.of(ROTATION_LAB);

    private BlueprintCatalog() {
    }

    public static List<String> ids() {
        return IDS;
    }

    public static VoxelBlueprint load(String id) throws IOException {
        if (!IDS.contains(id)) throw new IOException("Unknown blueprint: " + id);
        String resource = "/blueprints/" + id + ".vbp";
        try (InputStream stream = BlueprintCatalog.class.getResourceAsStream(resource)) {
            if (stream == null) throw new IOException("Missing bundled blueprint: " + resource);
            return VoxelBlueprint.read(new InputStreamReader(stream, StandardCharsets.UTF_8));
        }
    }
}
