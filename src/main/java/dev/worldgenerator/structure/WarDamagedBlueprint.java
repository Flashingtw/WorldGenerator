package dev.worldgenerator.structure;

/** Procedural voxel blueprints with coherent blast damage, collapse, rust, and rubble. */
final class WarDamagedBlueprint {
    private WarDamagedBlueprint() {
    }

    static StructureMaterial materialAt(StructurePlacement placement, int x, int y, int z) {
        StructureMaterial raw = switch (placement.type()) {
            case GAS_STATION -> gasStation(x, y, z);
            case WAREHOUSE -> warehouse(x, y, z);
            case MILITARY_COMPOUND -> ModernMilitaryBlueprint.materialAt(x, y, z);
        };
        return damage(placement, x, y, z, raw);
    }

    private static StructureMaterial gasStation(int x, int y, int z) {
        if (y == 0) {
            boolean buildingApron = x >= -22 && x <= 22 && z >= -1 && z <= 18;
            boolean forecourt = x >= -19 && x <= 6 && z >= -17 && z <= -4;
            boolean driveway = x >= 6 && x <= 23 && z >= -8 && z <= -3;
            return buildingApron || forecourt || driveway ? crackedPad(x, z) : null;
        }

        if (y == 9 && x >= -19 && x <= 6 && z >= -17 && z <= -4
                && Math.abs(x + 6) + Math.abs(z + 10) <= 21) return StructureMaterial.DARK_ROOF;
        if (y >= 1 && y < 9 && (x == -17 || x == 4) && (z == -15 || z == -6)) {
            return StructureMaterial.METAL;
        }
        if ((x == -14 || x == -5 || x == 4) && z >= -11 && z <= -9 && y <= 3) {
            return y == 3 ? StructureMaterial.COUNTER : StructureMaterial.RUSTED_METAL;
        }
        if (x >= 15 && x <= 17 && z >= -15 && z <= -13) {
            if (y >= 1 && y <= 9 && (x == 15 || x == 17)) return StructureMaterial.RUSTED_METAL;
            if (y >= 9 && y <= 14) return StructureMaterial.WARNING;
        }

        StructureMaterial shop = box(x, y, z, -21, 4, 0, 16, 10, StructureMaterial.CONCRETE);
        if (shop != null) {
            boolean frontWindow = z == 0 && y >= 3 && y <= 6
                    && (inRange(x, -18, -13) || inRange(x, -10, -5));
            boolean sideWindow = x == -21 && y >= 3 && y <= 5 && inRange(z, 5, 10);
            if (frontWindow || sideWindow) return StructureMaterial.GLASS;
            if (z == 0 && x >= -2 && x <= 0 && y <= 4) return StructureMaterial.CLEAR;
            if (shop == StructureMaterial.CLEAR) {
                if (y <= 3 && (z == 7 || z == 11) && x >= -17 && x <= -8) {
                    return StructureMaterial.SHELF;
                }
                if (y <= 2 && z == 4 && x >= -4 && x <= 1) return StructureMaterial.COUNTER;
                if (y <= 4 && z >= 13 && z <= 15 && x >= -18 && x <= -13) {
                    return StructureMaterial.CABINET;
                }
                if (y == 1 && z == 4 && (x == -16 || x == -12)) return StructureMaterial.TABLE;
                if (y == 1 && z == 3 && (x == -16 || x == -12)) return StructureMaterial.CHAIR;
                if (y == 8 && z == 15 && x >= -18 && x <= 0) return StructureMaterial.PIPE;
                if (y <= 6 && x == 3 && z >= 8 && z <= 15) return StructureMaterial.BRICK;
            }
            return shop;
        }

        StructureMaterial garage = chamferedBox(
                x - 15, y, z - 11, 7, 7, 2, 12, StructureMaterial.BRICK);
        if (garage != null) {
            if (z == 4 && x >= 11 && x <= 19 && y <= 8) {
                return y == 8 ? StructureMaterial.RUSTED_METAL : StructureMaterial.CLEAR;
            }
            if (garage == StructureMaterial.CLEAR) {
                if (y <= 2 && z >= 15 && z <= 17 && x >= 10 && x <= 20) {
                    return StructureMaterial.COUNTER;
                }
                if (y <= 5 && (x == 11 || x == 19) && (z == 8 || z == 14)) {
                    return y == 1 ? StructureMaterial.MACHINE : StructureMaterial.METAL;
                }
                if (y == 9 && x >= 10 && x <= 20 && z == 16) return StructureMaterial.PIPE;
            }
            return garage;
        }
        return StructureMaterial.CLEAR;
    }

