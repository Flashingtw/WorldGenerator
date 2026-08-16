package dev.worldgenerator.structure;

/**
 * Authored modern military installation used by large adventure-map POIs.
 * The public seam is deliberately tiny: callers ask only for the semantic
 * material at a local coordinate, while this class owns the whole site plan.
 */
final class ModernMilitaryBlueprint {
    private ModernMilitaryBlueprint() {
    }

    static StructureMaterial materialAt(int x, int y, int z) {
        if (!compoundInside(x, z)) return null;
        if (y == 0) return groundAt(x, z);

        StructureMaterial material = perimeterAt(x, y, z);
        if (material != null) return material;

        material = checkpointAt(x, y, z);
        if (material != null) return material;
        material = towerAt(x, y, z, -58, -44);
        if (material == null) material = towerAt(x, y, z, 58, -44);
        if (material == null) material = towerAt(x, y, z, -58, 43);
        if (material == null) material = towerAt(x, y, z, 58, 43);
        if (material != null) return material;

        material = hangarAt(x, y, z);
        if (material != null) return material;
        material = motorPoolAt(x, y, z);
        if (material != null) return material;
        material = barracksAt(x, y, z);
        if (material != null) return material;
        material = headquartersAt(x, y, z);
        if (material != null) return material;
        material = armoryAt(x, y, z);
        if (material != null) return material;
        material = helipadAt(x, y, z);
        if (material != null) return material;
        return fieldDefencesAt(x, y, z);
    }

    private static StructureMaterial groundAt(int x, int z) {
        if (!compoundInside(x, z)) return null;
        double helipad = Math.hypot(x - 39, z + 35);
        if (helipad <= 14.5) {
            boolean ring = helipad >= 12.4;
            boolean h = (Math.abs(x - 39) <= 1 && z >= -42 && z <= -28)
                    || (Math.abs(z + 35) <= 1 && (inRange(x, 33, 37) || inRange(x, 41, 45)));
            return ring || h ? StructureMaterial.WARNING : StructureMaterial.FLOOR;
        }

        boolean mainLane = Math.abs(x) <= 4 && z >= -60 && z <= 48;
        boolean crossLane = Math.abs(z + 2) <= 4 && x >= -57 && x <= 57;
        boolean hangarApron = x >= -58 && x <= -16 && z >= -3 && z <= 9;
        boolean motorApron = x >= -57 && x <= -18 && z >= -48 && z <= -17;
        boolean barracksWalk = x >= 12 && x <= 61 && z >= -13 && z <= 25;
        boolean hqWalk = x >= 12 && x <= 58 && z >= 25 && z <= 54;
        boolean armoryWalk = x >= -13 && x <= 13 && z >= 27 && z <= 52;
        boolean checkpointApron = x >= -7 && x <= 23 && z >= -59 && z <= -39;
        boolean perimeterFooting = onCompoundEdge(x, z, 1);
        if (hangarFootprint(x, z) || barracksFootprint(x, z)
                || headquartersFootprint(x, z) || armoryFootprint(x, z)) {
            return StructureMaterial.FLOOR;
        }
        if (mainLane || crossLane || hangarApron || motorApron || barracksWalk
                || hqWalk || armoryWalk || checkpointApron || perimeterFooting) {
            return patch(x, z);
        }
        return null;
    }

    private static StructureMaterial perimeterAt(int x, int y, int z) {
        boolean gate = z <= -57 && Math.abs(x) <= 9;
        if (onCompoundEdge(x, z, 2) && !gate && y <= 5) {
            return y <= 2 ? StructureMaterial.CONCRETE : StructureMaterial.IRON_BARS;
        }
        if (z <= -56 && inRange(Math.abs(x), 10, 13) && y <= 8) {
            if (y <= 5) return StructureMaterial.CONCRETE;
            return y == 8 ? StructureMaterial.WARNING : StructureMaterial.METAL;
        }
        return null;
    }

