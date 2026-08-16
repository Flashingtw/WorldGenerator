package dev.worldgenerator.preview;

import dev.worldgenerator.WorldGeneratorPlugin;
import dev.worldgenerator.blueprint.BlueprintCatalog;
import dev.worldgenerator.blueprint.BlueprintEdit;
import dev.worldgenerator.blueprint.BlueprintPlacement;
import dev.worldgenerator.blueprint.VoxelBlueprint;
import io.papermc.paper.math.Position;
import java.io.IOException;
import org.bukkit.Difficulty;
import org.bukkit.GameRules;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;

/** Owns creation, clearing, rendering, and viewing of the isolated blueprint world. */
public final class PreviewWorldManager {
    private static final int ANCHOR_Y = PreviewChunkGenerator.FLOOR_Y + 1;
    private static final int CLEAR_RADIUS = 16;
    private static final int CLEAR_HEIGHT = 20;
    private final WorldGeneratorPlugin plugin;

    public PreviewWorldManager(WorldGeneratorPlugin plugin) {
        this.plugin = plugin;
    }

    public VoxelBlueprint render(String id, int quarterTurns, boolean mirrored) throws IOException {
        VoxelBlueprint blueprint = BlueprintCatalog.load(id);
        World world = world();
        clearBlocks(world);
        for (BlueprintEdit edit : blueprint.place(
                new BlueprintPlacement(0, ANCHOR_Y, 0, quarterTurns, mirrored))) {
            BukkitBlueprintAdapter.apply(world, edit);
        }
        return blueprint;
    }

    public void clear() {
        clearBlocks(world());
    }

    public void teleport(Player player) {
        World world = world();
        Location view = new Location(world, 0.5, ANCHOR_Y + 6.0, 15.5, 180.0f, 18.0f);
        player.teleportAsync(view);
    }

    public NamespacedKey worldKey() {
        return new NamespacedKey(plugin, "preview");
    }

    private World world() {
        NamespacedKey key = worldKey();
        World loaded = plugin.getServer().getWorld(key);
        if (loaded != null) return loaded;
        World created = WorldCreator.ofKey(key)
                .environment(World.Environment.NORMAL)
                .generateStructures(false)
                .generator(new PreviewChunkGenerator())
                .forcedSpawnPosition(Position.block(0, ANCHOR_Y + 1, 14), 180.0f, 0.0f)
                .createWorld();
        if (created == null) throw new IllegalStateException("Paper could not create the preview world");
        created.setDifficulty(Difficulty.PEACEFUL);
        created.setGameRule(GameRules.ADVANCE_TIME, false);
        created.setGameRule(GameRules.ADVANCE_WEATHER, false);
        created.setGameRule(GameRules.SPAWN_MOBS, false);
        created.setTime(6000L);
        created.setStorm(false);
        created.getWorldBorder().setCenter(0.0, 0.0);
        created.getWorldBorder().setSize(128.0);
        return created;
    }

    private static void clearBlocks(World world) {
        for (int x = -CLEAR_RADIUS; x <= CLEAR_RADIUS; x++) {
            for (int z = -CLEAR_RADIUS; z <= CLEAR_RADIUS; z++) {
                for (int y = ANCHOR_Y; y < ANCHOR_Y + CLEAR_HEIGHT; y++) {
                    world.getBlockAt(x, y, z).setType(Material.AIR, false);
                }
            }
        }
    }
}