    private static StructureMaterial warehouse(int x, int y, int z) {
        boolean mainFootprint = chamferedInside(x + 4, z, 27, 19, 6);
        boolean officeFootprint = x >= 14 && x <= 31 && z >= 7 && z <= 24;
        boolean loadingApron = x >= -32 && x <= 16 && z >= -25 && z <= -17;
        if (y == 0) return mainFootprint || officeFootprint || loadingApron ? crackedPad(x, z) : null;

        boolean monitor = x >= -11 && x <= 3 && z >= -12 && z <= 12;
        if (monitor && y == 17) return StructureMaterial.CLEAR;
        if (monitor && y >= 18 && y <= 19 && (x == -11 || x == 3)) return StructureMaterial.GLASS;
        if (monitor && y >= 18 && y <= 19 && (z == -12 || z == 12)) return StructureMaterial.METAL;
        if (monitor && y == 20) return StructureMaterial.DARK_ROOF;

        if (x >= -32 && x <= -27 && z >= -14 && z <= 12) {
            if (y == 8) return StructureMaterial.DARK_ROOF;
            if (y < 8 && x == -31 && (z == -12 || z == 10)) return StructureMaterial.METAL;
        }

        StructureMaterial office = box(x, y, z, 14, 31, 7, 24, 10, StructureMaterial.CONCRETE);
        if (office != null) {
            if (z == 7 && x >= 20 && x <= 22 && y <= 4) return StructureMaterial.CLEAR;
            if ((x == 31 || z == 24) && y >= 3 && y <= 5 && Math.floorMod(x + z, 7) <= 2) {
                return StructureMaterial.GLASS;
            }
            if (office == StructureMaterial.CLEAR) {
                if (y <= 2 && (z == 12 || z == 19) && x >= 18 && x <= 27) {
                    return y == 1 ? StructureMaterial.TABLE : StructureMaterial.CABINET;
                }
                if (y == 1 && (z == 13 || z == 20) && (x == 20 || x == 25)) {
                    return StructureMaterial.CHAIR;
                }
                if (y <= 6 && x == 23 && z >= 8 && z <= 23) return StructureMaterial.BRICK;
            }
            return office;
        }

        StructureMaterial shell = offsetChamferedBox(
                x, y, z, -4, 0, 27, 19, 6, 17, StructureMaterial.BRICK);
        if (shell == null) return StructureMaterial.CLEAR;
        if (z == -19 && y <= 9 && inAnyRange(x, -26, -18, -12, -4, 3, 11)) {
            return y == 8 ? StructureMaterial.RUSTED_METAL : StructureMaterial.CLEAR;
        }
        if (z == 19 && y >= 8 && y <= 10
                && (inRange(x, -22, -16) || inRange(x, -10, -4) || inRange(x, 2, 8))) {
            return StructureMaterial.GLASS;
        }
        if (shell == StructureMaterial.CLEAR) {
            if ((x == -22 || x == -12 || x == -2 || x == 8) && z >= -11 && z <= 12
                    && (y == 2 || y == 5 || (y <= 6 && Math.floorMod(z, 5) == 0))) {
                return StructureMaterial.SHELF;
            }
            if (y == 14 && z == 13 && x >= -27 && x <= 18) return StructureMaterial.PIPE;
            if (y <= 3 && x >= -27 && x <= -20 && z >= 13 && z <= 17) {
                return y == 1 ? StructureMaterial.CRATE : StructureMaterial.SHELF;
            }
        }
        if (y < 17 && (x == -22 || x == -4 || x == 14) && (z == -12 || z == 12)) {
            return StructureMaterial.METAL;
        }
        return shell;
    }

