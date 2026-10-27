/*
 * GNU Lesser General Public License v3
 * Copyright (C) 2024 Tschipp
 * mrtschipp@gmail.com
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package tschipp.carryon.common.carry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.common.util.BlockSnapshot;

import tschipp.carryon.Constants;
import tschipp.carryon.common.config.ListHandler;
import tschipp.carryon.networking.CarryNetwork;
import tschipp.carryon.utils.SizeHelper;

/**
 * Server-side placement logic, mirrored from the modern PlacementHandler with 1.7.10 APIs.
 * 1.7.10 uses block+metadata and explicit block-place checks instead of BlockStates.
 */
public class PlacementHandler {

    /** Callback that allows other mods to veto block placement. */
    public interface BlockPlaceCallback {

        boolean allow(BlockSnapshot previous);
    }

    /** Callback that allows other mods to veto entity placement/stacking. */
    public interface EntityPlaceCallback {

        boolean allow(Entity toPlace);
    }

    public static boolean tryPlaceBlock(EntityPlayerMP player, World level, int x, int y, int z, int face,
        BlockPlaceCallback callback) {
        CarryOnData carry = CarryOnDataManager.getCarryData(player);
        if (!carry.isCarrying(CarryType.BLOCK)) return false;

        if (player.ticksExisted == carry.getTick()) return false;

        Block block = carry.getBlock();
        if (block == null || block == net.minecraft.init.Blocks.air) return false;
        int meta = tschipp.carryon.common.carry.MetadataReorientation
            .getReorientedMetadata(block, carry.getBlockMetadata(), player);

        // If the clicked position is not free, place on the clicked face instead.
        int tx = x;
        int ty = y;
        int tz = z;
        if (!isReplaceable(level, x, y, z)) {
            if (face >= 0 && face < 6) {
                EnumFacing facing = EnumFacing.getFront(face);
                tx = x + facing.getFrontOffsetX();
                ty = y + facing.getFrontOffsetY();
                tz = z + facing.getFrontOffsetZ();
            } else {
                return false;
            }
        }

        if (ty < 0 || ty >= level.getHeight()) {
            deniedSound(player);
            return false;
        }

        TileEntity blockEntity = carry.getBlockEntity(level, tx, ty, tz);

        ItemStack stack = new ItemStack(net.minecraft.item.Item.getItemFromBlock(block), 1, 0);
        boolean canPlace = level
            .canPlaceEntityOnSide(block, tx, ty, tz, false, face >= 0 && face < 6 ? face : 1, player, stack);
        if (!canPlace) {
            deniedSound(player);
            return false;
        }

        if (!player.canPlayerEdit(tx, ty, tz, face, stack)) return false;
        net.minecraftforge.common.util.BlockSnapshot previous = net.minecraftforge.common.util.BlockSnapshot
            .getBlockSnapshot(level, tx, ty, tz);
        if (carry.getTileNbt() != null && blockEntity == null) return false;
        boolean capturing = level.captureBlockSnapshots;
        int snapshotCount = level.capturedBlockSnapshots.size();
        level.captureBlockSnapshots = true;
        boolean placed;
        try {
            placed = level.setBlock(tx, ty, tz, block, meta, 3);
            if (placed && blockEntity != null) level.setTileEntity(tx, ty, tz, blockEntity);
        } catch (RuntimeException error) {
            restorePlacement(previous);
            throw error;
        } finally {
            level.captureBlockSnapshots = capturing;
            if (!capturing) while (level.capturedBlockSnapshots.size() > snapshotCount)
                level.capturedBlockSnapshots.remove(level.capturedBlockSnapshots.size() - 1);
        }
        // PlaceEvent consumers must see the actual carried block and restored tile, not the old air.
        if (!placed) return false;
        boolean doPlace;
        try {
            doPlace = callback == null || callback.allow(previous);
        } catch (RuntimeException error) {
            restorePlacement(previous);
            throw error;
        }
        if (!doPlace) {
            restorePlacement(previous);
            return false;
        }
        if (level.getBlock(tx, ty, tz) != block) {
            restorePlacement(previous);
            return false;
        }

        String command = placementCommand(carry);
        carry.clear();
        CarryOnDataManager.setCarryData(player, carry);
        if (!block.hasTileEntity(meta)) block.onBlockAdded(level, tx, ty, tz);
        level.markAndNotifyBlock(tx, ty, tz, null, previous.replacedBlock, block, 3);

        level.playSoundEffect(tx + 0.5D, ty + 0.5D, tz + 0.5D, block.stepSound.func_150496_b(), 1.0F, 0.5F);
        player.swingItem();
        player.removePotionEffect(Potion.moveSlowdown.id);
        runPlacementCommand(player, command);
        return true;
    }

