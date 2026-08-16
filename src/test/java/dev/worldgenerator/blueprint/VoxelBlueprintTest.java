package dev.worldgenerator.blueprint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import org.junit.jupiter.api.Test;

class VoxelBlueprintTest {
    private static final String SAMPLE = """
            id=test_rig
            anchor=0,0,0
            palette.S=minecraft:stone_stairs;north;bottom
            palette.D=minecraft:iron_door;east;top
            palette.G=minecraft:iron_bars;north;full;east,west
            layer=0
            SDG
            """;

    @Test
    void rotationTransformsCoordinatesFacingAndConnectionsTogether() throws IOException {
        VoxelBlueprint blueprint = VoxelBlueprint.read(new StringReader(SAMPLE));
        List<BlueprintEdit> edits = blueprint.place(new BlueprintPlacement(10, 64, 20, 1, false));

        assertEquals(new BlueprintEdit(10, 64, 20,
                new BlueprintState("minecraft:stone_stairs", BlueprintFacing.EAST,
                        BlueprintPart.BOTTOM, 0, false)), edits.get(0));
        assertEquals(10, edits.get(1).x());
        assertEquals(21, edits.get(1).z());
        assertEquals(BlueprintFacing.SOUTH, edits.get(1).state().facing());
        assertEquals(BlueprintPart.TOP, edits.get(1).state().part());
        assertEquals(BlueprintState.NORTH | BlueprintState.SOUTH,
                edits.get(2).state().horizontalConnections());
    }

    @Test
    void mirroringTransformsCoordinatesFacingAndConnections() throws IOException {
        VoxelBlueprint blueprint = VoxelBlueprint.read(new StringReader(SAMPLE));
        List<BlueprintEdit> edits = blueprint.place(new BlueprintPlacement(0, 0, 0, 0, true));

        assertEquals(-1, edits.get(1).x());
        assertEquals(BlueprintFacing.WEST, edits.get(1).state().facing());
        assertEquals(BlueprintState.EAST | BlueprintState.WEST,
                edits.get(2).state().horizontalConnections());
    }

    @Test
    void allFourQuarterTurnsKeepCoordinatesAndFacingInLockstep() throws IOException {
        VoxelBlueprint blueprint = VoxelBlueprint.read(new StringReader(SAMPLE));
        int[][] expectedCoordinates = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};
        BlueprintFacing[] expectedFacings = {
                BlueprintFacing.EAST,
                BlueprintFacing.SOUTH,
                BlueprintFacing.WEST,
                BlueprintFacing.NORTH};
        for (int turn = 0; turn < 4; turn++) {
            BlueprintEdit door = blueprint.place(
                    new BlueprintPlacement(0, 0, 0, turn, false)).get(1);
            assertEquals(expectedCoordinates[turn][0], door.x(), "x at turn " + turn);
            assertEquals(expectedCoordinates[turn][1], door.z(), "z at turn " + turn);
            assertEquals(expectedFacings[turn], door.state().facing(), "facing at turn " + turn);
        }
    }

    @Test
    void bundledAcceptanceRigLoadsAsSparseAuthoredData() throws IOException {
        VoxelBlueprint blueprint = BlueprintCatalog.load(BlueprintCatalog.ROTATION_LAB);
        assertEquals("rotation_lab", blueprint.id());
        assertEquals(11, blueprint.width());
        assertEquals(5, blueprint.height());
        assertEquals(9, blueprint.depth());
        assertTrue(blueprint.place(new BlueprintPlacement(0, 64, 0, 0, false)).size() > 130);
    }

    @Test
    void parserRejectsUndefinedPaletteSymbols() {
        String invalid = """
                id=bad
                anchor=0,0,0
                palette.C=minecraft:stone
                layer=0
                X
                """;
        IOException failure = assertThrows(
                IOException.class, () -> VoxelBlueprint.read(new StringReader(invalid)));
        assertTrue(failure.getMessage().contains("undefined palette symbol X"));
    }
}
