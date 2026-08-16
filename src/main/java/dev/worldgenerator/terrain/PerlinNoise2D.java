package dev.worldgenerator.terrain;

/** Deterministic gradient Perlin noise for organic boundaries and material patches. */
public final class PerlinNoise2D {
    private final long seed;

    public PerlinNoise2D(long seed) {
        this.seed = seed;
    }

    public double sample(double x, double z) {
        int x0 = fastFloor(x);
        int z0 = fastFloor(z);
        double tx = x - x0;
        double tz = z - z0;
        double u = fade(tx);
        double v = fade(tz);

        double a = gradientDot(x0, z0, tx, tz);
        double b = gradientDot(x0 + 1, z0, tx - 1.0, tz);
        double c = gradientDot(x0, z0 + 1, tx, tz - 1.0);
        double d = gradientDot(x0 + 1, z0 + 1, tx - 1.0, tz - 1.0);
        return lerp(lerp(a, b, u), lerp(c, d, u), v) * 0.7071067811865476;
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

    private double gradientDot(int x, int z, double dx, double dz) {
        long hash = mix(seed ^ (long) x * 0x632BE59BD9B4E019L
                ^ (long) z * 0x9E3779B97F4A7C15L);
        return switch ((int) (hash & 7L)) {
            case 0 -> dx;
            case 1 -> -dx;
            case 2 -> dz;
            case 3 -> -dz;
            case 4 -> (dx + dz) * 0.7071067811865476;
            case 5 -> (dx - dz) * 0.7071067811865476;
            case 6 -> (-dx + dz) * 0.7071067811865476;
            default -> (-dx - dz) * 0.7071067811865476;
        };
    }

    private static long mix(long value) {
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
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