    public static boolean tryPlaceEntity(EntityPlayerMP player, World level, int x, int y, int z, int face,
        EntityPlaceCallback callback) {
        CarryOnData carry = CarryOnDataManager.getCarryData(player);
        if (!carry.isCarrying(CarryType.ENTITY) && !carry.isCarrying(CarryType.PLAYER)) return false;

        if (player.ticksExisted == carry.getTick()) return false;

        int tx = x;
        int ty = y;
        int tz = z;
        if (!isReplaceable(level, x, y, z)) {
            if (face >= 0 && face < 6) {
                EnumFacing facing = EnumFacing.getFront(face);
                tx = x + facing.getFrontOffsetX();
                ty = y + facing.getFrontOffsetY();
                tz = z + facing.getFrontOffsetZ();
            } else {
                return false;
            }
        }

        if (ty < 0 || ty >= level.getHeight()) {
            deniedSound(player);
            return false;
        }

        double px = tx + 0.5D;
        double py = ty;
        double pz = tz + 0.5D;

        if (carry.isCarrying(CarryType.PLAYER)) {
            EntityPlayer otherPlayer = carry.getCarryingPlayer(level);
            if (otherPlayer == null || otherPlayer.isDead || otherPlayer.ridingEntity != player) {
                carry.clear();
                CarryOnDataManager.setCarryData(player, carry);
                player.removePotionEffect(Potion.moveSlowdown.id);
                return true;
            }

            if (!level
                .getCollidingBoundingBoxes(
                    otherPlayer,
                    otherPlayer.boundingBox
                        .getOffsetBoundingBox(px - otherPlayer.posX, py - otherPlayer.posY, pz - otherPlayer.posZ))
                .isEmpty() || callback != null && !callback.allow(otherPlayer)) return false;

            // Dismount the carried player from the carrier and drop them in the world.
            String command = placementCommand(carry);
            otherPlayer.mountEntity(null);
            CarryOnData data = carry;
            data.clear();
            CarryOnDataManager.setCarryData(player, data);
            CarryNetwork
                .sendStartRidingOtherToAllInDimension(player, player.getEntityId(), otherPlayer.getEntityId(), false);
            otherPlayer.setPositionAndUpdate(px, py, pz);
            player.swingItem();
            player.removePotionEffect(Potion.moveSlowdown.id);
            runPlacementCommand(player, command);
            return true;
        }

        Entity entity = carry.getEntity(level);
        if (entity == null) return false;

        entity.setPosition(px, py, pz);
        entity.rotationYaw = player.rotationYaw;
        entity.prevRotationYaw = entity.rotationYaw;

        if (!level.getCollidingBoundingBoxes(entity, entity.boundingBox)
            .isEmpty()) return false;

        boolean doPlace = callback == null || callback.allow(entity);
        if (!doPlace) return false;

        boolean spawned = level.spawnEntityInWorld(entity);
        if (!spawned) {
            entity.setDead();
            Constants.LOG.warn("PlacementHandler#tryPlaceEntity: Could not spawn entity " + entity);
            return false;
        }

        String command = placementCommand(carry);
        carry.clear();
        CarryOnDataManager.setCarryData(player, carry);
        player.swingItem();
        player.removePotionEffect(Potion.moveSlowdown.id);
        runPlacementCommand(player, command);
        return true;
    }

