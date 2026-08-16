package dev.worldgenerator.preview;

import dev.worldgenerator.blueprint.BlueprintEdit;
import dev.worldgenerator.blueprint.BlueprintFacing;
import dev.worldgenerator.blueprint.BlueprintPart;
import dev.worldgenerator.blueprint.BlueprintState;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.block.data.Openable;
import org.bukkit.block.data.type.Slab;

/** Paper adapter at the voxel-blueprint seam. */
final class BukkitBlueprintAdapter {
    private BukkitBlueprintAdapter() {
    }

    static void apply(World world, BlueprintEdit edit) {
        BlueprintState state = edit.state();
        Material material = Material.matchMaterial(state.materialKey());
        if (material == null || !material.isBlock()) {
            throw new IllegalArgumentException("Unknown blueprint block material: " + state.materialKey());
        }
        BlockData data = material.createBlockData();
        if (data instanceof Directional directional) {
            BlockFace facing = blockFace(state.facing());
            if (directional.getFaces().contains(facing)) directional.setFacing(facing);
        }
        if (data instanceof Bisected bisected && state.part() != BlueprintPart.FULL) {
            bisected.setHalf(state.part() == BlueprintPart.TOP
                    ? Bisected.Half.TOP : Bisected.Half.BOTTOM);
        }
        if (data instanceof Slab slab && state.part() != BlueprintPart.FULL) {
            slab.setType(state.part() == BlueprintPart.TOP ? Slab.Type.TOP : Slab.Type.BOTTOM);
        }
        if (data instanceof MultipleFacing multipleFacing) {
            setFace(multipleFacing, BlockFace.NORTH,
                    (state.horizontalConnections() & BlueprintState.NORTH) != 0);
            setFace(multipleFacing, BlockFace.EAST,
                    (state.horizontalConnections() & BlueprintState.EAST) != 0);
            setFace(multipleFacing, BlockFace.SOUTH,
                    (state.horizontalConnections() & BlueprintState.SOUTH) != 0);
            setFace(multipleFacing, BlockFace.WEST,
                    (state.horizontalConnections() & BlueprintState.WEST) != 0);
        }
        if (data instanceof Openable openable) openable.setOpen(state.open());
        world.getBlockAt(edit.x(), edit.y(), edit.z()).setBlockData(data, false);
    }

    private static void setFace(MultipleFacing data, BlockFace face, boolean connected) {
        if (data.getAllowedFaces().contains(face)) data.setFace(face, connected);
    }

    private static BlockFace blockFace(BlueprintFacing facing) {
        return switch (facing) {
            case NORTH -> BlockFace.NORTH;
            case EAST -> BlockFace.EAST;
            case SOUTH -> BlockFace.SOUTH;
            case WEST -> BlockFace.WEST;
        };
    }
}
