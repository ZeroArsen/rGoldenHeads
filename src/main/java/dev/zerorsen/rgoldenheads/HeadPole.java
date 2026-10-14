package dev.zerorsen.rgoldenheads;

import com.destroystokyo.paper.profile.PlayerProfile;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Skull;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Rotatable;

/**
 * The classic UHC head pole: a post with the dead player's head on top. The head is a real
 * player head block, so breaking it drops the skinned head item, ready to craft.
 */
public final class HeadPole {

    // How far above the death spot to look for room, e.g. when the player died in a 1-block gap
    private static final int MAX_LIFT = 3;

    // Skull rotations clockwise from south, the same order as Minecraft's yaw
    private static final BlockFace[] ROTATIONS = {
            BlockFace.SOUTH, BlockFace.SOUTH_SOUTH_WEST, BlockFace.SOUTH_WEST, BlockFace.WEST_SOUTH_WEST,
            BlockFace.WEST, BlockFace.WEST_NORTH_WEST, BlockFace.NORTH_WEST, BlockFace.NORTH_NORTH_WEST,
            BlockFace.NORTH, BlockFace.NORTH_NORTH_EAST, BlockFace.NORTH_EAST, BlockFace.EAST_NORTH_EAST,
            BlockFace.EAST, BlockFace.EAST_SOUTH_EAST, BlockFace.SOUTH_EAST, BlockFace.SOUTH_SOUTH_EAST};

    private HeadPole() {
    }

    /**
     * Builds the pole at {@code at}, with the head facing the way {@code at} looks. Must run on
     * the thread that owns that location. Returns false, changing nothing, when there's no room
     * within a few blocks or the spot is outside the world's height.
     */
    public static boolean place(Location at, PlayerProfile profile, Material pole) {
        World world = at.getWorld();
        if (world == null) {
            return false;
        }
        Block base = at.getBlock();
        for (int lift = 0; lift <= MAX_LIFT; lift++) {
            Block poleBlock = base.getRelative(BlockFace.UP, lift);
            Block headBlock = poleBlock.getRelative(BlockFace.UP);
            if (poleBlock.getY() < world.getMinHeight() || headBlock.getY() >= world.getMaxHeight()) {
                return false;
            }
            if (!poleBlock.isReplaceable() || !headBlock.isReplaceable()) {
                continue;
            }

            poleBlock.setType(pole);
            headBlock.setType(Material.PLAYER_HEAD, false);
            BlockData data = headBlock.getBlockData();
            if (data instanceof Rotatable rotatable) {
                rotatable.setRotation(facing(at.getYaw()));
                headBlock.setBlockData(rotatable, false);
            }
            if (headBlock.getState() instanceof Skull skull) {
                skull.setPlayerProfile(profile);
                skull.update(true, false);
            }
            return true;
        }
        return false;
    }

    private static BlockFace facing(float yaw) {
        return ROTATIONS[Math.round(yaw / 22.5f) & 15];
    }
}
