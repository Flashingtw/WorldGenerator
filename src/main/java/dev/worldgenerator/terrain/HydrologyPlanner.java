package dev.worldgenerator.terrain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/** Builds a small seeded drainage network from uplands to the finite-world ocean. */
public final class HydrologyPlanner {
    private static final int GRID = 64;
    private static final int[] NEIGHBOR_X = {-1, 0, 1, -1, 1, -1, 0, 1};
    private static final int[] NEIGHBOR_Z = {-1, -1, -1, 0, 0, 1, 1, 1};

    private HydrologyPlanner() {
    }

    public static HydrologyPlan create(long seed, WorldBounds bounds, TerrainSource terrain) {
        if (!bounds.isLimited()) return HydrologyPlan.empty();
        List<Source> sources = selectSources(seed, bounds.size(), terrain);
        List<HydrologyPlan.River> rivers = new ArrayList<>();
        PerlinNoise2D warp = new PerlinNoise2D(seed ^ 0x452821E638D01377L);
        for (int index = 0; index < sources.size(); index++) {
            Source source = sources.get(index);
            Point mouth = chooseMouth(seed, bounds.size(), terrain, source, index);
            List<Point> route = new DrainageGrid(
                    seed, index, bounds.size(), terrain, source, mouth).search();
            if (route.size() < 3) continue;
            List<HydrologyPlan.WaterNode> water = waterProfile(route, terrain, warp, index);
            double scale = bounds.size() >= 10_000 ? 1.18 : 1.0;
            rivers.add(new HydrologyPlan.River(water,
                    4.2 * scale, (9.0 + index % 3) * scale,
                    2.2, 4.0 + index % 2, 101 + index * 37));
        }
        List<HydrologyPlan.Lake> lakes = placeLakes(seed, bounds.size(), rivers);
        return new HydrologyPlan(seed, rivers, lakes);
    }

    private static List<Source> selectSources(long seed, int size, TerrainSource terrain) {
        int half = size / 2;
        int margin = Math.max(520, size / 12);
        int step = Math.max(180, size / 32);
        List<Source> candidates = new ArrayList<>();
        for (int x = -half + margin; x <= half - margin; x += step) {
            for (int z = -half + margin; z <= half - margin; z += step) {
                int jitter = step / 3;
                int px = x + signedInt(seed, x, z, 17, jitter);
                int pz = z + signedInt(seed, x, z, 31, jitter);
                TerrainSample sample = terrain.sample(px, pz);
                if (sample.height() < 88 || sample.height() > 154
                        || sample.continentalness() < 0.18
                        || sample.mountainStrength() > 0.82) continue;
                double score = sample.height() + sample.mountainStrength() * 28.0
                        + unit(seed, px, pz, 47) * 18.0;
                candidates.add(new Source(px, pz, score));
            }
        }
        candidates.sort(Comparator.comparingDouble(Source::score).reversed());
        int target = size >= 10_000 ? 6 : 4;
        double separation = size >= 10_000 ? 1_450.0 : 920.0;
        List<Source> selected = new ArrayList<>();
        for (Source candidate : candidates) {
            if (selected.stream().anyMatch(source -> Math.hypot(
                    source.x() - candidate.x(), source.z() - candidate.z()) < separation)) continue;
            selected.add(candidate);
            if (selected.size() == target) break;
        }
        return selected;
    }

    private static Point chooseMouth(
            long seed, int size, TerrainSource terrain, Source source, int index) {
        double baseAngle = Math.atan2(source.z(), source.x());
        if (Math.hypot(source.x(), source.z()) < size * 0.12) {
            baseAngle = unit(seed, source.x(), source.z(), 61 + index) * Math.PI * 2.0;
        }
        double angle = baseAngle + signedUnit(seed, source.x(), source.z(), 73 + index) * 0.72;
        int radius = size / 2 - GRID;
        Point best = new Point(
                (int) Math.round(Math.cos(angle) * radius),
                (int) Math.round(Math.sin(angle) * radius));
        double bestCost = Double.POSITIVE_INFINITY;
        for (int offset = -5; offset <= 5; offset++) {
            double candidateAngle = angle + offset * 0.085;
            int x = (int) Math.round(Math.cos(candidateAngle) * radius);
            int z = (int) Math.round(Math.sin(candidateAngle) * radius);
            TerrainSample sample = terrain.sample(x, z);
            double cost = sample.height() * 12.0
                    + Math.hypot(x - source.x(), z - source.z()) * 0.03
                    + Math.abs(offset) * 8.0;
            if (cost < bestCost) {
                bestCost = cost;
                best = new Point(x, z);
            }
        }
        return best;
    }

