package dev.worldgenerator.map;

import dev.worldgenerator.terrain.BaseTerrainSampler;
import dev.worldgenerator.terrain.TerrainSample;
import dev.worldgenerator.terrain.TerrainSampler;
import dev.worldgenerator.terrain.WorldBounds;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Plans deterministic build pads and a connected low-cost road network. */
public final class AdventureMapPlanner {
    private AdventureMapPlanner() {
    }

    public static AdventureMapPlan create(long seed, WorldBounds bounds, BaseTerrainSampler terrain) {
        if (!bounds.isLimited()) return AdventureMapPlan.empty();
        List<Candidate> candidates = findCandidates(seed, bounds.size(), terrain);
        List<MapPoi> points = selectPoints(candidates, bounds.size());
        return new AdventureMapPlan(seed, points, connect(seed, bounds.size(), points, terrain));
    }

    private static List<Candidate> findCandidates(long seed, int size, BaseTerrainSampler terrain) {
        int margin = Math.max(360, size / 14);
        int radius = size / 2 - margin;
        int step = Math.max(260, size / 20);
        List<Candidate> result = new ArrayList<>();
        for (int x = -radius; x <= radius; x += step) {
            for (int z = -radius; z <= radius; z += step) {
                int jitter = Math.max(1, step / 3);
                int px = x + signedInt(seed, x, z, 11, jitter);
                int pz = z + signedInt(seed, x, z, 29, jitter);
                TerrainSample center = terrain.sample(px, pz);
                if (center.height() < TerrainSampler.SEA_LEVEL + 6 || center.height() > 108
                        || center.mountainStrength() > 0.58) continue;
                int slope = 0;
                for (int dx : new int[] {-28, 28}) {
                    for (int dz : new int[] {-28, 28}) {
                        slope = Math.max(slope,
                                Math.abs(center.height() - terrain.sample(px + dx, pz + dz).height()));
                    }
                }
                if (slope <= 9) {
                    result.add(new Candidate(px, center.height(), pz, hash(seed, px, pz, 43)));
                }
            }
        }
        result.sort(Comparator.comparingLong(Candidate::priority));
        return result;
    }

    private static List<MapPoi> selectPoints(List<Candidate> candidates, int size) {
        int target = Math.max(14, size / 220);
        double minimumDistance = Math.max(330.0, size / 18.0);
        List<MapPoi> result = new ArrayList<>();
        for (Candidate candidate : candidates) {
            boolean separated = result.stream().allMatch(point ->
                    Math.hypot(point.x() - candidate.x(), point.z() - candidate.z()) >= minimumDistance);
            if (!separated) continue;
            int index = result.size();
            PoiType type = index % 9 == 0 ? PoiType.LARGE : index % 3 == 0 ? PoiType.MEDIUM : PoiType.SMALL;
            result.add(new MapPoi(candidate.x(), candidate.y(), candidate.z(), type));
            if (result.size() >= target) break;
        }
        return List.copyOf(result);
    }

