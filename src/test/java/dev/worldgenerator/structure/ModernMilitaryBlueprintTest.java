package dev.worldgenerator.structure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ModernMilitaryBlueprintTest {
    @Test
    void authoredZonesHaveDistinctArchitectureAndInteriors() {
        assertTrue(Set.of(StructureMaterial.PAD, StructureMaterial.CRACKED_PAD)
                .contains(ModernMilitaryBlueprint.materialAt(0, 0, 0)));
        assertEquals(StructureMaterial.DARK_ROOF,
                ModernMilitaryBlueprint.materialAt(-37, 19, 20), "hangar must have a raised ridge");
        assertEquals(StructureMaterial.MACHINE,
                ModernMilitaryBlueprint.materialAt(-47, 1, 20), "hangar needs service equipment");
        assertEquals(StructureMaterial.BED,
                ModernMilitaryBlueprint.materialAt(20, 1, -7), "barracks need bunks");
        assertEquals(StructureMaterial.LOCKER,
                ModernMilitaryBlueprint.materialAt(18, 1, 0), "barracks need lockers");
        assertEquals(StructureMaterial.SCREEN,
                ModernMilitaryBlueprint.materialAt(19, 2, 32), "headquarters need workstations");
        assertEquals(StructureMaterial.SHELF,
                ModernMilitaryBlueprint.materialAt(0, 1, 48), "armory needs storage");
        assertEquals(StructureMaterial.WARNING,
                ModernMilitaryBlueprint.materialAt(39, 0, -35), "helipad needs an H marking");
    }

    @Test
    void compoundUsesAControlledModernPalette() {
        Set<StructureMaterial> present = EnumSet.noneOf(StructureMaterial.class);
        for (int x = -72; x <= 72; x++) {
            for (int z = -60; z <= 60; z++) {
                for (int y = 0; y <= 22; y++) {
                    StructureMaterial material = ModernMilitaryBlueprint.materialAt(x, y, z);
                    if (material != null) present.add(material);
                }
            }
        }
        assertTrue(present.containsAll(Set.of(
                StructureMaterial.OLIVE_PANEL,
                StructureMaterial.WHITE_PANEL,
                StructureMaterial.IRON_BARS,
                StructureMaterial.HESCO,
                StructureMaterial.BED,
                StructureMaterial.LOCKER,
                StructureMaterial.SCREEN,
                StructureMaterial.LIGHT,
                StructureMaterial.MACHINE)));
    }

    @Test
    void entranceIsOpenAndUnusedGroundRemainsNatural() {
        assertEquals(StructureMaterial.PAD, ModernMilitaryBlueprint.materialAt(0, 0, -60));
        assertNull(ModernMilitaryBlueprint.materialAt(0, 1, -60), "main gate must remain open");
        assertNull(ModernMilitaryBlueprint.materialAt(-35, 0, 52),
                "the compound must not become one rectangular concrete slab");
        assertNull(ModernMilitaryBlueprint.materialAt(72, 0, 60),
                "clipped perimeter corners must stay outside the compound");
    }
}
