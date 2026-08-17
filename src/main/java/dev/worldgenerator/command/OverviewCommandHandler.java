package dev.worldgenerator.command;

import dev.worldgenerator.WorldGeneratorPlugin;
import dev.worldgenerator.overview.MapOverviewRenderer;
import dev.worldgenerator.overview.OverviewRequest;
import dev.worldgenerator.terrain.WorldBounds;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.command.CommandSender;

/** Parses overview requests and keeps expensive PNG rendering off the server thread. */
public final class OverviewCommandHandler {
    private final WorldGeneratorPlugin plugin;
    private final Set<String> activeJobs = ConcurrentHashMap.newKeySet();

    public OverviewCommandHandler(WorldGeneratorPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean execute(CommandSender sender, String label, String[] args) {
        if (args.length > 3) {
            sender.sendMessage("Usage: /" + label
                    + " overview [seed|random] [5000x5000|10000x10000]");
            return true;
        }
        long seed = args.length < 2 || args[1].equalsIgnoreCase("random")
                ? new Random().nextLong() : parseSeed(args[1]);
        int size;
        try {
            size = OverviewRequest.parseSize(args.length >= 3 ? args[2] : "5000x5000");
        } catch (IllegalArgumentException exception) {
            sender.sendMessage(exception.getMessage());
            return true;
        }
        String job = size + ":" + seed;
        if (!activeJobs.add(job)) {
            sender.sendMessage("That overview is already being generated.");
            return true;
        }
        String fileName = new OverviewRequest(seed, size).fileName();
        Path output = plugin.getDataPath().resolve("overviews").resolve(fileName);
        sender.sendMessage("Generating " + size + "x" + size + " overview for seed "
                + seed + " in the background...");
        long started = System.nanoTime();
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                MapOverviewRenderer.render(seed, new WorldBounds(size), output);
                double seconds = (System.nanoTime() - started) / 1_000_000_000.0;
                respond(sender, "Overview ready in " + String.format(Locale.ROOT, "%.1f", seconds)
                        + "s: " + output.toAbsolutePath().normalize());
            } catch (Exception exception) {
                plugin.getSLF4JLogger().error("Failed to render map overview", exception);
                respond(sender, "Overview generation failed. Check the server log.");
            } finally {
                activeJobs.remove(job);
            }
        });
        return true;
    }

    public List<String> suggestions(String[] args) {
        if (args.length == 2) {
            return CommandSuggestions.matchingPrefix(List.of("random"), args[1]);
        }
        if (args.length == 3) {
            return CommandSuggestions.matchingPrefix(
                    List.of("5000x5000", "10000x10000"), args[2]);
        }
        return List.of();
    }

    private static long parseSeed(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return value.hashCode();
        }
    }

    private void respond(CommandSender sender, String message) {
        plugin.getServer().getScheduler().runTask(plugin, () -> sender.sendMessage(message));
    }
}
