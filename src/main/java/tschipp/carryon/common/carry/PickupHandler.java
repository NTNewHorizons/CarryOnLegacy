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

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;

import tschipp.carryon.CarryOnCommon;
import tschipp.carryon.Constants;
import tschipp.carryon.common.config.ListHandler;
import tschipp.carryon.common.pickupcondition.PickupConditionHandler;
import tschipp.carryon.common.scripting.CarryOnScript;
import tschipp.carryon.common.scripting.ScriptManager;
import tschipp.carryon.networking.CarryNetwork;
import tschipp.carryon.networking.clientbound.ClientboundStartRidingOtherPlayerPacket;
import tschipp.carryon.utils.SizeHelper;

/**
 * Server-side pickup logic, mirrors the modern PickupHandler with 1.7.10 APIs.
 */
public class PickupHandler {

    /** Callback that allows other mods to veto block pickups (used for BreakEvent compat). */
    public interface BlockPickupFilter {

        boolean allow(Block block, int meta, int x, int y, int z);
    }

    /** Callback that allows other mods to veto entity pickups (used for EntityPickupEvent compat). */
    public interface EntityPickupFilter {

        boolean allow(Entity entity);
    }

    public static boolean canCarryGeneral(EntityPlayerMP player) {
        if (player.inventory.getCurrentItem() != null) return false;

        CarryOnData carry = CarryOnDataManager.getCarryData(player);
        if (carry.isCarrying()) return false;

        if (!carry.isKeyPressed()) return false;

        // Needed so that we don't pick up and place in the same tick
        if (player.ticksExisted == carry.getTick()) return false;

        if (player.theItemInWorldManager.getGameType() == WorldSettings.GameType.ADVENTURE) return false;

        return true;
    }

    private static boolean isInDistance(EntityPlayerMP player, double x, double y, double z) {
        double maxDistance = Constants.COMMON_CONFIG.settings.maxDistance;
        double dx = player.posX - x;
        double dy = player.posY - y;
        double dz = player.posZ - z;
        return dx * dx + dy * dy + dz * dz < maxDistance * maxDistance;
    }

