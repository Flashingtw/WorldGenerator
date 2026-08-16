package dev.worldgenerator.terrain;

/** Deterministic, allocation-free 2D value noise. */
public final class Noise2D {
    private final long seed;

    public Noise2D(long seed) {
        this.seed = seed;
    }

    public double sample(double x, double z) {
        int x0 = fastFloor(x);
        int z0 = fastFloor(z);
        double tx = fade(x - x0);
        double tz = fade(z - z0);

        double a = lerp(value(x0, z0), value(x0 + 1, z0), tx);
        double b = lerp(value(x0, z0 + 1), value(x0 + 1, z0 + 1), tx);
        return lerp(a, b, tz);
    }

    public double fractal(double x, double z, int octaves, double lacunarity, double persistence) {
        double value = 0.0;
        double amplitude = 1.0;
        double amplitudeSum = 0.0;
        for (int octave = 0; octave < octaves; octave++) {
            value += sample(x, z) * amplitude;
            amplitudeSum += amplitude;
            x *= lacunarity;
            z *= lacunarity;
            amplitude *= persistence;
        }
        return value / amplitudeSum;
    }

    private double value(int x, int z) {
        long hash = seed;
        hash ^= (long) x * 0x632BE59BD9B4E019L;
        hash ^= (long) z * 0x9E3779B97F4A7C15L;
        hash = (hash ^ (hash >>> 30)) * 0xBF58476D1CE4E5B9L;
        hash = (hash ^ (hash >>> 27)) * 0x94D049BB133111EBL;
        hash ^= hash >>> 31;
        return ((hash >>> 11) * 0x1.0p-53) * 2.0 - 1.0;
    }

    private static int fastFloor(double value) {
        int integer = (int) value;
        return value < integer ? integer - 1 : integer;
    }

    private static double fade(double value) {
        return value * value * value * (value * (value * 6.0 - 15.0) + 10.0);
    }

    private static double lerp(double a, double b, double amount) {
        return a + (b - a) * amount;
    }
}