    private static StructureMaterial militaryCompound(int x, int y, int z) {
        if (!compoundInside(x, z)) return null;
        if (y == 0) {
            double helipad = Math.hypot(x - 38, z + 37);
            boolean helipadMarking = helipad >= 11.5 && helipad <= 14.0
                    || (Math.abs(x - 38) <= 1 && z >= -44 && z <= -30)
                    || (Math.abs(z + 37) <= 1 && x >= 31 && x <= 45);
            if (helipadMarking) return StructureMaterial.WARNING;
            return militaryPad(x, z) ? crackedPad(x, z) : null;
        }

        boolean perimeter = compoundInside(x, z)
                && (!compoundInside(x + 2, z) || !compoundInside(x - 2, z)
                || !compoundInside(x, z + 2) || !compoundInside(x, z - 2));
        boolean frontGate = z <= -57 && Math.abs(x) <= 9;
        if (perimeter && y <= 5 && !frontGate) {
            return y <= 2 ? StructureMaterial.CONCRETE : StructureMaterial.IRON_BARS;
        }
        if (z <= -56 && Math.abs(x) >= 10 && Math.abs(x) <= 13 && y <= 7) {
            return y <= 5 ? StructureMaterial.WARNING : StructureMaterial.METAL;
        }
        if (z >= -52 && z <= -48 && (inRange(x, -26, -15) || inRange(x, 15, 26)) && y <= 3) {
            return y == 3 ? StructureMaterial.WARNING : StructureMaterial.CONCRETE;
        }

        StructureMaterial tower = watchTower(x, y, z, -57, -45);
        if (tower == null) tower = watchTower(x, y, z, 57, -45);
        if (tower == null) tower = watchTower(x, y, z, -57, 45);
        if (tower == null) tower = watchTower(x, y, z, 57, 45);
        if (tower != null) return tower;

        StructureMaterial hangar = offsetChamferedBox(
                x, y, z, -27, 11, 32, 23, 6, 17, StructureMaterial.METAL);
        if (hangar != null) {
            if (z == -12 && x >= -48 && x <= -7 && y <= 12) {
                return y >= 11 ? StructureMaterial.RUSTED_METAL : StructureMaterial.CLEAR;
            }
            if (hangar == StructureMaterial.CLEAR) {
                if (y == 13 && (x == -51 || x == -35 || x == -19 || x == -3)
                        && z >= -7 && z <= 31) return StructureMaterial.PIPE;
                if (y <= 3 && (z == 25 || z == 30) && x >= -54 && x <= -8) {
                    return StructureMaterial.COUNTER;
                }
                if (y == 1 && (x == -45 || x == -27 || x == -10) && z == 20) {
                    return StructureMaterial.MACHINE;
                }
                if (y <= 7 && x == -52 && Math.floorMod(z + 8, 8) <= 1) {
                    return StructureMaterial.SHELF;
                }
            }
            return hangar;
        }
        StructureMaterial barracks = box(x, y, z, 17, 56, -8, 18, 10, StructureMaterial.BRICK);
        if (barracks != null) {
            if (z == -8 && x >= 34 && x <= 37 && y <= 4) return StructureMaterial.CLEAR;
            if ((z == -8 || z == 18) && y >= 4 && y <= 6
                    && (inRange(x, 20, 23) || inRange(x, 29, 32)
                    || inRange(x, 42, 45) || inRange(x, 50, 53))) {
                return StructureMaterial.GLASS;
            }
            if (barracks == StructureMaterial.CLEAR) {
                if (y <= 2 && (z == -3 || z == 5 || z == 13)
                        && (inRange(x, 20, 27) || inRange(x, 45, 52))) {
                    if (y == 1 && Math.floorMod(x, 4) <= 2) return StructureMaterial.TABLE;
                    if (y == 2 && (x == 20 || x == 27 || x == 45 || x == 52)) {
                        return StructureMaterial.CABINET;
                    }
                }
                if (y <= 8 && x == 38 && z >= -7 && z <= 17) return StructureMaterial.BRICK;
            }
            return barracks;
        }
        StructureMaterial barracksWing = box(
                x, y, z, 45, 61, 2, 15, 7, StructureMaterial.BRICK);
        if (barracksWing != null) {
            if (barracksWing == StructureMaterial.CLEAR && y <= 2
                    && (z == 5 || z == 12) && x >= 48 && x <= 58) {
                if (y == 1 && Math.floorMod(x, 4) <= 2) return StructureMaterial.TABLE;
                if (y == 2 && (x == 48 || x == 58)) return StructureMaterial.CABINET;
            }
            return barracksWing;
        }
        StructureMaterial admin = offsetChamferedBox(
                x, y, z, 33, 37, 15, 11, 3, 9, StructureMaterial.CONCRETE);
        if (admin != null) {
            if (z == 26 && x >= 31 && x <= 34 && y <= 4) return StructureMaterial.CLEAR;
            if ((z == 26 || z == 48) && y >= 3 && y <= 5
                    && (inRange(x, 21, 25) || inRange(x, 40, 44))) {
                return StructureMaterial.GLASS;
            }
            if (admin == StructureMaterial.CLEAR) {
                if (y <= 7 && (x == 29 || x == 39) && z >= 28 && z <= 46) {
                    return StructureMaterial.BRICK;
                }
                if (y <= 2 && (z == 32 || z == 42)
                        && (inRange(x, 21, 27) || inRange(x, 41, 47))) {
                    return y == 1 ? StructureMaterial.TABLE : StructureMaterial.CABINET;
                }
                if (y == 1 && (z == 33 || z == 43) && (x == 23 || x == 43)) {
                    return StructureMaterial.CHAIR;
                }
            }
            return admin;
        }
        StructureMaterial bunker = offsetChamferedBox(
                x, y, z, -52, 43, 10, 8, 2, 6, StructureMaterial.CONCRETE);
        if (bunker != null) {
            if (z == 35 && x >= -54 && x <= -51 && y <= 3) return StructureMaterial.CLEAR;
            return bunker;
        }
        if (x >= -47 && x <= -12 && z >= -46 && z <= -27) {
            if (y == 8) return StructureMaterial.DARK_ROOF;
            if (y < 8 && (x == -45 || x == -14) && (z == -44 || z == -29)) {
                return StructureMaterial.METAL;
            }
            if (y <= 2 && (z == -40 || z == -33) && Math.floorMod(x, 7) <= 3) {
                return y == 1 ? StructureMaterial.CRATE : StructureMaterial.RUSTED_METAL;
            }
        }
        return null;
    }