    private static List<RoadSegment> connect(
            long seed, int mapSize, List<MapPoi> points, BaseTerrainSampler terrain) {
        if (points.size() < 2) return List.of();
        List<Edge> selected = new ArrayList<>();
        Set<Integer> connected = new HashSet<>();
        Set<Long> edges = new HashSet<>();
        connected.add(0);
        while (connected.size() < points.size()) {
            double bestCost = Double.POSITIVE_INFINITY;
            int bestFrom = -1;
            int bestTo = -1;
            for (int from : connected) {
                for (int to = 0; to < points.size(); to++) {
                    if (connected.contains(to)) continue;
                    double cost = edgeCost(points.get(from), points.get(to), terrain);
                    if (cost < bestCost) {
                        bestCost = cost;
                        bestFrom = from;
                        bestTo = to;
                    }
                }
            }
            addEdge(selected, edges, bestFrom, bestTo);
            connected.add(bestTo);
        }

        int loops = Math.max(1, points.size() / 6);
        for (int added = 0; added < loops; added++) {
            double bestCost = Double.POSITIVE_INFINITY;
            int bestFrom = -1;
            int bestTo = -1;
            for (int from = 0; from < points.size(); from++) {
                for (int to = from + 1; to < points.size(); to++) {
                    if (edges.contains(edgeKey(from, to))) continue;
                    double cost = edgeCost(points.get(from), points.get(to), terrain);
                    if (cost < bestCost) {
                        bestCost = cost;
                        bestFrom = from;
                        bestTo = to;
                    }
                }
            }
            if (bestFrom < 0) break;
            addEdge(selected, edges, bestFrom, bestTo);
        }

        int[] degree = new int[points.size()];
        for (Edge edge : selected) {
            degree[edge.from()]++;
            degree[edge.to()]++;
        }
        List<Integer> ranked = new ArrayList<>();
        for (int index = 0; index < selected.size(); index++) ranked.add(index);
        ranked.sort((first, second) -> {
            double firstScore = trunkScore(selected.get(first), points, degree);
            double secondScore = trunkScore(selected.get(second), points, degree);
            int comparison = Double.compare(secondScore, firstScore);
            return comparison != 0 ? comparison : Integer.compare(first, second);
        });
        Set<Integer> trunks = new HashSet<>(ranked.subList(
                0, Math.max(1, selected.size() / 3)));

        List<RoadSegment> roads = new ArrayList<>();
        for (int index = 0; index < selected.size(); index++) {
            Edge edge = selected.get(index);
            MapPoi from = points.get(edge.from());
            MapPoi to = points.get(edge.to());
            RoadNode start = entrance(seed, from);
            RoadNode goal = entrance(seed, to);
            RoadKind kind = trunks.contains(index) ? RoadKind.TRUNK : RoadKind.BRANCH;
            roads.add(new RoadSegment(from, to, kind,
                    TerrainRoadRouter.route(seed, mapSize, terrain, start, goal)));
        }
        return List.copyOf(roads);
    }

    private static double trunkScore(Edge edge, List<MapPoi> points, int[] degree) {
        MapPoi from = points.get(edge.from());
        MapPoi to = points.get(edge.to());
        double distance = Math.hypot(to.x() - from.x(), to.z() - from.z());
        double largeBonus = (from.type() == PoiType.LARGE ? 900.0 : 0.0)
                + (to.type() == PoiType.LARGE ? 900.0 : 0.0);
        return distance + largeBonus + (degree[edge.from()] + degree[edge.to()]) * 180.0;
    }

    private static RoadNode entrance(long seed, MapPoi poi) {
        double angle;
        if (Math.hypot(poi.x(), poi.z()) >= 120.0) {
            angle = Math.atan2(-poi.z(), -poi.x())
                    + signedUnit(seed, poi.x(), poi.z(), 97) * 0.34;
        } else {
            angle = unit(seed, poi.x(), poi.z(), 101) * Math.PI * 2.0;
        }
        double distance = switch (poi.type()) {
            case SMALL -> 18.0;
            case MEDIUM -> 26.0;
            case LARGE -> 62.0;
        };
        return new RoadNode(
                poi.x() + Math.cos(angle) * distance,
                poi.y(),
                poi.z() + Math.sin(angle) * distance);
    }

    private static double edgeCost(MapPoi from, MapPoi to, BaseTerrainSampler terrain) {
        double distance = Math.hypot(to.x() - from.x(), to.z() - from.z());
        double penalty = Math.abs(to.y() - from.y()) * 18.0;
        int previousHeight = from.y();
        for (int step = 1; step < 12; step++) {
            double t = step / 12.0;
            int x = (int) Math.round(from.x() + (to.x() - from.x()) * t);
            int z = (int) Math.round(from.z() + (to.z() - from.z()) * t);
            TerrainSample sample = terrain.sample(x, z);
            if (sample.height() <= TerrainSampler.SEA_LEVEL + 1) penalty += 8_000.0;
            penalty += Math.max(0, sample.height() - 96) * 15.0;
            penalty += Math.abs(sample.height() - previousHeight) * 25.0;
            previousHeight = sample.height();
        }
        return distance + penalty;
    }

    private static void addEdge(List<Edge> selected, Set<Long> edges, int from, int to) {
        selected.add(new Edge(from, to));
        edges.add(edgeKey(from, to));
    }

    private static long edgeKey(int first, int second) {
        int low = Math.min(first, second);
        int high = Math.max(first, second);
        return ((long) low << 32) | (high & 0xffffffffL);
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

    private record Candidate(int x, int y, int z, long priority) {
    }

    private record Edge(int from, int to) {
    }
}