    private static List<HydrologyPlan.WaterNode> waterProfile(
            List<Point> route, TerrainSource terrain, PerlinNoise2D warp, int riverIndex) {
        route = smoothRoute(route, warp, riverIndex);
        List<Point> coastalRoute = new ArrayList<>();
        for (Point point : route) {
            coastalRoute.add(point);
            if (coastalRoute.size() > 3
                    && terrain.sample(point.x(), point.z()).height()
                    <= TerrainSampler.SEA_LEVEL + 1) break;
        }
        if (coastalRoute.size() >= 3) route = List.copyOf(coastalRoute);
        List<Point> organic = new ArrayList<>();
        for (int index = 0; index < route.size(); index++) {
            Point point = route.get(index);
            if (index == 0 || index == route.size() - 1) {
                organic.add(point);
                continue;
            }
            Point before = route.get(index - 1);
            Point after = route.get(index + 1);
            double dx = after.x() - before.x();
            double dz = after.z() - before.z();
            double length = Math.max(1.0, Math.hypot(dx, dz));
            double mountain = terrain.sample(point.x(), point.z()).mountainStrength();
            double amplitude = 18.0 + (1.0 - Math.min(1.0, mountain / 0.52)) * 42.0;
            double offset = warp.fractal(
                    (point.x() + riverIndex * 911) / 310.0,
                    (point.z() - riverIndex * 577) / 310.0,
                    3, 2.0, 0.50) * amplitude;
            organic.add(new Point(
                    (int) Math.round(point.x() - dz / length * offset),
                    (int) Math.round(point.z() + dx / length * offset)));
        }

        List<HydrologyPlan.WaterNode> result = new ArrayList<>();
        double firstNatural = terrain.sample(organic.get(0).x(), organic.get(0).z()).height() - 2.0;
        double previous = Math.max(TerrainSampler.SEA_LEVEL + 3.0, firstNatural);
        boolean waterfallPlaced = false;
        for (int index = 0; index < organic.size(); index++) {
            Point point = organic.get(index);
            double progress = index / (organic.size() - 1.0);
            double natural = terrain.sample(point.x(), point.z()).height() - 1.0;
            double profileCeiling = firstNatural
                    + (TerrainSampler.SEA_LEVEL - firstNatural) * progress + 5.0;
            double desired = Math.min(previous, Math.min(natural, profileCeiling));
            double maximumDrop = 1.0;
            if (!waterfallPlaced && previous >= TerrainSampler.SEA_LEVEL + 12.0
                    && previous - natural >= 7.0 && index > organic.size() / 8
                    && index < organic.size() * 7 / 8) {
                maximumDrop = Math.min(7.0, previous - natural);
                waterfallPlaced = true;
            }
            double level = Math.max(desired, previous - maximumDrop);
            level = Math.max(TerrainSampler.SEA_LEVEL, level);
            if (index == organic.size() - 1) level = TerrainSampler.SEA_LEVEL;
            result.add(new HydrologyPlan.WaterNode(point.x(), point.z(), Math.floor(level)));
            previous = level;
        }
        return List.copyOf(result);
    }