    private static boolean militaryPad(int x, int z) {
        boolean mainRoad = Math.abs(x) <= 5 && z >= -58 && z <= 42;
        boolean crossRoad = Math.abs(z) <= 4 && x >= -58 && x <= 58;
        boolean hangar = x >= -61 && x <= 7 && z >= -14 && z <= 37;
        boolean barracks = x >= 15 && x <= 62 && z >= -10 && z <= 20;
        boolean admin = x >= 16 && x <= 51 && z >= 24 && z <= 50;
        boolean bunker = x >= -64 && x <= -40 && z >= 33 && z <= 52;
        double helipad = Math.hypot(x - 38, z + 37);
        boolean perimeterFooting = !compoundInside(x + 1, z) || !compoundInside(x - 1, z)
                || !compoundInside(x, z + 1) || !compoundInside(x, z - 1);
        return mainRoad || crossRoad || hangar || barracks || admin || bunker
                || helipad <= 14 || perimeterFooting;
    }

    private static boolean compoundInside(int x, int z) {
        int absX = Math.abs(x);
        int absZ = Math.abs(z);
        return absX <= 72 && absZ <= 60 && absX + absZ <= 112;
    }

    private static StructureMaterial watchTower(int x, int y, int z, int centerX, int centerZ) {
        int dx = Math.abs(x - centerX);
        int dz = Math.abs(z - centerZ);
        if (dx > 4 || dz > 4) return null;
        if (y >= 1 && y <= 10 && dx == 3 && dz == 3) return StructureMaterial.METAL;
        if (y == 9 && dx <= 4 && dz <= 4) return StructureMaterial.WOOD;
        if (y >= 10 && y <= 13 && (dx == 4 || dz == 4)) return StructureMaterial.IRON_BARS;
        if (y == 14 && dx <= 4 && dz <= 4) return StructureMaterial.DARK_ROOF;
        return StructureMaterial.CLEAR;
    }

