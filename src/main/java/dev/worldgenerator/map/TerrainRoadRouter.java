package dev.worldgenerator.map;

import dev.worldgenerator.terrain.BaseTerrainSampler;
import dev.worldgenerator.terrain.PerlinNoise2D;
import dev.worldgenerator.terrain.TerrainSample;
import dev.worldgenerator.terrain.TerrainSampler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

/** Internal terrain-aware router. Its only result is a smooth, graded centerline. */
final class TerrainRoadRouter {
    private static final int GRID = 48;

    private TerrainRoadRouter() {
    }

    static List<RoadNode> route(
            long seed, int mapSize, BaseTerrainSampler terrain, RoadNode start, RoadNode goal) {
        SearchGrid grid = new SearchGrid(mapSize, terrain, start, goal);
        List<RoadNode> raw = grid.search();
        if (raw.isEmpty()) raw = fallback(terrain, start, goal);
        List<RoadNode> simplified = simplify(raw);
        List<RoadNode> organic = organicize(simplified, terrain, seed);
        List<RoadNode> rounded = roundedCorners(organic, terrain);
        List<RoadNode> geometry = usable(rounded, terrain) ? rounded
                : usable(organic, terrain) ? organic : simplified;
        return grade(geometry, terrain, start.y(), goal.y());
    }

    private static List<RoadNode> organicize(
            List<RoadNode> nodes, BaseTerrainSampler terrain, long seed) {
        if (nodes.size() <= 2) return nodes;
        PerlinNoise2D warp = new PerlinNoise2D(seed ^ 0xD1B54A32D192ED03L);
        List<RoadNode> result = new ArrayList<>();
        result.add(nodes.get(0));
        for (int index = 1; index < nodes.size() - 1; index++) {
            RoadNode node = nodes.get(index);
            double x = node.x() + warp.fractal(
                    node.x() / 210.0, node.z() / 210.0, 2, 2.0, 0.50) * 42.0;
            double z = node.z() + warp.fractal(
                    (node.x() + 4_091) / 210.0, (node.z() - 2_839) / 210.0,
                    2, 2.0, 0.50) * 42.0;
            result.add(onTerrain(terrain, new RoadNode(x, node.y(), z)));
        }
        result.add(nodes.get(nodes.size() - 1));
        return List.copyOf(result);
    }

    private static List<RoadNode> simplify(List<RoadNode> nodes) {
        if (nodes.size() <= 2) return nodes;
        List<RoadNode> result = new ArrayList<>();
        result.add(nodes.get(0));
        for (int index = 1; index < nodes.size() - 1; index++) {
            RoadNode previous = result.get(result.size() - 1);
            RoadNode current = nodes.get(index);
            RoadNode next = nodes.get(index + 1);
            double firstX = current.x() - previous.x();
            double firstZ = current.z() - previous.z();
            double secondX = next.x() - current.x();
            double secondZ = next.z() - current.z();
            double cross = Math.abs(firstX * secondZ - firstZ * secondX);
            double dot = firstX * secondX + firstZ * secondZ;
            if (cross > 1.0 || dot <= 0.0) result.add(current);
        }
        result.add(nodes.get(nodes.size() - 1));
        return List.copyOf(result);
    }

    private static List<RoadNode> roundedCorners(
            List<RoadNode> nodes, BaseTerrainSampler terrain) {
        if (nodes.size() <= 2) return nodes;
        List<RoadNode> result = new ArrayList<>();
        result.add(nodes.get(0));
        for (int index = 1; index < nodes.size(); index++) {
            RoadNode first = nodes.get(index - 1);
            RoadNode second = nodes.get(index);
            if (index > 1) result.add(onTerrain(terrain, interpolate(first, second, 0.22)));
            if (index < nodes.size() - 1) {
                result.add(onTerrain(terrain, interpolate(first, second, 0.78)));
            }
        }
        result.add(nodes.get(nodes.size() - 1));
        return List.copyOf(result);
    }

    private static RoadNode interpolate(RoadNode first, RoadNode second, double amount) {
        return new RoadNode(
                first.x() + (second.x() - first.x()) * amount,
                first.y() + (second.y() - first.y()) * amount,
                first.z() + (second.z() - first.z()) * amount);
    }

    private static RoadNode onTerrain(BaseTerrainSampler terrain, RoadNode node) {
        int height = terrain.sample((int) Math.round(node.x()), (int) Math.round(node.z())).height();
        return new RoadNode(node.x(), height, node.z());
    }