    private static StructureMaterial checkpointAt(int x, int y, int z) {
        StructureMaterial booth = chamferedBox(
                x, y, z, 15, -48, 7, 7, 2, 7, StructureMaterial.WHITE_PANEL);
        if (booth != null) {
            if (z == -55 && inRange(x, 13, 16) && y <= 4) return StructureMaterial.CLEAR;
            if ((x == 8 || x == 22 || z == -41) && y >= 3 && y <= 5) {
                return StructureMaterial.GLASS;
            }
            if (booth == StructureMaterial.CLEAR) {
                if (y == 1 && z == -47 && inRange(x, 12, 18)) return StructureMaterial.TABLE;
                if (y == 1 && z == -45 && (x == 13 || x == 17)) return StructureMaterial.CHAIR;
                if (y == 2 && x == 20 && inRange(z, -51, -47)) return StructureMaterial.SCREEN;
                if (y == 6 && x == 15 && z == -48) return StructureMaterial.LIGHT;
            }
            return booth;
        }
        if (z == -52 && inRange(x, -8, 7) && y <= 3) {
            if (inRange(x, -2, 1)) return StructureMaterial.CLEAR;
            return y == 3 ? StructureMaterial.WARNING : StructureMaterial.HESCO;
        }
        return null;
    }

    private static StructureMaterial hangarAt(int x, int y, int z) {
        if (!hangarFootprint(x, z)) return null;
        int roofY = 13 + Math.max(0, (25 - Math.abs(x + 37)) / 4);
        if (y < 1 || y > roofY) return null;
        if (y == roofY) return StructureMaterial.DARK_ROOF;

        boolean sideWall = (x == -62 || x == -12) && y <= 12;
        boolean endWall = z == 6 || z == 41;
        boolean vehicleDoor = z == 6 && inRange(x, -56, -19) && y <= 10;
        if ((sideWall || endWall) && !vehicleDoor) {
            if (z == 41 && y >= 5 && y <= 8
                    && (inRange(x, -55, -48) || inRange(x, -40, -33)
                    || inRange(x, -25, -18))) return StructureMaterial.GLASS;
            if (Math.floorMod(x + z, 11) == 0 && y <= 11) return StructureMaterial.WHITE_PANEL;
            return StructureMaterial.OLIVE_PANEL;
        }
        if (vehicleDoor && y >= 10) return StructureMaterial.METAL;

        boolean frame = (Math.floorMod(z - 7, 8) == 0)
                && (x == -59 || x == -37 || x == -15 || y == 11);
        if (frame && y <= 11) return StructureMaterial.METAL;
        if (y == 12 && Math.floorMod(z - 7, 8) == 0 && inRange(x, -59, -15)) {
            return StructureMaterial.PIPE;
        }
        if (y == 1 && (z == 28 || z == 35) && inRange(x, -58, -48)) {
            return StructureMaterial.COUNTER;
        }
        if (y <= 4 && x == -59 && Math.floorMod(z - 11, 7) <= 1) {
            return y == 1 ? StructureMaterial.CRATE : StructureMaterial.SHELF;
        }
        if (y == 1 && (x == -47 || x == -36 || x == -25)
                && (z == 20 || z == 31)) return StructureMaterial.MACHINE;
        if (y == 10 && (x == -49 || x == -37 || x == -25)
                && (z == 17 || z == 33)) return StructureMaterial.LIGHT;
        return StructureMaterial.CLEAR;
    }

    private static StructureMaterial motorPoolAt(int x, int y, int z) {
        if (!inRange(x, -57, -18) || !inRange(z, -47, -20)) return null;
        if (y == 8 && !(x > -27 && z < -37)) return StructureMaterial.DARK_ROOF;
        boolean post = (x == -55 || x == -38 || x == -20) && (z == -45 || z == -22);
        if (post && y <= 7) return StructureMaterial.METAL;
        if (y == 1 && (z == -42 || z == -29) && Math.floorMod(x + 58, 8) <= 3) {
            return StructureMaterial.CRATE;
        }
        if (y == 2 && x == -54 && Math.floorMod(z + 45, 6) <= 1) return StructureMaterial.PIPE;
        if (y == 1 && (x == -32 || x == -24) && (z == -34 || z == -26)) {
            return StructureMaterial.MACHINE;
        }
        return null;
    }

    private static StructureMaterial barracksAt(int x, int y, int z) {
        StructureMaterial shell = modularWing(x, y, z, 16, 59, -11, 1, 8);
        if (shell == null) shell = modularWing(x, y, z, 25, 59, 9, 22, 8);
        if (shell == null) shell = modularWing(x, y, z, 51, 59, 2, 8, 7);
        if (shell == null) return null;

        boolean entrance = (z == -11 && inRange(x, 35, 38) && y <= 4)
                || (z == 22 && inRange(x, 39, 42) && y <= 4);
        if (entrance) return StructureMaterial.CLEAR;
        boolean window = y >= 3 && y <= 5 && (z == -11 || z == 1 || z == 9 || z == 22)
                && Math.floorMod(x - 19, 10) <= 3;
        if (window) return StructureMaterial.GLASS;
        if (shell != StructureMaterial.CLEAR) return shell;

        if (y <= 2 && (z == -7 || z == -3 || z == 13 || z == 18)
                && Math.floorMod(x - 20, 8) <= 3) return StructureMaterial.BED;
        if (y <= 3 && (z == 0 || z == 10) && Math.floorMod(x - 18, 9) <= 1) {
            return StructureMaterial.LOCKER;
        }
        if (y == 1 && z == -5 && (x == 30 || x == 44)) return StructureMaterial.TABLE;
        if (y == 1 && z == -4 && (x == 30 || x == 44)) return StructureMaterial.CHAIR;
        if (y == 7 && Math.floorMod(x - 20, 12) == 0) return StructureMaterial.LIGHT;
        return StructureMaterial.CLEAR;
    }

