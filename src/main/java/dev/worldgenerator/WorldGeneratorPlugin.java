package dev.worldgenerator;

import dev.worldgenerator.biome.WorldGenBiomeProvider;
import dev.worldgenerator.terrain.SafeSpawnLocator;
import dev.worldgenerator.terrain.SpawnPoint;
import dev.worldgenerator.terrain.WorldGenChunkGenerator;
import io.papermc.paper.math.Position;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class WorldGeneratorPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        getLogger().info("WorldGenerator 0.2 terrain and biome generator enabled.");
    }

    @Override
    public @Nullable ChunkGenerator getDefaultWorldGenerator(
            @NotNull String worldName, @Nullable String id) {
        long seed = parseSeed(id, worldName.hashCode());
        return new WorldGenChunkGenerator(seed);
    }

    @Override
    public @Nullable BiomeProvider getDefaultBiomeProvider(
            @NotNull String worldName, @Nullable String id) {
        long seed = parseSeed(id, worldName.hashCode());
        return new WorldGenBiomeProvider(seed);
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        if (args.length < 2 || !args[0].equalsIgnoreCase("create")) {
            return handleNonCreateCommand(sender, label, args);
        }

        String name = args[1].toLowerCase(Locale.ROOT);
        if (!name.matches("[a-z0-9_-]{1,32}")) {
            sender.sendMessage("World name may only contain a-z, 0-9, _ and - (max 32). ");
            return true;
        }

        long seed = args.length >= 3 ? parseSeed(args[2], new Random().nextLong()) : new Random().nextLong();
        NamespacedKey key = new NamespacedKey(this, name);
        SpawnPoint spawn = new SafeSpawnLocator(seed).locate();
        WorldCreator creator = WorldCreator.ofKey(key)
                .seed(seed)
                .environment(World.Environment.NORMAL)
                .generateStructures(false)
                .generator(new WorldGenChunkGenerator(seed))
                .biomeProvider(new WorldGenBiomeProvider(seed))
                .forcedSpawnPosition(Position.block(spawn.x(), spawn.y(), spawn.z()), 0.0f, 0.0f);
        World world = creator.createWorld();
        if (world == null) {
            sender.sendMessage("World creation failed. Check the server log.");
            return true;
        }

        sender.sendMessage("Created world " + key + " with seed " + seed
                + " at safe spawn " + spawn.x() + ", " + spawn.y() + ", " + spawn.z() + ".");
        teleportPlayer(sender, world);
        return true;
    }

    @Override
    public @NotNull List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args) {
        if (!sender.hasPermission("worldgenerator.admin")) {
            return List.of();
        }

        if (args.length == 1) {
            return matchingPrefix(List.of("create", "tp", "lobby", "list"), args[0]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("tp")) {
            List<String> worldNames = new ArrayList<>();
            for (World world : Bukkit.getWorlds()) {
                String fullKey = world.key().asString();
                worldNames.add(fullKey);
                String ownNamespace = getName().toLowerCase(Locale.ROOT) + ":";
                if (fullKey.startsWith(ownNamespace)) {
                    worldNames.add(fullKey.substring(ownNamespace.length()));
                }
            }
            return matchingPrefix(worldNames, args[1]);
        }

        return List.of();
    }

    private static List<String> matchingPrefix(List<String> candidates, String input) {
        String prefix = input.toLowerCase(Locale.ROOT);
        return candidates.stream()
                .filter(candidate -> candidate.toLowerCase(Locale.ROOT).startsWith(prefix))
                .distinct()
                .sorted()
                .toList();
    }

    private boolean handleNonCreateCommand(CommandSender sender, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("list")) {
            String worlds = Bukkit.getWorlds().stream()
                    .map(world -> world.key().asString())
                    .collect(Collectors.joining(", "));
            sender.sendMessage("Loaded worlds: " + worlds);
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("lobby")) {
            if (Bukkit.getWorlds().isEmpty()) {
                sender.sendMessage("No lobby world is loaded.");
                return true;
            }
            teleportPlayer(sender, Bukkit.getWorlds().get(0));
            return true;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("tp")) {
            NamespacedKey key = parseWorldKey(args[1]);
            World world = key == null ? null : Bukkit.getWorld(key);
            if (world == null) {
                sender.sendMessage("World is not loaded: " + args[1]);
                return true;
            }
            teleportPlayer(sender, world);
            return true;
        }

        sender.sendMessage("Usage:");
        sender.sendMessage("/" + label + " create <name> [seed]");
        sender.sendMessage("/" + label + " tp <name|namespace:name>");
        sender.sendMessage("/" + label + " lobby");
        sender.sendMessage("/" + label + " list");
        return true;
    }

    private NamespacedKey parseWorldKey(String input) {
        if (input.contains(":")) {
            return NamespacedKey.fromString(input);
        }
        return new NamespacedKey(this, input.toLowerCase(Locale.ROOT));
    }

    private static void teleportPlayer(CommandSender sender, World world) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only a player can use teleport commands.");
            return;
        }
        Location spawn = world.getSpawnLocation().add(0.5, 1.0, 0.5);
        player.teleportAsync(spawn).thenAccept(success -> {
            if (success) {
                player.sendMessage("Teleported to " + world.key().asString() + ".");
            } else {
                player.sendMessage("Teleport failed.");
            }
        });
    }

    private static long parseSeed(@Nullable String value, long fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return value.hashCode();
        }
    }
}
