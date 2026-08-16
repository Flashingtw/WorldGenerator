package dev.worldgenerator.command;

import java.util.List;

public final class CommandSuggestions {
    private CommandSuggestions() {
    }

    public static List<String> createArgument(int argumentPosition) {
        return switch (argumentPosition) {
            case 2 -> List.of("random");
            case 3 -> List.of("5000x5000", "10000x10000", "unlimited");
            default -> List.of();
        };
    }
}