    private static StructureMaterial headquartersAt(int x, int y, int z) {
        if (!headquartersFootprint(x, z) || y < 1 || y > 10) return null;
        if (y == 10) return StructureMaterial.DARK_ROOF;
        boolean outside = !headquartersFootprint(x + 1, z) || !headquartersFootprint(x - 1, z)
                || !headquartersFootprint(x, z + 1) || !headquartersFootprint(x, z - 1);
        if (outside) {
            if (z == 27 && inRange(x, 21, 24) && y <= 4) return StructureMaterial.CLEAR;
            if (y >= 3 && y <= 6 && Math.floorMod(x + 2 * z, 11) <= 3) {
                return StructureMaterial.GLASS;
            }
            return y <= 2 ? StructureMaterial.CONCRETE : StructureMaterial.WHITE_PANEL;
        }
        boolean partition = (x == 30 && z <= 45) || (z == 40 && x >= 17 && x <= 54);
        if (partition && y <= 8) {
            if ((x == 30 && inRange(z, 33, 35)) || (z == 40 && inRange(x, 43, 45))) {
                return StructureMaterial.CLEAR;
            }
            return StructureMaterial.WHITE_PANEL;
        }
        if (y == 1 && (z == 33 || z == 46) && Math.floorMod(x - 19, 9) <= 4) {
            return StructureMaterial.TABLE;
        }
        if (y == 1 && (z == 34 || z == 47) && Math.floorMod(x - 19, 9) == 2) {
            return StructureMaterial.CHAIR;
        }
        if (y == 2 && (z == 32 || z == 45) && Math.floorMod(x - 19, 9) <= 4) {
            return StructureMaterial.SCREEN;
        }
        if (y <= 3 && x == 52 && Math.floorMod(z - 30, 6) <= 1) {
            return StructureMaterial.CABINET;
        }
        if (y == 8 && (x == 23 || x == 40 || x == 51) && (z == 34 || z == 46)) {
            return StructureMaterial.LIGHT;
        }
        return StructureMaterial.CLEAR;
    }

    private static StructureMaterial armoryAt(int x, int y, int z) {
        StructureMaterial shell = chamferedBox(
                x, y, z, 0, 40, 10, 11, 3, 7, StructureMaterial.CONCRETE);
        if (shell == null) return null;
        if (z == 29 && inRange(x, -2, 2) && y <= 4) return StructureMaterial.CLEAR;
        if (shell == StructureMaterial.CLEAR) {
            if (y <= 4 && (x == -7 || x == 7) && Math.floorMod(z - 32, 5) <= 1) {
                return StructureMaterial.LOCKER;
            }
            if (y <= 2 && z == 48 && inRange(x, -6, 6)) return StructureMaterial.SHELF;
            if (y == 5 && x == 0 && (z == 35 || z == 44)) return StructureMaterial.LIGHT;
        }
        return shell;
    }

    private static StructureMaterial helipadAt(int x, int y, int z) {
        double distance = Math.hypot(x - 39, z + 35);
        if (distance > 15.5) return null;
        if (y == 1 && distance >= 13.5 && distance <= 15.5
                && Math.floorMod((int) Math.round(distance * 10) + x + z, 4) == 0) {
            return StructureMaterial.WARNING;
        }
        if (y <= 3 && (x == 25 || x == 53) && z == -35) return StructureMaterial.LIGHT;
        return null;
    }

    private static StructureMaterial fieldDefencesAt(int x, int y, int z) {
        boolean frontLeft = inRange(x, -28, -14) && inRange(z, -53, -50);
        boolean frontRight = inRange(x, 28, 44) && inRange(z, -53, -50);
        boolean staggered = inRange(x, 7, 23) && inRange(z, -31, -28);
        if ((frontLeft || frontRight || staggered) && y <= 3) {
            return y == 3 && Math.floorMod(x, 5) == 0
                    ? StructureMaterial.WARNING : StructureMaterial.HESCO;
        }
        return null;
    }