    public static void tryStackEntity(EntityPlayerMP player, Entity entityClicked) {
        if (!Constants.COMMON_CONFIG.settings.stackableEntities) return;

        CarryOnData carry = CarryOnDataManager.getCarryData(player);
        if (!carry.isCarrying(CarryType.ENTITY) && !carry.isCarrying(CarryType.PLAYER)) return;

        if (player.ticksExisted == carry.getTick()) return;

        World level = player.worldObj;
        Entity entityHeld;
        if (carry.isCarrying(CarryType.ENTITY)) entityHeld = carry.getEntity(level);
        else entityHeld = carry.getCarryingPlayer(level);

        if (entityHeld == null || entityHeld.isDead) return;
        if (carry.isCarrying(CarryType.PLAYER) && entityHeld.ridingEntity != player) return;

        double sizeHeldEntity = SizeHelper.getEntityHeight(entityHeld) * SizeHelper.getEntityWidth(entityHeld);
        double distance = squDist(entityClicked, player);
        if (distance > Constants.COMMON_CONFIG.settings.maxDistance * Constants.COMMON_CONFIG.settings.maxDistance
            || entityClicked == player
            || entityClicked.isDead) return;

        Entity lowestEntity = entityClicked;
        while (lowestEntity.ridingEntity != null) lowestEntity = lowestEntity.ridingEntity;
        if (lowestEntity == player || lowestEntity == entityHeld) return;

        int numPassengers = getPassengerCount(lowestEntity);
        if (numPassengers < Constants.COMMON_CONFIG.settings.maxEntityStackLimit - 1) {
            Entity topEntity = getTopPassenger(lowestEntity);

            if (topEntity == entityHeld) return;

            if (!ListHandler.isStackingPermitted(topEntity)) return;

            double sizeEntity = SizeHelper.getEntityHeight(topEntity) * SizeHelper.getEntityWidth(topEntity);
            if (!Constants.COMMON_CONFIG.settings.entitySizeMattersStacking || sizeHeldEntity <= sizeEntity) {

                if (topEntity instanceof EntityHorse) ((EntityHorse) topEntity).setHorseTamed(true);

                double tempX = entityClicked.posX;
                double tempY = entityClicked.posY;
                double tempZ = entityClicked.posZ;

                if (carry.isCarrying(CarryType.ENTITY)) {
                    entityHeld.setPosition(tempX, tempY + 2.6D, tempZ);
                    if (!level.spawnEntityInWorld(entityHeld)) {
                        deniedSound(player);
                        return;
                    }
                    entityHeld.setPosition(tempX, tempY, tempZ);
                } else /* PLAYER */ {
                    // The carried player currently rides the carrier; dismount them first.
                    entityHeld.mountEntity(null);
                    CarryNetwork.sendStartRidingOtherToAllInDimension(
                        player,
                        player.getEntityId(),
                        entityHeld.getEntityId(),
                        false);
                    entityHeld.setPosition(tempX, tempY, tempZ);
                }

                entityHeld.mountEntity(topEntity);

                String command = placementCommand(carry);
                carry.clear();
                CarryOnDataManager.setCarryData(player, carry);
                level.playSoundEffect(player.posX, player.posY, player.posZ, "mob.horse.leather", 0.5F, 1.5F);
                player.swingItem();
                player.removePotionEffect(Potion.moveSlowdown.id);
                runPlacementCommand(player, command);
            } else {
                deniedSound(player);
            }
        } else {
            deniedSound(player);
        }
    }