    public static boolean tryPickUpBlock(EntityPlayerMP player, World world, int x, int y, int z,
        @Nullable BlockPickupFilter pickupCallback) {
        if (world.isRemote) return false;

        if (!isInDistance(player, x + 0.5D, y + 0.5D, z + 0.5D)) return false;

        if (!canCarryGeneral(player)) return false;

        CarryOnData carry = CarryOnDataManager.getCarryData(player);
        Block block = world.getBlock(x, y, z);
        int meta = world.getBlockMetadata(x, y, z);

        if (block == null || block == net.minecraft.init.Blocks.air) return false;

        TileEntity tileEntity = world.getTileEntity(x, y, z);
        NBTTagCompound tileNbt = null;
        if (tileEntity != null) {
            tileNbt = new NBTTagCompound();
            tileEntity.writeToNBT(tileNbt);
        }

        java.util.Optional<CarryOnScript> result = ScriptManager.inspectBlock(block, meta, world, x, y, z, tileNbt);
        boolean overrideChecks = result.map(CarryOnScript::isOverrideChecks)
            .orElse(false);

        if (!ListHandler.isPermitted(block)) return false;

        // Reject pickup of double blocks (doors/beds; also belt-and-braces for metadata halves)
        if (block instanceof net.minecraft.block.BlockDoor || block instanceof net.minecraft.block.BlockBed
            || block instanceof net.minecraft.block.BlockDoublePlant) return false;

        if (!overrideChecks && (block.getBlockHardness(world, x, y, z) < 0 && !player.capabilities.isCreativeMode
            && !Constants.COMMON_CONFIG.settings.pickupUnbreakableBlocks)) return false;

        if (!overrideChecks && (tileEntity == null && !Constants.COMMON_CONFIG.settings.pickupAllBlocks)) return false;

        // Check if the TE is locked
        if (tileNbt != null) {
            if (tileNbt.hasKey("Lock") && !tileNbt.getString("Lock")
                .equals("")) return false;
        }

        java.util.Optional<tschipp.carryon.common.pickupcondition.PickupCondition> cond = PickupConditionHandler
            .getPickupCondition(block, meta);
        if (cond.isPresent()) {
            if (!cond.get()
                .isFulfilled(player)) return false;
        }

        boolean doPickup = pickupCallback == null ? true : pickupCallback.allow(block, meta, x, y, z);
        if (!doPickup) return false;

        if (result.isPresent()) {
            CarryOnScript script = result.get();
            if (!script.fulfillsConditions(player)) return false;

        }

        // A protection listener may replace the target while handling the pickup request.
        if (world.getBlock(x, y, z) != block || world.getBlockMetadata(x, y, z) != meta
            || world.getTileEntity(x, y, z) != tileEntity) return false;
        if (tileEntity != null) {
            tileNbt = new NBTTagCompound();
            tileEntity.writeToNBT(tileNbt);
            if (!tileNbt.getString("Lock")
                .isEmpty()) return false;
        }
        carry.setActiveScript(result.orElse(null));
        carry.setBlock(block, meta, tileNbt);

        world.removeTileEntity(x, y, z);
        if (!world.setBlockToAir(x, y, z)) {
            if (tileEntity != null) world.setTileEntity(x, y, z, tileEntity);
            carry.clear();
            return false;
        }

        CarryOnDataManager.setCarryData(player, carry);
        world.playSoundEffect(
            x + 0.5D,
            y + 0.5D,
            z + 0.5D,
            block.stepSound.func_150496_b() /* break sound */,
            1.0F,
            0.5F);
        player.swingItem();
        if (!player.capabilities.isCreativeMode || Constants.COMMON_CONFIG.settings.slownessInCreative)
            player.addPotionEffect(
                new PotionEffect(Potion.moveSlowdown.id, 100000000, CarryOnCommon.potionLevel(carry, player)));
        runPickupCommand(player, carry);
        return true;
    }

