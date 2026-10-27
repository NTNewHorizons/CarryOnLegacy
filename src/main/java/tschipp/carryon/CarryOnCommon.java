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

package tschipp.carryon;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;

import tschipp.carryon.common.carry.CarryOnData;
import tschipp.carryon.common.carry.CarryOnDataManager;
import tschipp.carryon.common.carry.CarryType;
import tschipp.carryon.common.carry.PlacementHandler;
import tschipp.carryon.utils.SizeHelper;

/**
 * Shared common logic (ported from modern CarryOnCommon).
 */
public class CarryOnCommon {

    public static void registerConfigs() {
        // Done through ConfigLoader in the @Mod class (Forge Configuration system on 1.7.10).
    }

    public static void onCarryTick(EntityPlayerMP player) {
        CarryOnData carry = CarryOnDataManager.getCarryData(player);
        if (carry.isCarrying(CarryType.PLAYER)) {
            EntityPlayer passenger = carry.getCarryingPlayer(player.worldObj);
            // Vanilla sneak, dimension travel, and other mods can dismount without our placement path.
            // Drop only the stale relationship: never dismount an unrelated player from a new mount.
            if (passenger == null || passenger.isDead || passenger.ridingEntity != player) {
                carry.clear();
                CarryOnDataManager.setCarryData(player, carry);
                player.removePotionEffect(net.minecraft.potion.Potion.moveSlowdown.id);
            }
        }
        if (carry.isCarrying()) {
            // Dumb fix to sync carry data after a respawn with keepInventory, because we can't sync in the first tick.
            if (player.ticksExisted == 1) tschipp.carryon.networking.CarryNetwork.sendSyncCarryDataToDimension(player);

            if (Constants.COMMON_CONFIG.settings.useScripts && carry.getActiveScript()
                .isPresent()) {
                String cmd = carry.getActiveScript()
                    .get().scriptEffects == null ? ""
                        : carry.getActiveScript()
                            .get().scriptEffects.commandLoop;
                if (cmd != null && !cmd.isEmpty()) MinecraftServerCommands.executeAsPlayer(player, cmd);
            }

            InventoryPlayer inv = player.inventory;
            if (carry.getSelected() >= 0 && carry.getSelected() < 9) inv.currentItem = carry.getSelected();
        }
    }

    /**
     * Returns true if the block can be broken.
     */
    public static boolean onTryBreakBlock(EntityPlayer player) {
        if (player != null && !Constants.COMMON_CONFIG.settings.hitWhileCarrying) {
            CarryOnData carry = CarryOnDataManager.getCarryData(player);
            if (carry.isCarrying()) return false;
        }
        return true;
    }

    /**
     * Returns true if the entity can be attacked.
     */
    public static boolean onAttackedByPlayer(EntityPlayer player) {
        if (player != null && !Constants.COMMON_CONFIG.settings.hitWhileCarrying) {
            CarryOnData carry = CarryOnDataManager.getCarryData(player);
            if (carry.isCarrying()) return false;
        }
        return true;
    }

    public static void onPlayerAttacked(EntityPlayer player) {
        if (Constants.COMMON_CONFIG.settings.dropCarriedWhenHit && !player.worldObj.isRemote) {
            CarryOnData carry = CarryOnDataManager.getCarryData(player);
            if (carry.isCarrying()) {
                PlacementHandler.placeCarried((EntityPlayerMP) player);
            }
        }
    }

    public static void onRiderDisconnected(EntityPlayer rider) {
        if (rider.ridingEntity instanceof EntityPlayerMP) {
            EntityPlayerMP vehicle = (EntityPlayerMP) rider.ridingEntity;
            CarryOnData data = CarryOnDataManager.getCarryData(vehicle);
            if (data.isCarrying(CarryType.PLAYER)) {
                PlacementHandler.placeCarried(vehicle);
            }
        }
    }

    public static int potionLevel(CarryOnData carry, EntityPlayer player) {
        if (carry.isCarrying(CarryType.PLAYER)) return 1;

        if (carry.isCarrying(CarryType.ENTITY)) {
            Entity entity = carry.getEntity(player.worldObj);
            if (entity == null) return 0;
            int i = 1;
            if (Constants.COMMON_CONFIG.settings.heavyEntities)
                i = (int) SizeHelper.getRelativeEntityArea(player, entity);

            i = (int) (i * Constants.COMMON_CONFIG.settings.entitySlownessMultiplier);
            if (i > 4) i = 4;
            return i;
        }

        if (carry.isCarrying(CarryType.BLOCK)) {
            int i = 1;
            if (Constants.COMMON_CONFIG.settings.heavyTiles) {
                String nbt = carry.getNbt()
                    .toString();
                i = nbt.length() / 500;
            }

            i = (int) (i * Constants.COMMON_CONFIG.settings.blockSlownessMultiplier);

            if (i > 4) i = 4;
            return i;
        }

        return 0;
    }

    /** Small indirection so carry logic does not depend on command plumbing directly. */
    private static class MinecraftServerCommands {

        static void executeAsPlayer(EntityPlayerMP player, String cmd) {
            net.minecraft.server.MinecraftServer.getServer()
                .getCommandManager()
                .executeCommand(player, cmd);
        }
    }

}