    /** Places the carried object when the player dies or switches dimensions. */
    public static void placeCarriedOnDeath(EntityPlayerMP oldPlayer, EntityPlayerMP newPlayer, boolean died) {
        CarryOnData carry = CarryOnDataManager.getCarryData(oldPlayer);
        CarryOnDataManager.getCarryData(newPlayer);
        if (!died || oldPlayer.worldObj.getGameRules()
            .getGameRuleBooleanValue("keepInventory")) {
            if (!carry.isCarrying(CarryType.PLAYER)) {
                // Keep the carried data on the new player (dimension change or keepInventory respawn).
                CarryOnExtendedProperties oldProps = (CarryOnExtendedProperties) oldPlayer
                    .getExtendedProperties(Constants.CARRY_DATA_PROPERTY_KEY);
                CarryOnExtendedProperties newProps = (CarryOnExtendedProperties) newPlayer
                    .getExtendedProperties(Constants.CARRY_DATA_PROPERTY_KEY);
                if (oldProps != null && newProps != null) newProps.copyFrom(oldProps);

                newPlayer.inventory.currentItem = oldPlayer.inventory.currentItem;
                return;
            }
        }

        if (carry.isCarrying()) placeCarried(oldPlayer);
        // A failed emergency placement must survive the death clone instead of disappearing.
        if (carry.isCarrying() && !carry.isCarrying(CarryType.PLAYER)) {
            CarryOnDataManager.getCarryData(newPlayer);
            CarryOnExtendedProperties properties = (CarryOnExtendedProperties) newPlayer
                .getExtendedProperties(Constants.CARRY_DATA_PROPERTY_KEY);
            properties.setCarryData(carry.clone());
            properties.getCarryData()
                .setKeyPressed(false);
            properties.getCarryData()
                .setTick(-1);
        }
    }

    public static void placeCarried(EntityPlayerMP player) {
        CarryOnData carry = CarryOnDataManager.getCarryData(player);
        if (carry.isCarrying(CarryType.ENTITY)) {
            Entity entity = carry.getEntity(player.worldObj);
            if (entity != null) {
                entity.setPosition(player.posX, player.posY, player.posZ);
                entity.rotationYaw = player.rotationYaw;
                if (!player.worldObj.spawnEntityInWorld(entity)) return;
            } else {
                return;
            }
        } else if (carry.isCarrying(CarryType.BLOCK)) {
            Block block = carry.getBlock();
            if (block == null || block == net.minecraft.init.Blocks.air) return;
            int meta = tschipp.carryon.common.carry.MetadataReorientation
                .getReorientedMetadata(block, carry.getBlockMetadata(), player);

            int x = net.minecraft.util.MathHelper.floor_double(player.posX);
            int y = net.minecraft.util.MathHelper.floor_double(player.posY);
            int z = net.minecraft.util.MathHelper.floor_double(player.posZ);
            int[] target = getDeathPlacementPos(block, player, x, y, z);
            if (target == null) {
                Constants.LOG.warn("No safe position for carried block; retaining carry data for recovery");
                return;
            }

            TileEntity blockEntity = carry.getBlockEntity(player.worldObj, target[0], target[1], target[2]);
            if (carry.getTileNbt() != null && blockEntity == null) return;
            World world = player.worldObj;
            BlockSnapshot previous = BlockSnapshot.getBlockSnapshot(world, target[0], target[1], target[2]);
            boolean capturing = world.captureBlockSnapshots;
            int snapshots = world.capturedBlockSnapshots.size();
            world.captureBlockSnapshots = true;
            boolean placed;
            try {
                placed = world.setBlock(target[0], target[1], target[2], block, meta, 3);
                if (placed && blockEntity != null) world.setTileEntity(target[0], target[1], target[2], blockEntity);
            } catch (RuntimeException error) {
                restorePlacement(previous);
                Constants.LOG.warn("Emergency placement failed; retaining carry data for recovery", error);
                return;
            } finally {
                world.captureBlockSnapshots = capturing;
                if (!capturing) while (world.capturedBlockSnapshots.size() > snapshots)
                    world.capturedBlockSnapshots.remove(world.capturedBlockSnapshots.size() - 1);
            }
            if (!placed) return;
            // Commit carry ownership before hooks can run commands or throw from another mod.
            carry.clear();
            CarryOnDataManager.setCarryData(player, carry);
            player.removePotionEffect(Potion.moveSlowdown.id);
            if (!block.hasTileEntity(meta)) block.onBlockAdded(world, target[0], target[1], target[2]);
            world.markAndNotifyBlock(target[0], target[1], target[2], null, previous.replacedBlock, block, 3);
            return;
        } else if (carry.isCarrying(CarryType.PLAYER)) {
            EntityPlayer otherPlayer = carry.getCarryingPlayer(player.worldObj);
            if (otherPlayer != null && otherPlayer.ridingEntity == player) {
                otherPlayer.mountEntity(null);
                CarryNetwork.sendStartRidingOtherToAllInDimension(
                    player,
                    player.getEntityId(),
                    otherPlayer.getEntityId(),
                    false);
            }
        }

        carry.clear();
        CarryOnDataManager.setCarryData(player, carry);
        player.removePotionEffect(Potion.moveSlowdown.id);
    }