    private static List<Point> smoothRoute(
            List<Point> route, PerlinNoise2D warp, int riverIndex) {
        List<Point> result = new ArrayList<>();
        for (int segment = 0; segment < route.size() - 1; segment++) {
            Point p0 = route.get(Math.max(0, segment - 1));
            Point p1 = route.get(segment);
            Point p2 = route.get(segment + 1);
            Point p3 = route.get(Math.min(route.size() - 1, segment + 2));
            int samples = Math.max(2,
                    (int) Math.ceil(Math.hypot(p2.x() - p1.x(), p2.z() - p1.z()) / 16.0));
            for (int step = segment == 0 ? 0 : 1; step <= samples; step++) {
                double amount = step / (double) samples;
                double x = catmull(p0.x(), p1.x(), p2.x(), p3.x(), amount);
                double z = catmull(p0.z(), p1.z(), p2.z(), p3.z(), amount);
                double dx = p2.x() - p1.x();
                double dz = p2.z() - p1.z();
                double length = Math.max(1.0, Math.hypot(dx, dz));
                double envelope = Math.sin(Math.PI * amount);
                double bend = warp.fractal(
                        (x + 3_101 + riverIndex * 733) / 185.0,
                        (z - 1_997 - riverIndex * 419) / 185.0,
                        3, 2.0, 0.52) * 11.0 * envelope;
                result.add(new Point(
                        (int) Math.round(x - dz / length * bend),
                        (int) Math.round(z + dx / length * bend)));
            }
        }
        return List.copyOf(result);
    }

    private static double catmull(
            double p0, double p1, double p2, double p3, double amount) {
        double amount2 = amount * amount;
        double amount3 = amount2 * amount;
        return 0.5 * ((2.0 * p1) + (-p0 + p2) * amount
                + (2.0 * p0 - 5.0 * p1 + 4.0 * p2 - p3) * amount2
                + (-p0 + 3.0 * p1 - 3.0 * p2 + p3) * amount3);
    }

    private static List<HydrologyPlan.Lake> placeLakes(
            long seed, int size, List<HydrologyPlan.River> rivers) {
        int target = size >= 10_000 ? 3 : 2;
        List<HydrologyPlan.Lake> lakes = new ArrayList<>();
        for (int riverIndex = 0; riverIndex < rivers.size() && lakes.size() < target; riverIndex++) {
            List<HydrologyPlan.WaterNode> nodes = rivers.get(riverIndex).centerline();
            int bestIndex = -1;
            long bestPriority = Long.MAX_VALUE;
            for (int index = Math.max(2, nodes.size() / 4);
                    index < Math.min(nodes.size() - 2, nodes.size() * 3 / 4); index++) {
                HydrologyPlan.WaterNode before = nodes.get(index - 1);
                HydrologyPlan.WaterNode node = nodes.get(index);
                HydrologyPlan.WaterNode after = nodes.get(index + 1);
                if (node.waterLevel() <= TerrainSampler.SEA_LEVEL + 4
                        || before.waterLevel() - after.waterLevel() > 2.0) continue;
                long priority = hash(seed, (int) node.x(), (int) node.z(), 89 + riverIndex);
                if (priority < bestPriority) {
                    bestPriority = priority;
                    bestIndex = index;
                }
            }
            if (bestIndex < 0) continue;
            HydrologyPlan.WaterNode node = nodes.get(bestIndex);
            double radius = (size >= 10_000 ? 108.0 : 78.0)
                    + unit(seed, (int) node.x(), (int) node.z(), 103) * 42.0;
            lakes.add(new HydrologyPlan.Lake(
                    node.x(), node.z(), radius * 1.35, radius * 0.82,
                    (int) node.waterLevel(), 4.0 + radius / 30.0,
                    unit(seed, (int) node.x(), (int) node.z(), 107) * Math.PI));
        }
        return List.copyOf(lakes);
    }

    private static int signedInt(long seed, int x, int z, int salt, int magnitude) {
        return (int) Math.floorMod(hash(seed, x, z, salt), magnitude * 2L + 1L) - magnitude;
    }

    private static double signedUnit(long seed, int x, int z, int salt) {
        return unit(seed, x, z, salt) * 2.0 - 1.0;
    }

    private static double unit(long seed, int x, int z, int salt) {
        return (hash(seed, x, z, salt) >>> 11) * 0x1.0p-53;
    }

    private static long hash(long seed, int x, int z, int salt) {
        long value = seed ^ ((long) x * 0x9E3779B97F4A7C15L)
                ^ ((long) z * 0xC2B2AE3D27D4EB4FL) ^ salt;
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }

    private record Source(int x, int z, double score) {
    }

    private record Point(int x, int z) {
    }

    private static final class DrainageGrid {
        private final TerrainSource terrain;
        private final int min;
        private final int width;
        private final int start;
        private final int goal;
        private final TerrainSample[] samples;
        private final PerlinNoise2D routeBias;

