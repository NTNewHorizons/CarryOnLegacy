/*
 * Carry On — GNU Lesser General Public License v3 or later.
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * See LICENSE for terms and attribution to the original Carry On authors.
 */
package tschipp.carryon.events;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.EntityInteractEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import tschipp.carryon.CarryOnCommon;
import tschipp.carryon.common.carry.CarryOnData;
import tschipp.carryon.common.carry.CarryOnDataManager;
import tschipp.carryon.common.carry.CarryType;
import tschipp.carryon.common.carry.PickupHandler;
import tschipp.carryon.common.carry.PlacementHandler;
import tschipp.carryon.networking.CarryNetwork;
import tschipp.carryon.networking.clientbound.ClientboundSyncScriptsPacket;

public class CommonEvents {

    @SubscribeEvent
    public void constructing(EntityEvent.EntityConstructing event) {
        if (event.entity instanceof EntityPlayer) CarryOnDataManager.getCarryData((EntityPlayer) event.entity);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void interactBlock(PlayerInteractEvent event) {
        if (event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK || event.world.isRemote
            || !(event.entityPlayer instanceof EntityPlayerMP)) return;
        CarryNetwork.runServerTasks();
        EntityPlayerMP player = (EntityPlayerMP) event.entityPlayer;
        CarryOnData data = CarryOnDataManager.getCarryData(player);
        boolean consume = data.isCarrying();
        if (!consume) {
            consume = PickupHandler.tryPickUpBlock(
                player,
                event.world,
                event.x,
                event.y,
                event.z,
                (block, meta, x, y, z) -> !MinecraftForge.EVENT_BUS
                    .post(new BlockEvent.BreakEvent(x, y, z, event.world, block, meta, player)));
        } else if (data.isCarrying(CarryType.BLOCK)) {
            PlacementHandler.tryPlaceBlock(
                player,
                event.world,
                event.x,
                event.y,
                event.z,
                event.face,
                previous -> !MinecraftForge.EVENT_BUS.post(
                    new BlockEvent.PlaceEvent(previous, event.world.getBlock(event.x, event.y, event.z), player)));
        } else {
            PlacementHandler.tryPlaceEntity(player, event.world, event.x, event.y, event.z, event.face, null);
        }
        if (consume) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void interactEntity(EntityInteractEvent event) {
        if (event.entityPlayer.worldObj.isRemote || !(event.entityPlayer instanceof EntityPlayerMP)) return;
        CarryNetwork.runServerTasks();
        EntityPlayerMP player = (EntityPlayerMP) event.entityPlayer;
        CarryOnData data = CarryOnDataManager.getCarryData(player);
        if (!data.isCarrying()) {
            if (PickupHandler.tryPickupEntity(
                player,
                event.target,
                entity -> !MinecraftForge.EVENT_BUS.post(new EntityPickupEvent(player, entity))))
                event.setCanceled(true);
        } else {
            PlacementHandler.tryStackEntity(player, event.target);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void breaking(BlockEvent.BreakEvent event) {
        if (!CarryOnCommon.onTryBreakBlock(event.getPlayer())) event.setCanceled(true);
    }

    @SubscribeEvent
    public void breakSpeed(PlayerEvent.BreakSpeed event) {
        if (!CarryOnCommon.onTryBreakBlock(event.entityPlayer)) event.newSpeed = 0;
    }

    @SubscribeEvent
    public void attacking(AttackEntityEvent event) {
        if (!CarryOnCommon.onAttackedByPlayer(event.entityPlayer)) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void attacked(LivingAttackEvent event) {
        if (event.entityLiving instanceof EntityPlayerMP)
            CarryOnCommon.onPlayerAttacked((EntityPlayerMP) event.entityLiving);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void death(LivingDeathEvent event) {
        if (event.entityLiving instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) event.entityLiving;
            CarryOnCommon.onRiderDisconnected(player);
            if (!player.worldObj.getGameRules()
                .getGameRuleBooleanValue("keepInventory")) PlacementHandler.placeCarried(player);
        }
    }

    @SubscribeEvent
    public void clonePlayer(PlayerEvent.Clone event) {
        if (event.original instanceof EntityPlayerMP && event.entityPlayer instanceof EntityPlayerMP) PlacementHandler
            .placeCarriedOnDeath((EntityPlayerMP) event.original, (EntityPlayerMP) event.entityPlayer, event.wasDeath);
    }

    @SubscribeEvent
    public void tracking(PlayerEvent.StartTracking event) {
        if (event.entityPlayer instanceof EntityPlayerMP && event.target instanceof EntityPlayerMP)
            CarryOnDataManager.syncCarryDataTo((EntityPlayerMP) event.entityPlayer, (EntityPlayerMP) event.target);
    }

    @SubscribeEvent
    public void join(EntityJoinWorldEvent event) {
        if (event.entity instanceof EntityPlayerMP) CarryOnDataManager.getCarryData((EntityPlayerMP) event.entity)
            .setKeyPressed(false);
    }

    @SubscribeEvent
    public void login(cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent event) {
        sync(event.player);
    }

    @SubscribeEvent
    public void respawn(cpw.mods.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent event) {
        sync(event.player);
    }

    @SubscribeEvent
    public void dimension(cpw.mods.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent event) {
        sync(event.player);
    }

    private void sync(EntityPlayer player) {
        if (player instanceof EntityPlayerMP) {
            EntityPlayerMP target = (EntityPlayerMP) player;
            CarryNetwork.sendSyncCarryDataToDimension(target);
            for (Object object : target.worldObj.playerEntities) {
                if (object instanceof EntityPlayerMP && object != target)
                    CarryNetwork.sendSyncCarryDataToPlayer(target, (EntityPlayerMP) object);
            }
            CarryNetwork.CHANNEL.sendTo(ClientboundSyncScriptsPacket.current(), target);
        }
    }

    @SubscribeEvent
    public void logout(cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent event) {
        CarryOnCommon.onRiderDisconnected(event.player);
        CarryOnDataManager.getCarryData(event.player)
            .setKeyPressed(false);
        CarryOnData data = CarryOnDataManager.getCarryData(event.player);
        if (data.isCarrying(CarryType.PLAYER) && event.player instanceof EntityPlayerMP)
            PlacementHandler.placeCarried((EntityPlayerMP) event.player);
    }

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        CarryNetwork.runServerTasks();
        for (Object player : net.minecraft.server.MinecraftServer.getServer()
            .getConfigurationManager().playerEntityList) CarryOnCommon.onCarryTick((EntityPlayerMP) player);
    }
}
