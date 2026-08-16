package dev.worldgenerator;

import dev.worldgenerator.biome.WorldGenBiomeProvider;
import dev.worldgenerator.command.CommandSuggestions;
import dev.worldgenerator.command.PreviewCommandHandler;
import dev.worldgenerator.preview.PreviewWorldManager;
import dev.worldgenerator.terrain.SafeSpawnLocator;
import dev.worldgenerator.terrain.SpawnPoint;
import dev.worldgenerator.terrain.WorldGenChunkGenerator;
import dev.worldgenerator.terrain.WorldBounds;
import io.papermc.paper.math.Position;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
import org.bukkit.entity.SpawnCategory;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class WorldGeneratorPlugin extends JavaPlugin {
    private PreviewCommandHandler previewCommand;

    @Override
    public void onEnable() {
        previewCommand = new PreviewCommandHandler(new PreviewWorldManager(this));
        getLogger().info("WorldGenerator 0.7.2 natural slope relief enabled.");
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
        if (args.length >= 1 && args[0].equalsIgnoreCase("preview")) {
            return previewCommand.execute(sender, label, args);
        }
        if (args.length < 2 || !args[0].equalsIgnoreCase("create")) {
            return handleNonCreateCommand(sender, label, args);
        }

        String name = args[1].toLowerCase(Locale.ROOT);
        if (!name.matches("[a-z0-9_-]{1,32}")) {
            sender.sendMessage("World name may only contain a-z, 0-9, _ and - (max 32). ");
            return true;
        }

        if (args.length > 4) {
            sender.sendMessage("Usage: /" + label + " create <name> [seed] [size]");
            return true;
        }
        String seedArgument = null;
        String sizeArgument = null;
        if (args.length >= 3) {
            if (looksLikeSize(args[2])) sizeArgument = args[2];
            else seedArgument = args[2];
        }
        if (args.length == 4) sizeArgument = args[3];

        long seed = seedArgument == null || seedArgument.equalsIgnoreCase("random")
                ? new Random().nextLong()
                : parseSeed(seedArgument, new Random().nextLong());
        WorldBounds bounds;
        try {
            bounds = parseWorldBounds(sizeArgument);
        } catch (IllegalArgumentException exception) {
            sender.sendMessage(exception.getMessage());
            return true;
        }
        NamespacedKey key = new NamespacedKey(this, name);
        SpawnPoint spawn = new SafeSpawnLocator(seed, bounds).locate();
        WorldCreator creator = WorldCreator.ofKey(key)
                .seed(seed)
                .environment(World.Environment.NORMAL)
                .generateStructures(false)
                .generator(new WorldGenChunkGenerator(seed, bounds))
                .biomeProvider(new WorldGenBiomeProvider(seed, bounds))
                .forcedSpawnPosition(Position.block(spawn.x(), spawn.y(), spawn.z()), 0.0f, 0.0f);
        World world = creator.createWorld();
        if (world == null) {
            sender.sendMessage("World creation failed. Check the server log.");
            return true;
        }

        if (bounds.isLimited()) {
            world.getWorldBorder().setCenter(0.0, 0.0);
            world.getWorldBorder().setSize(bounds.size());
        }
        disableAnimalSpawning(world);

        sender.sendMessage("Created world " + key + " with seed " + seed
                + (bounds.isLimited() ? " size " + bounds.size() + "x" + bounds.size() : " (unlimited)")
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
            return CommandSuggestions.matchingPrefix(
                    List.of("create", "tp", "delete", "lobby", "list", "preview"), args[0]);
        }

        if (args[0].equalsIgnoreCase("preview")) return previewCommand.suggestions(args);

        if (args.length == 2
                && (args[0].equalsIgnoreCase("tp") || args[0].equalsIgnoreCase("delete"))) {
            List<String> worldNames = new ArrayList<>();
            for (World world : Bukkit.getWorlds()) {
                String fullKey = world.key().asString();
                worldNames.add(fullKey);
                String ownNamespace = getName().toLowerCase(Locale.ROOT) + ":";
                if (fullKey.startsWith(ownNamespace)) {
                    worldNames.add(fullKey.substring(ownNamespace.length()));
                }
            }
            return CommandSuggestions.matchingPrefix(worldNames, args[1]);
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("delete")) {
            return CommandSuggestions.matchingPrefix(List.of("confirm"), args[2]);
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("create")) {
            return CommandSuggestions.matchingPrefix(CommandSuggestions.createArgument(2), args[2]);
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("create")) {
            return CommandSuggestions.matchingPrefix(CommandSuggestions.createArgument(3), args[3]);
        }

        return List.of();
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

        if (args.length >= 2 && args[0].equalsIgnoreCase("delete")) {
            return deleteWorld(sender, args);
        }

        sender.sendMessage("Usage:");
        sender.sendMessage("/" + label + " create <name> [seed] [size]");
        sender.sendMessage("/" + label + " tp <name|namespace:name>");
        sender.sendMessage("/" + label + " delete <name> confirm");
        sender.sendMessage("/" + label + " lobby");
        sender.sendMessage("/" + label + " list");
        sender.sendMessage("/" + label + " preview [blueprint|rebuild|clear] [rotation] [mirror]");
        return true;
    }

    private boolean deleteWorld(CommandSender sender, String[] args) {
        NamespacedKey key = parseWorldKey(args[1]);
        if (key == null || !key.getNamespace().equals(getName().toLowerCase(Locale.ROOT))) {
            sender.sendMessage("Only WorldGenerator worlds can be deleted.");
            return true;
        }

        World world = Bukkit.getWorld(key);
        if (world == null) {
            sender.sendMessage("World must be loaded before it can be deleted: " + key);
            return true;
        }
        if (!world.getPlayers().isEmpty()) {
            sender.sendMessage("Cannot delete a world while players are inside it.");
            return true;
        }
        if (args.length != 3 || !args[2].equalsIgnoreCase("confirm")) {
            sender.sendMessage("This removes the world from the server.");
            sender.sendMessage("Confirm with: /wg delete " + key.getKey() + " confirm");
            return true;
        }

        Path source = world.getWorldFolder().toPath().toAbsolutePath().normalize();
        if (!Bukkit.unloadWorld(world, true)) {
            sender.sendMessage("World could not be unloaded, so nothing was deleted.");
            return true;
        }

        Path trashDirectory = getDataPath().resolve("trash");
        Path destination = trashDirectory.resolve(key.getKey() + "-" + System.currentTimeMillis());
        try {
            Files.createDirectories(trashDirectory);
            Files.move(source, destination);
            sender.sendMessage("Deleted " + key + ". Recovery copy: " + destination.getFileName());
        } catch (IOException exception) {
            getSLF4JLogger().error("Failed to move deleted world {} to trash", key, exception);
            sender.sendMessage("World was unloaded but could not be moved to trash. Its files remain at " + source);
        }
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

    private static void disableAnimalSpawning(World world) {
        for (SpawnCategory category : List.of(
                SpawnCategory.ANIMAL,
                SpawnCategory.WATER_ANIMAL,
                SpawnCategory.WATER_AMBIENT,
                SpawnCategory.WATER_UNDERGROUND_CREATURE,
                SpawnCategory.AXOLOTL,
                SpawnCategory.AMBIENT)) {
            world.setSpawnLimit(category, 0);
        }
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

    private static boolean looksLikeSize(String value) {
        return value.equalsIgnoreCase("unlimited") || value.matches("\\d+[xX]\\d+");
    }

    private static WorldBounds parseWorldBounds(@Nullable String value) {
        if (value == null || value.isBlank() || value.equalsIgnoreCase("unlimited")) {
            return WorldBounds.UNLIMITED;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        String[] dimensions = normalized.contains("x") ? normalized.split("x", -1) : new String[] {normalized, normalized};
        if (dimensions.length != 2 || !dimensions[0].equals(dimensions[1])) {
            throw new IllegalArgumentException("World size must be square, for example 5000x5000.");
        }
        try {
            int size = Integer.parseInt(dimensions[0]);
            if (size < 1_000 || size > 100_000) {
                throw new IllegalArgumentException("World size must be between 1000x1000 and 100000x100000.");
            }
            return new WorldBounds(size);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid world size: " + value);
        }
    }
}