    private static StructureMaterial box(
            int x, int y, int z, int minX, int maxX, int minZ, int maxZ,
            int height, StructureMaterial wall) {
        if (x < minX || x > maxX || z < minZ || z > maxZ || y < 1 || y > height) return null;
        if (y == height) return StructureMaterial.DARK_ROOF;
        if (x == minX || x == maxX || z == minZ || z == maxZ) return wall;
        return StructureMaterial.CLEAR;
    }

    private static StructureMaterial chamferedBox(
            int x, int y, int z, int halfX, int halfZ, int cut,
            int height, StructureMaterial wall) {
        if (y < 1 || y > height || !chamferedInside(x, z, halfX, halfZ, cut)) return null;
        if (y == height) return StructureMaterial.DARK_ROOF;
        boolean edge = !chamferedInside(x + 1, z, halfX, halfZ, cut)
                || !chamferedInside(x - 1, z, halfX, halfZ, cut)
                || !chamferedInside(x, z + 1, halfX, halfZ, cut)
                || !chamferedInside(x, z - 1, halfX, halfZ, cut);
        return edge ? wall : StructureMaterial.CLEAR;
    }

    private static StructureMaterial offsetChamferedBox(
            int x, int y, int z, int centerX, int centerZ, int halfX, int halfZ,
            int cut, int height, StructureMaterial wall) {
        return chamferedBox(x - centerX, y, z - centerZ, halfX, halfZ, cut, height, wall);
    }

    private static boolean chamferedInside(int x, int z, int halfX, int halfZ, int cut) {
        int absX = Math.abs(x);
        int absZ = Math.abs(z);
        if (absX > halfX || absZ > halfZ) return false;
        int cornerX = Math.max(0, absX - (halfX - cut));
        int cornerZ = Math.max(0, absZ - (halfZ - cut));
        return cornerX + cornerZ <= cut;
    }

