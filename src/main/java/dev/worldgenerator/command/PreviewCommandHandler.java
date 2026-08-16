package dev.worldgenerator.command;

import dev.worldgenerator.blueprint.BlueprintCatalog;
import dev.worldgenerator.blueprint.VoxelBlueprint;
import dev.worldgenerator.preview.PreviewWorldManager;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Parses the preview subcommand while the manager owns all world side effects. */
public final class PreviewCommandHandler {
    private final PreviewWorldManager previews;

    public PreviewCommandHandler(PreviewWorldManager previews) {
        this.previews = previews;
    }

    public boolean execute(CommandSender sender, String label, String[] args) {
        if (args.length >= 2 && args[1].equalsIgnoreCase("clear")) {
            previews.clear();
            sender.sendMessage("Cleared blueprint preview blocks in " + previews.worldKey() + ".");
            return true;
        }
        boolean rebuild = args.length >= 2 && args[1].equalsIgnoreCase("rebuild");
        String id = rebuild || args.length == 1
                ? BlueprintCatalog.ROTATION_LAB : args[1].toLowerCase(Locale.ROOT);
        int rotationIndex = 2;
        String rotationArgument = args.length > rotationIndex ? args[rotationIndex] : "0";
        int quarterTurns;
        try {
            quarterTurns = parseRotation(rotationArgument);
        } catch (IllegalArgumentException exception) {
            sender.sendMessage(exception.getMessage());
            sender.sendMessage("Usage: /" + label + " preview [blueprint|rebuild|clear] [0|90|180|270] [mirror]");
            return true;
        }
        boolean mirrored = args.length > rotationIndex + 1
                && args[rotationIndex + 1].equalsIgnoreCase("mirror");
        if (args.length > rotationIndex + 2) {
            sender.sendMessage("Usage: /" + label + " preview [blueprint|rebuild|clear] [0|90|180|270] [mirror]");
            return true;
        }
        try {
            VoxelBlueprint blueprint = previews.render(id, quarterTurns, mirrored);
            if (sender instanceof Player player) previews.teleport(player);
            sender.sendMessage("Previewed " + blueprint.id() + " at " + (quarterTurns * 90)
                    + " degrees" + (mirrored ? " mirrored" : "") + " in " + previews.worldKey() + ".");
        } catch (IOException | IllegalArgumentException exception) {
            sender.sendMessage("Could not load blueprint: " + exception.getMessage());
        }
        return true;
    }

    public List<String> suggestions(String[] args) {
        if (args.length == 2) {
            return CommandSuggestions.matchingPrefix(
                    List.of("rotation_lab", "rebuild", "clear"), args[1]);
        }
        if (args.length == 3) {
            return CommandSuggestions.matchingPrefix(List.of("0", "90", "180", "270"), args[2]);
        }
        if (args.length == 4) {
            return CommandSuggestions.matchingPrefix(List.of("mirror"), args[3]);
        }
        return List.of();
    }

    static int parseRotation(String value) {
        return switch (value) {
            case "0" -> 0;
            case "90" -> 1;
            case "180" -> 2;
            case "270" -> 3;
            default -> throw new IllegalArgumentException("Rotation must be 0, 90, 180, or 270.");
        };
    }
}