        DrainageGrid(
                long seed, int riverIndex, int mapSize,
                TerrainSource terrain, Source source, Point mouth) {
            this.terrain = terrain;
            routeBias = new PerlinNoise2D(seed ^ (0xBE5466CF34E90C6CL + riverIndex * 0x9E3779B9L));
            min = -mapSize / 2 + GRID;
            int max = mapSize / 2 - GRID;
            width = (max - min) / GRID + 1;
            start = index(local(source.x()), local(source.z()));
            goal = index(local(mouth.x()), local(mouth.z()));
            samples = new TerrainSample[width * width];
        }

        List<Point> search() {
            double[] costs = new double[width * width];
            Arrays.fill(costs, Double.POSITIVE_INFINITY);
            int[] previous = new int[width * width];
            Arrays.fill(previous, -1);
            PriorityQueue<SearchNode> open = new PriorityQueue<>(
                    Comparator.comparingDouble(SearchNode::priority));
            costs[start] = 0.0;
            open.add(new SearchNode(start, heuristic(start)));
            while (!open.isEmpty()) {
                SearchNode current = open.poll();
                if (current.priority() > costs[current.index()] + heuristic(current.index()) + 0.001) continue;
                if (current.index() == goal) return reconstruct(previous);
                int x = current.index() % width;
                int z = current.index() / width;
                for (int direction = 0; direction < NEIGHBOR_X.length; direction++) {
                    int nextX = x + NEIGHBOR_X[direction];
                    int nextZ = z + NEIGHBOR_Z[direction];
                    if (nextX < 0 || nextZ < 0 || nextX >= width || nextZ >= width) continue;
                    int next = index(nextX, nextZ);
                    double candidate = costs[current.index()] + movementCost(current.index(), next);
                    if (candidate >= costs[next]) continue;
                    costs[next] = candidate;
                    previous[next] = current.index();
                    open.add(new SearchNode(next, candidate + heuristic(next)));
                }
            }
            return List.of();
        }

        private double movementCost(int fromIndex, int toIndex) {
            TerrainSample from = sample(fromIndex);
            TerrainSample to = sample(toIndex);
            int fromX = fromIndex % width;
            int fromZ = fromIndex / width;
            int toX = toIndex % width;
            int toZ = toIndex / width;
            double distance = Math.hypot(toX - fromX, toZ - fromZ) * GRID;
            double uphill = Math.max(0.0, to.height() - from.height());
            double cost = distance + uphill * uphill * 72.0;
            cost += Math.max(0.0, to.height() - 112.0) * 10.0;
            cost += to.mountainStrength() * to.mountainStrength() * 150.0;
            double bend = routeBias.fractal(
                    world(toX) / 410.0, world(toZ) / 410.0,
                    3, 2.0, 0.50);
            cost += distance * (bend + 1.0) * 0.34;
            return cost;
        }

        private double heuristic(int candidate) {
            int x = candidate % width;
            int z = candidate / width;
            int goalX = goal % width;
            int goalZ = goal / width;
            return Math.hypot(x - goalX, z - goalZ) * GRID;
        }

        private TerrainSample sample(int candidate) {
            if (samples[candidate] == null) {
                samples[candidate] = terrain.sample(world(candidate % width), world(candidate / width));
            }
            return samples[candidate];
        }

        private List<Point> reconstruct(int[] previous) {
            List<Point> reversed = new ArrayList<>();
            int current = goal;
            while (current >= 0) {
                reversed.add(new Point(world(current % width), world(current / width)));
                if (current == start) break;
                current = previous[current];
            }
            if (reversed.get(reversed.size() - 1).x() != world(start % width)
                    || reversed.get(reversed.size() - 1).z() != world(start / width)) return List.of();
            Collections.reverse(reversed);
            return List.copyOf(reversed);
        }

        private int local(int coordinate) {
            return Math.max(0, Math.min(width - 1, (int) Math.round((coordinate - min) / (double) GRID)));
        }

        private int world(int coordinate) {
            return min + coordinate * GRID;
        }

        private int index(int x, int z) {
            return z * width + x;
        }
    }

    private record SearchNode(int index, double priority) {
    }
}