    private static StructureMaterial damage(
            StructurePlacement placement, int x, int y, int z, StructureMaterial raw) {
        long seed = placement.damageSeed();
        double blast = switch (placement.condition()) {
            case INTACT, WEATHERED -> Double.POSITIVE_INFINITY;
            case DAMAGED -> nearestBlast(placement.type(), seed, x, y, z) * 1.18;
            case RUINED -> nearestBlast(placement.type(), seed, x, y, z) * 0.72;
        };
        double jaggedEdge = unitHash(seed, x, y, z) * 0.42;
        if (y > 0 && raw != null && raw != StructureMaterial.CLEAR && blast < 1.08 + jaggedEdge) {
            return StructureMaterial.CLEAR;
        }
        if (y >= 1 && y <= 3 && (raw == null || raw == StructureMaterial.CLEAR)
                && blast < 1.85 && blast > 0.32 + y * 0.10
                && unitHash(seed ^ 0x44B1L, x, y, z) > 0.46 + y * 0.13) {
            return unitHash(seed ^ 0x91E3L, x, y, z) > 0.7
                    ? StructureMaterial.RUSTED_METAL : StructureMaterial.RUBBLE;
        }
        if (raw != null && raw != StructureMaterial.CLEAR && y > 0
                && blast >= 1.08 && blast < 1.95
                && unitHash(seed ^ 0x7711L, x, y, z) > 0.70) {
            return StructureMaterial.SCORCH;
        }
        double wear = unitHash(seed, Math.floorDiv(x, 5), Math.floorDiv(y, 4), Math.floorDiv(z, 5));
        double concreteThreshold = placement.condition() == StructureCondition.INTACT ? 0.96 : 0.83;
        double metalThreshold = placement.condition() == StructureCondition.INTACT ? 0.91 : 0.64;
        if (raw == StructureMaterial.CONCRETE && wear > concreteThreshold) {
            return StructureMaterial.CRACKED_CONCRETE;
        }
        if (raw == StructureMaterial.METAL && wear > metalThreshold) {
            return StructureMaterial.RUSTED_METAL;
        }
        return raw;
    }

    private static double nearestBlast(
            StructureType type, long seed, int x, int y, int z) {
        int sideX = (seed & 1L) == 0L ? 1 : -1;
        int sideZ = (seed & 2L) == 0L ? 1 : -1;
        return switch (type) {
            case GAS_STATION -> Math.min(
                    blast(x, y, z, sideX * 16, 7, 11, 7, 6, 7),
                    blast(x, y, z, -8, 8, -10, 6, 5, 6));
            case WAREHOUSE -> Math.min(
                    blast(x, y, z, sideX * 24, 10, sideZ * 15, 10, 9, 9),
                    Math.min(
                            blast(x, y, z, -8 * sideX, 15, -16 * sideZ, 8, 7, 8),
                            blast(x, y, z, 20, 6, 16, 7, 6, 7)));
            case MILITARY_COMPOUND -> Math.min(
                    blast(x, y, z, -43, 10, 12, 13, 10, 12),
                    Math.min(
                            blast(x, y, z, 43, 6, 7, 10, 7, 9),
                            Math.min(
                                    blast(x, y, z, 35, 6, 38, 9, 7, 9),
                                    Math.min(
                                            blast(x, y, z, sideX * 58, 7, sideZ * 45, 10, 8, 10),
                                            blast(x, y, z, 0, 5, -55, 8, 6, 8)))));
        };
    }

    private static double blast(
            int x, int y, int z,
            double centerX, double centerY, double centerZ,
            double radiusX, double radiusY, double radiusZ) {
        return square((x - centerX) / radiusX)
                + square((y - centerY) / radiusY)
                + square((z - centerZ) / radiusZ);
    }

    private static StructureMaterial crackedPad(int x, int z) {
        int patchX = Math.floorDiv(x + 3, 8);
        int patchZ = Math.floorDiv(z - 2, 8);
        long value = ((long) patchX * 734_287L) ^ ((long) patchZ * 912_931L);
        return Math.floorMod(value, 9) == 0 ? StructureMaterial.CRACKED_PAD : StructureMaterial.PAD;
    }

    private static boolean inAnyRange(int value, int... ranges) {
        for (int index = 0; index < ranges.length; index += 2) {
            if (value >= ranges[index] && value <= ranges[index + 1]) return true;
        }
        return false;
    }

    private static boolean inRange(int value, int minimum, int maximum) {
        return value >= minimum && value <= maximum;
    }

    private static double square(double value) {
        return value * value;
    }

    private static double unitHash(long seed, int x, int y, int z) {
        long value = seed ^ ((long) x * 0x9E3779B97F4A7C15L)
                ^ ((long) y * 0xC2B2AE3D27D4EB4FL)
                ^ ((long) z * 0x165667B19E3779F9L);
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        value ^= value >>> 31;
        return (value >>> 11) * 0x1.0p-53;
    }
}
