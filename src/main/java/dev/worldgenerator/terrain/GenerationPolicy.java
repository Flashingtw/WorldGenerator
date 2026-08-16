package dev.worldgenerator.terrain;

/** Generation-stage policy kept explicit so tree and animal behavior is testable. */
public record GenerationPolicy(boolean vanillaDecorations, boolean initialMobs) {
    public static final GenerationPolicy BARREN = new GenerationPolicy(false, false);
}
