package dev.worldgenerator.overview;

import java.util.Locale;

/** Platform-neutral overview identity and argument rules. */
public record OverviewRequest(long seed, int size) {
    public static int parseSize(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "5000", "5000x5000" -> 5_000;
            case "10000", "10000x10000" -> 10_000;
            default -> throw new IllegalArgumentException(
                    "Overview size must be 5000x5000 or 10000x10000.");
        };
    }

    public String fileName() {
        String safeSeed = seed < 0 ? "m" + Long.toUnsignedString(-seed) : Long.toString(seed);
        return "overview-" + size + "x" + size + "-seed-" + safeSeed + ".png";
    }
}
