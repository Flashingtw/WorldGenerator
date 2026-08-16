package dev.worldgenerator.terrain;

/** Square terrain bounds centered on the world origin; zero means unlimited. */
public record WorldBounds(int size) {
    public static final WorldBounds UNLIMITED = new WorldBounds(0);

    public WorldBounds {
        if (size < 0) throw new IllegalArgumentException("size cannot be negative");
    }

    public boolean isLimited() {
        return size > 0;
    }

    public boolean contains(int x, int z) {
        if (!isLimited()) return true;
        int minimum = -size / 2;
        int maximumExclusive = minimum + size;
        return x >= minimum && x < maximumExclusive && z >= minimum && z < maximumExclusive;
    }

    public int spawnSearchRadius() {
        return isLimited() ? Math.max(0, size / 2 - 128) : 4_096;
    }
}
