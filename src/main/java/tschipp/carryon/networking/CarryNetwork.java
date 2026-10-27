/*
 * Carry On — GNU Lesser General Public License v3 or later.
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * See LICENSE for terms and attribution to the original Carry On authors.
 */
package tschipp.carryon.networking;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import tschipp.carryon.CarryOn;
import tschipp.carryon.common.carry.CarryOnDataManager;
import tschipp.carryon.networking.clientbound.ClientboundStartRidingOtherPlayerPacket;
import tschipp.carryon.networking.clientbound.ClientboundStartRidingPacket;
import tschipp.carryon.networking.clientbound.ClientboundSyncCarryDataPacket;
import tschipp.carryon.networking.clientbound.ClientboundSyncScriptsPacket;
import tschipp.carryon.networking.serverbound.ServerboundCarryKeyPressedPacket;

public final class CarryNetwork {

    public static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel("carryon");
    private static final Queue<Runnable> SERVER_TASKS = new ConcurrentLinkedQueue<Runnable>();

    public static void register() {
        CHANNEL.registerMessage(KeyHandler.class, ServerboundCarryKeyPressedPacket.class, 0, Side.SERVER);
        // Forge 1.7.10 uses the handler class name as the Netty pipeline key.
        CHANNEL.registerMessage(new CarryDataHandler(), ClientboundSyncCarryDataPacket.class, 1, Side.CLIENT);
        CHANNEL.registerMessage(new StartRidingHandler(), ClientboundStartRidingPacket.class, 2, Side.CLIENT);
        CHANNEL.registerMessage(
            new OtherPlayerRidingHandler(),
            ClientboundStartRidingOtherPlayerPacket.class,
            3,
            Side.CLIENT);
        CHANNEL.registerMessage(new ScriptsHandler(), ClientboundSyncScriptsPacket.class, 4, Side.CLIENT);
    }

    public static void runServerTasks() {
        Runnable task;
        while ((task = SERVER_TASKS.poll()) != null) task.run();
    }

    public static void clearServerTasks() {
        SERVER_TASKS.clear();
    }

    public static void sendPacketToAllInDimension(IMessage message, World world) {
        CHANNEL.sendToDimension(message, world.provider.dimensionId);
    }

    public static void sendSyncCarryDataToDimension(EntityPlayerMP owner) {
        sendPacketToAllInDimension(
            new ClientboundSyncCarryDataPacket(
                owner.getEntityId(),
                CarryOnDataManager.getCarryData(owner)
                    .getNbt()),
            owner.worldObj);
    }

    public static void sendSyncCarryDataToPlayer(EntityPlayerMP target, EntityPlayerMP owner) {
        CHANNEL.sendTo(
            new ClientboundSyncCarryDataPacket(
                owner.getEntityId(),
                CarryOnDataManager.getCarryData(owner)
                    .getNbt()),
            target);
    }

    public static void sendStartRidingOtherToAllInDimension(EntityPlayerMP owner, int mount, int rider, boolean ride) {
        sendPacketToAllInDimension(new ClientboundStartRidingOtherPlayerPacket(mount, rider, ride), owner.worldObj);
    }

    public static class KeyHandler implements IMessageHandler<ServerboundCarryKeyPressedPacket, IMessage> {

        @Override
        public IMessage onMessage(ServerboundCarryKeyPressedPacket message, MessageContext context) {
            final EntityPlayerMP player = context.getServerHandler().playerEntity;
            final boolean pressed = message.pressed;
            SERVER_TASKS.add(() -> {
                if (!player.isDead && player.worldObj != null) {
                    // Key changes must not reset the pickup/place tick guard or locked hotbar slot.
                    CarryOnDataManager.getCarryData(player)
                        .setKeyPressed(pressed);
                }
            });
            return null;
        }
    }

    public static final class CarryDataHandler extends ClientHandler {
    }

    public static final class StartRidingHandler extends ClientHandler {
    }

    public static final class OtherPlayerRidingHandler extends ClientHandler {
    }

    public static final class ScriptsHandler extends ClientHandler {
    }

    /** Safe to load on a dedicated server; all Minecraft client access lives in ClientProxy. */
    public static class ClientHandler implements IMessageHandler<IMessage, IMessage> {

        @Override
        public IMessage onMessage(IMessage message, MessageContext context) {
            CarryOn.proxy.receiveClientPacket(message);
            return null;
        }
    }
}