    public static boolean tryPickupEntity(EntityPlayerMP player, Entity entity,
        @Nullable EntityPickupFilter pickupCallback) {
        if (entity.worldObj.isRemote) return false;

        if (!isInDistance(player, entity.posX, entity.posY + entity.height / 2, entity.posZ)) return false;

        if (!canCarryGeneral(player)) return false;

        if (entity.hurtResistantTime != 0) return false;

        if (entity.isDead) return false;

        if (entity == player
            || (!(entity instanceof EntityPlayer) && net.minecraft.entity.EntityList.getEntityString(entity) == null))
            return false;
        // Do not detach an existing rider or create a carrier/rider cycle.
        if (player.riddenByEntity != null || player.ridingEntity != null) return false;

        if (entity instanceof net.minecraft.entity.passive.EntityTameable) {
            net.minecraft.entity.passive.EntityTameable tame = (net.minecraft.entity.passive.EntityTameable) entity;
            EntityLivingBase owner = tame.getOwner();
            if (owner != null && owner != player
                && owner.getUniqueID() != null
                && !owner.getUniqueID()
                    .equals(player.getUniqueID()))
                return false;
        }

        java.util.Optional<CarryOnScript> result = ScriptManager.inspectEntity(entity);
        boolean overrideChecks = result.map(CarryOnScript::isOverrideChecks)
            .orElse(false);

        if (!ListHandler.isPermitted(entity)) {
            // We can pick up baby animals even if the grown up animal is blacklisted.
            if (!overrideChecks && !(entity instanceof EntityAgeable && Constants.COMMON_CONFIG.settings.allowBabies
                && ((EntityAgeable) entity).isChild())) return false;
        }

        // Non-Creative only guards
        if (!player.capabilities.isCreativeMode) {
            if (!overrideChecks && (!Constants.COMMON_CONFIG.settings.pickupHostileMobs
                && entity instanceof net.minecraft.entity.monster.IMob)) return false;

            if (Constants.COMMON_CONFIG.settings.maxEntityHeight < SizeHelper.getRelativeEntityHeight(player, entity)
                || Constants.COMMON_CONFIG.settings.maxEntityWidth < SizeHelper.getRelativeEntityWidth(player, entity))
                return false;
        }

        java.util.Optional<tschipp.carryon.common.pickupcondition.PickupCondition> cond = PickupConditionHandler
            .getPickupCondition(entity);
        if (cond.isPresent()) {
            if (!cond.get()
                .isFulfilled(player)) return false;
        }

        boolean doPickup = pickupCallback == null || pickupCallback.allow(entity);
        if (!doPickup) return false;

        CarryOnData carry = CarryOnDataManager.getCarryData(player);

        if (result.isPresent()) {
            CarryOnScript script = result.get();
            if (!script.fulfillsConditions(player)) return false;

        }

        World world = entity.worldObj;

        if (entity instanceof EntityPlayer) {
            EntityPlayer otherPlayer = (EntityPlayer) entity;
            if (!Constants.COMMON_CONFIG.settings.pickupPlayers) return false;

            if (!player.capabilities.isCreativeMode && otherPlayer.capabilities.isCreativeMode) return false;

            carry.setActiveScript(result.orElse(null));

            if (otherPlayer.riddenByEntity != null) otherPlayer.riddenByEntity.mountEntity(null);
            otherPlayer.mountEntity(null);

            otherPlayer.mountEntity(player);
            CarryNetwork.sendPacketToAllInDimension(
                new ClientboundStartRidingOtherPlayerPacket(player.getEntityId(), otherPlayer.getEntityId(), true),
                world);
            carry.setCarryingPlayer(otherPlayer);
            player.swingItem();
            world.playSoundEffect(player.posX, player.posY, player.posZ, "mob.horse.leather", 1.0F, 0.5F);
            CarryOnDataManager.setCarryData(player, carry);
            if (!player.capabilities.isCreativeMode || Constants.COMMON_CONFIG.settings.slownessInCreative)
                player.addPotionEffect(
                    new PotionEffect(Potion.moveSlowdown.id, 100000000, CarryOnCommon.potionLevel(carry, player)));
            runPickupCommand(player, carry);
            return true;
        }

        if (entity.riddenByEntity != null) entity.riddenByEntity.mountEntity(null);
        entity.mountEntity(null);

        if (entity instanceof EntityLiving) {
            EntityLiving living = (EntityLiving) entity;
            try {
                if (living.getLeashed()) living.clearLeashed(true, false);
            } catch (Exception e) {}
        }

        carry.setActiveScript(result.orElse(null));
        carry.setEntity(entity);
        entity.setDead();

        world.playSoundEffect(player.posX, player.posY, player.posZ, "mob.horse.leather", 1.0F, 0.5F);
        CarryOnDataManager.setCarryData(player, carry);
        player.swingItem();
        if (!player.capabilities.isCreativeMode || Constants.COMMON_CONFIG.settings.slownessInCreative)
            player.addPotionEffect(
                new PotionEffect(Potion.moveSlowdown.id, 100000000, CarryOnCommon.potionLevel(carry, player)));
        runPickupCommand(player, carry);
        return true;
    }

    private static void runPickupCommand(EntityPlayerMP player, CarryOnData carry) {
        if (!Constants.COMMON_CONFIG.settings.useScripts || !carry.getActiveScript()
            .isPresent()
            || carry.getActiveScript()
                .get().scriptEffects == null)
            return;
        String command = carry.getActiveScript()
            .get().scriptEffects.commandPickup;
        if (command != null && !command.isEmpty()) MinecraftServer.getServer()
            .getCommandManager()
            .executeCommand(player, command);
    }

}