    private static boolean usable(List<RoadNode> nodes, BaseTerrainSampler terrain) {
        for (int index = 1; index < nodes.size(); index++) {
            RoadNode from = nodes.get(index - 1);
            RoadNode to = nodes.get(index);
            int samples = Math.max(1, (int) Math.ceil(from.horizontalDistanceTo(to) / 24.0));
            for (int step = 0; step <= samples; step++) {
                double amount = step / (double) samples;
                int x = (int) Math.round(from.x() + (to.x() - from.x()) * amount);
                int z = (int) Math.round(from.z() + (to.z() - from.z()) * amount);
                TerrainSample sample = terrain.sample(x, z);
                if (sample.height() <= TerrainSampler.SEA_LEVEL + 1
                        || sample.mountainStrength() >= 0.82) return false;
            }
        }
        return true;
    }

    private static List<RoadNode> grade(
            List<RoadNode> nodes, BaseTerrainSampler terrain,
            double startHeight, double goalHeight) {
        if (nodes.size() <= 2) return List.of(
                new RoadNode(nodes.get(0).x(), startHeight, nodes.get(0).z()),
                new RoadNode(nodes.get(nodes.size() - 1).x(), goalHeight,
                        nodes.get(nodes.size() - 1).z()));
        double[] heights = new double[nodes.size()];
        for (int index = 0; index < nodes.size(); index++) {
            double total = 0.0;
            int count = 0;
            for (int neighbor = Math.max(0, index - 1);
                    neighbor <= Math.min(nodes.size() - 1, index + 1); neighbor++) {
                RoadNode node = nodes.get(neighbor);
                total += terrain.sample(
                        (int) Math.round(node.x()), (int) Math.round(node.z())).height();
                count++;
            }
            heights[index] = total / count;
        }
        heights[0] = startHeight;
        heights[heights.length - 1] = goalHeight;
        for (int index = 1; index < heights.length - 1; index++) {
            double maximumChange = maximumChange(nodes, index - 1, index);
            heights[index] = Math.max(heights[index - 1] - maximumChange,
                    Math.min(heights[index - 1] + maximumChange, heights[index]));
        }
        for (int index = heights.length - 2; index > 0; index--) {
            double maximumChange = maximumChange(nodes, index, index + 1);
            heights[index] = Math.max(heights[index + 1] - maximumChange,
                    Math.min(heights[index + 1] + maximumChange, heights[index]));
        }

        List<RoadNode> result = new ArrayList<>();
        for (int index = 0; index < nodes.size(); index++) {
            RoadNode node = nodes.get(index);
            result.add(new RoadNode(node.x(), heights[index], node.z()));
        }
        return List.copyOf(result);
    }

    private static double maximumChange(List<RoadNode> nodes, int first, int second) {
        return Math.max(1.0,
                nodes.get(first).horizontalDistanceTo(nodes.get(second)) * 0.105);
    }

    private static List<RoadNode> fallback(
            BaseTerrainSampler terrain, RoadNode start, RoadNode goal) {
        List<RoadNode> result = new ArrayList<>();
        result.add(start);
        double dx = goal.x() - start.x();
        double dz = goal.z() - start.z();
        double length = Math.max(1.0, Math.hypot(dx, dz));
        double normalX = -dz / length;
        double normalZ = dx / length;
        double bend = Math.min(120.0, length * 0.12);
        for (int index = 1; index < 4; index++) {
            double amount = index / 4.0;
            double offset = Math.sin(amount * Math.PI) * bend;
            double x = start.x() + dx * amount + normalX * offset;
            double z = start.z() + dz * amount + normalZ * offset;
            result.add(onTerrain(terrain, new RoadNode(x, 0.0, z)));
        }
        result.add(goal);
        return List.copyOf(result);
    }

    private static final class SearchGrid {
        private final BaseTerrainSampler terrain;
        private final RoadNode start;
        private final RoadNode goal;
        private final int minimumGridX;
        private final int minimumGridZ;
        private final int width;
        private final int depth;
        private final double[] costs;
        private final int[] parents;
        private final TerrainSample[] samples;

        SearchGrid(int mapSize, BaseTerrainSampler terrain, RoadNode start, RoadNode goal) {
            this.terrain = terrain;
            this.start = start;
            this.goal = goal;
            double distance = start.horizontalDistanceTo(goal);
            double margin = Math.max(420.0, Math.min(1_150.0, distance * 0.42));
            int limit = mapSize / 2 - 64;
            int minimumX = (int) Math.max(-limit, Math.floor(Math.min(start.x(), goal.x()) - margin));
            int maximumX = (int) Math.min(limit, Math.ceil(Math.max(start.x(), goal.x()) + margin));
            int minimumZ = (int) Math.max(-limit, Math.floor(Math.min(start.z(), goal.z()) - margin));
            int maximumZ = (int) Math.min(limit, Math.ceil(Math.max(start.z(), goal.z()) + margin));
            minimumGridX = Math.floorDiv(minimumX, GRID);
            minimumGridZ = Math.floorDiv(minimumZ, GRID);
            width = Math.floorDiv(maximumX, GRID) - minimumGridX + 1;
            depth = Math.floorDiv(maximumZ, GRID) - minimumGridZ + 1;
            costs = new double[width * depth];
            Arrays.fill(costs, Double.POSITIVE_INFINITY);
            parents = new int[width * depth];
            Arrays.fill(parents, -1);
            samples = new TerrainSample[width * depth];
        }