    /** Searches for a free position near the player to safely drop a block. */
    private static int[] getDeathPlacementPos(Block block, EntityPlayerMP player, int px, int py, int pz) {
        final int DISTANCE = 15;
        World level = player.worldObj;

        List<int[]> potential = new ArrayList<int[]>(10300);
        for (int j = -DISTANCE; j <= DISTANCE; j++) for (int i = -DISTANCE; i <= DISTANCE; i++)
            for (int k = -DISTANCE; k <= DISTANCE; k++) potential.add(new int[] { px + i, py + j, pz + k });

        Collections.sort(potential, new Comparator<int[]>() {

            @Override
            public int compare(int[] a, int[] b) {
                double da = sq(a[0] + 0.5D - player.posX) + sq(a[1] + 0.5D - player.posY)
                    + sq(a[2] + 0.5D - player.posZ);
                double db = sq(b[0] + 0.5D - player.posX) + sq(b[1] + 0.5D - player.posY)
                    + sq(b[2] + 0.5D - player.posZ);
                return Double.compare(da, db);
            }
        });

        ItemStack stack = new ItemStack(net.minecraft.item.Item.getItemFromBlock(block), 1, 0);
        for (int[] target : potential) {
            if (target[1] < 0 || target[1] >= level.getHeight()) continue;

            if (level.canPlaceEntityOnSide(block, target[0], target[1], target[2], false, 1, player, stack))
                return target;
        }

        return null;
    }

    private static int getPassengerCount(Entity entity) {
        int passengers = 0;
        while (entity.riddenByEntity != null) {
            entity = entity.riddenByEntity;
            passengers++;
        }
        return passengers;
    }

    private static Entity getTopPassenger(Entity entity) {
        Entity top = entity;
        while (entity.riddenByEntity != null) {
            entity = entity.riddenByEntity;
            top = entity;
        }
        return top;
    }

    private static boolean isReplaceable(World level, int x, int y, int z) {
        Block block = level.getBlock(x, y, z);
        return block == null || level.isAirBlock(x, y, z)
            || block.getMaterial()
                .isReplaceable();
    }

    private static double squDist(Entity a, Entity b) {
        double dx = a.posX - b.posX;
        double dy = a.posY - b.posY;
        double dz = a.posZ - b.posZ;
        return dx * dx + dy * dy + dz * dz;
    }

    private static double sq(double d) {
        return d * d;
    }

    private static void deniedSound(EntityPlayerMP player) {
        player.worldObj.playSoundEffect(player.posX, player.posY, player.posZ, "random.fizz", 0.5F, 0.5F);
    }

    private static String placementCommand(CarryOnData carry) {
        return carry.getActiveScript()
            .isPresent()
            && carry.getActiveScript()
                .get().scriptEffects != null ? carry.getActiveScript()
                    .get().scriptEffects.commandPlace : "";
    }

    private static void restorePlacement(BlockSnapshot snapshot) {
        World world = snapshot.world;
        boolean restoring = world.restoringBlockSnapshots;
        boolean capturing = world.captureBlockSnapshots;
        world.restoringBlockSnapshots = true;
        world.captureBlockSnapshots = false;
        try {
            // Forge uses restoringBlockSnapshots to suppress drops from the removed temporary block.
            snapshot.restore(true, false);
        } finally {
            world.restoringBlockSnapshots = restoring;
            world.captureBlockSnapshots = capturing;
        }
    }

    private static void runPlacementCommand(EntityPlayerMP player, String command) {
        if (Constants.COMMON_CONFIG.settings.useScripts && command != null && !command.isEmpty())
            net.minecraft.server.MinecraftServer.getServer()
                .getCommandManager()
                .executeCommand(player, command);
    }

}
