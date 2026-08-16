package dev.worldgenerator.command;

import java.util.List;
import java.util.Locale;

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

    public static List<String> matchingPrefix(List<String> candidates, String input) {
        String prefix = input.toLowerCase(Locale.ROOT);
        return candidates.stream()
                .filter(candidate -> candidate.toLowerCase(Locale.ROOT).startsWith(prefix))
                .distinct()
                .sorted()
                .toList();
    }
}