        List<RoadNode> search() {
            int startIndex = index(nearestGridX(start.x()), nearestGridZ(start.z()));
            int goalIndex = index(nearestGridX(goal.x()), nearestGridZ(goal.z()));
            if (startIndex < 0 || goalIndex < 0) return List.of();
            PriorityQueue<State> pending = new PriorityQueue<>();
            costs[startIndex] = 0.0;
            pending.add(new State(startIndex, heuristic(startIndex)));
            boolean[] closed = new boolean[costs.length];
            int[] offsets = {-1, 0, 1};
            while (!pending.isEmpty()) {
                State state = pending.remove();
                if (closed[state.index()]) continue;
                if (state.index() == goalIndex) return reconstruct(goalIndex);
                closed[state.index()] = true;
                int localX = state.index() % width;
                int localZ = state.index() / width;
                for (int dx : offsets) {
                    for (int dz : offsets) {
                        if (dx == 0 && dz == 0) continue;
                        int nextX = localX + dx;
                        int nextZ = localZ + dz;
                        if (nextX < 0 || nextX >= width || nextZ < 0 || nextZ >= depth) continue;
                        int next = nextZ * width + nextX;
                        if (closed[next]) continue;
                        double candidate = costs[state.index()] + movementCost(state.index(), next);
                        if (candidate >= costs[next]) continue;
                        costs[next] = candidate;
                        parents[next] = state.index();
                        pending.add(new State(next, candidate + heuristic(next)));
                    }
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
            double slope = Math.abs(to.height() - from.height()) / distance;
            double cost = distance * (1.0 + slope * slope * 240.0
                    + to.mountainStrength() * to.mountainStrength() * 13.0);
            if (to.height() <= TerrainSampler.SEA_LEVEL + 1) cost += 12_000.0;
            if (to.height() > 98) cost += (to.height() - 98) * (to.height() - 98) * 2.2;
            return cost;
        }

        private double heuristic(int index) {
            int localX = index % width;
            int localZ = index / width;
            return Math.hypot(worldX(localX) - goal.x(), worldZ(localZ) - goal.z());
        }

        private TerrainSample sample(int index) {
            TerrainSample sample = samples[index];
            if (sample != null) return sample;
            int localX = index % width;
            int localZ = index / width;
            sample = terrain.sample(worldX(localX), worldZ(localZ));
            samples[index] = sample;
            return sample;
        }

        private List<RoadNode> reconstruct(int goalIndex) {
            List<RoadNode> reversed = new ArrayList<>();
            int current = goalIndex;
            while (current >= 0) {
                int localX = current % width;
                int localZ = current / width;
                TerrainSample sample = sample(current);
                reversed.add(new RoadNode(worldX(localX), sample.height(), worldZ(localZ)));
                current = parents[current];
            }
            Collections.reverse(reversed);
            List<RoadNode> result = new ArrayList<>();
            result.add(start);
            for (RoadNode node : reversed) {
                if (node.horizontalDistanceTo(start) > GRID * 0.65
                        && node.horizontalDistanceTo(goal) > GRID * 0.65) result.add(node);
            }
            result.add(goal);
            return List.copyOf(result);
        }

        private int nearestGridX(double worldX) {
            return (int) Math.round(worldX / GRID) - minimumGridX;
        }

        private int nearestGridZ(double worldZ) {
            return (int) Math.round(worldZ / GRID) - minimumGridZ;
        }

        private int index(int localX, int localZ) {
            if (localX < 0 || localX >= width || localZ < 0 || localZ >= depth) return -1;
            return localZ * width + localX;
        }

        private int worldX(int localX) {
            return (minimumGridX + localX) * GRID;
        }

        private int worldZ(int localZ) {
            return (minimumGridZ + localZ) * GRID;
        }

        private record State(int index, double estimatedTotal) implements Comparable<State> {
            @Override
            public int compareTo(State other) {
                int comparison = Double.compare(estimatedTotal, other.estimatedTotal);
                return comparison != 0 ? comparison : Integer.compare(index, other.index);
            }
        }
    }
}