    private static StructureMaterial towerAt(int x, int y, int z, int centerX, int centerZ) {
        int dx = Math.abs(x - centerX);
        int dz = Math.abs(z - centerZ);
        if (dx > 4 || dz > 4) return null;
        if (y >= 1 && y <= 10 && dx == 3 && dz == 3) return StructureMaterial.METAL;
        if (y == 9 && dx <= 4 && dz <= 4) return StructureMaterial.FLOOR;
        if (y >= 10 && y <= 13 && (dx == 4 || dz == 4)) return StructureMaterial.IRON_BARS;
        if (y == 14 && dx <= 4 && dz <= 4) return StructureMaterial.DARK_ROOF;
        if (y == 10 && dx <= 3 && dz <= 3) return StructureMaterial.CLEAR;
        return null;
    }

    private static StructureMaterial modularWing(
            int x, int y, int z, int minX, int maxX, int minZ, int maxZ, int height) {
        if (!inRange(x, minX, maxX) || !inRange(z, minZ, maxZ) || y < 1 || y > height) {
            return null;
        }
        if (y == height) return StructureMaterial.DARK_ROOF;
        if (x == minX || x == maxX || z == minZ || z == maxZ) {
            return y <= 2 ? StructureMaterial.CONCRETE : StructureMaterial.OLIVE_PANEL;
        }
        return StructureMaterial.CLEAR;
    }

    private static StructureMaterial chamferedBox(
            int x, int y, int z, int centerX, int centerZ, int halfX, int halfZ,
            int cut, int height, StructureMaterial wall) {
        int dx = x - centerX;
        int dz = z - centerZ;
        if (y < 1 || y > height || !chamferedInside(dx, dz, halfX, halfZ, cut)) return null;
        if (y == height) return StructureMaterial.DARK_ROOF;
        boolean edge = !chamferedInside(dx + 1, dz, halfX, halfZ, cut)
                || !chamferedInside(dx - 1, dz, halfX, halfZ, cut)
                || !chamferedInside(dx, dz + 1, halfX, halfZ, cut)
                || !chamferedInside(dx, dz - 1, halfX, halfZ, cut);
        return edge ? wall : StructureMaterial.CLEAR;
    }

    private static boolean hangarFootprint(int x, int z) {
        return inRange(x, -62, -12) && inRange(z, 6, 41);
    }

    private static boolean barracksFootprint(int x, int z) {
        return inRange(x, 16, 59) && inRange(z, -11, 1)
                || inRange(x, 25, 59) && inRange(z, 9, 22)
                || inRange(x, 51, 59) && inRange(z, 2, 8);
    }

    private static boolean headquartersFootprint(int x, int z) {
        return inRange(x, 15, 55) && inRange(z, 27, 40)
                || inRange(x, 15, 30) && inRange(z, 41, 53)
                || inRange(x, 44, 55) && inRange(z, 41, 50);
    }

    private static boolean armoryFootprint(int x, int z) {
        return chamferedInside(x, z - 40, 10, 11, 3);
    }

    private static boolean compoundInside(int x, int z) {
        int absX = Math.abs(x);
        int absZ = Math.abs(z);
        return absX <= 72 && absZ <= 60 && absX + absZ <= 112;
    }

    private static boolean onCompoundEdge(int x, int z, int thickness) {
        return compoundInside(x, z) && (!compoundInside(x + thickness, z)
                || !compoundInside(x - thickness, z) || !compoundInside(x, z + thickness)
                || !compoundInside(x, z - thickness));
    }

    private static boolean chamferedInside(int x, int z, int halfX, int halfZ, int cut) {
        int absX = Math.abs(x);
        int absZ = Math.abs(z);
        if (absX > halfX || absZ > halfZ) return false;
        return Math.max(0, absX - (halfX - cut)) + Math.max(0, absZ - (halfZ - cut)) <= cut;
    }

    private static StructureMaterial patch(int x, int z) {
        long cell = (long) Math.floorDiv(x, 10) * 0x9E3779B97F4A7C15L
                ^ (long) Math.floorDiv(z, 10) * 0xC2B2AE3D27D4EB4FL;
        cell ^= cell >>> 29;
        return Math.floorMod(cell, 7) == 0 ? StructureMaterial.CRACKED_PAD : StructureMaterial.PAD;
    }

    private static boolean inRange(int value, int min, int max) {
        return value >= min && value <= max;
    }
}
