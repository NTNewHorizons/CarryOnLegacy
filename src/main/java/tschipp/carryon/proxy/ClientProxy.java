/*
 * Carry On — GNU Lesser General Public License v3 or later.
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * See LICENSE for terms and attribution to the original Carry On authors.
 */
package tschipp.carryon.proxy;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagList;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import tschipp.carryon.Constants;
import tschipp.carryon.client.keybinds.CarryOnKeybinds;
import tschipp.carryon.common.carry.CarryOnData;
import tschipp.carryon.common.carry.CarryOnDataManager;
import tschipp.carryon.common.config.ConfigLoader;
import tschipp.carryon.common.scripting.CarryOnScript;
import tschipp.carryon.common.scripting.ScriptManager;
import tschipp.carryon.common.scripting.ScriptParser;
import tschipp.carryon.events.ClientEvents;
import tschipp.carryon.networking.clientbound.ClientboundStartRidingOtherPlayerPacket;
import tschipp.carryon.networking.clientbound.ClientboundStartRidingPacket;
import tschipp.carryon.networking.clientbound.ClientboundSyncCarryDataPacket;
import tschipp.carryon.networking.clientbound.ClientboundSyncScriptsPacket;

public class ClientProxy extends CommonProxy {

    @Override
    public void init() {
        tschipp.carryon.client.modeloverride.ModelOverrideHandler.initialize();
    }

    private static final Queue<IMessage> PACKETS = new ConcurrentLinkedQueue<IMessage>();
    private static final Map<Integer, ClientboundSyncCarryDataPacket> PENDING_CARRY = new HashMap<Integer, ClientboundSyncCarryDataPacket>();
    private static final Map<Integer, Integer> CARRY_EXPIRY = new HashMap<Integer, Integer>();
    private static final Map<Integer, PendingRiding> PENDING_RIDING = new HashMap<Integer, PendingRiding>();
    private static Object packetWorld;
    private static int pendingTicks;

    @Override
    public void preInit(File configDirectory) {
        ConfigLoader.loadClient(new File(configDirectory, "carryon-client.cfg"));
        CarryOnKeybinds.register();
        FMLCommonHandler.instance()
            .bus()
            .register(new ClientEvents());
        net.minecraftforge.common.MinecraftForge.EVENT_BUS
            .register(new tschipp.carryon.client.render.CarriedObjectRender());
        tschipp.carryon.client.render.CarryRenderHelper arms = new tschipp.carryon.client.render.CarryRenderHelper();
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(arms);
        FMLCommonHandler.instance()
            .bus()
            .register(arms);
    }

    @Override
    public void receiveClientPacket(IMessage message) {
        PACKETS.add(message);
    }

    public static void drainPackets() {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.theWorld == null || minecraft.thePlayer == null) {
            PACKETS.clear();
            PENDING_CARRY.clear();
            CARRY_EXPIRY.clear();
            PENDING_RIDING.clear();
            packetWorld = null;
            return;
        }
        if (packetWorld != minecraft.theWorld) {
            PENDING_CARRY.clear();
            CARRY_EXPIRY.clear();
            PENDING_RIDING.clear();
            packetWorld = minecraft.theWorld;
            pendingTicks = 0;
        }
        pendingTicks++;
        IMessage message;
        while ((message = PACKETS.poll()) != null) {
            if (message instanceof ClientboundSyncCarryDataPacket) {
                ClientboundSyncCarryDataPacket packet = (ClientboundSyncCarryDataPacket) message;
                if (packet.data != null) {
                    if (PENDING_CARRY.size() >= 1024 && !PENDING_CARRY.containsKey(packet.entityId)) {
                        PENDING_CARRY.clear();
                        CARRY_EXPIRY.clear();
                    }
                    PENDING_CARRY.put(packet.entityId, packet);
                    CARRY_EXPIRY.put(packet.entityId, pendingTicks + 200);
                }
            } else if (message instanceof ClientboundStartRidingOtherPlayerPacket) {
                ClientboundStartRidingOtherPlayerPacket packet = (ClientboundStartRidingOtherPlayerPacket) message;
                queueRiding(packet.rider, packet.mount, packet.ride);
            } else if (message instanceof ClientboundStartRidingPacket) {
                ClientboundStartRidingPacket packet = (ClientboundStartRidingPacket) message;
                queueRiding(packet.rider, minecraft.thePlayer.getEntityId(), packet.ride);
            } else if (message instanceof ClientboundSyncScriptsPacket) {
                ClientboundSyncScriptsPacket packet = (ClientboundSyncScriptsPacket) message;
                // Integrated servers share static script state with the client; the payload was already copied.
                if (packet.data == null) continue;
                if (minecraft.isIntegratedServerRunning()) continue;
                NBTTagList list = packet.data.getTagList("scripts", 8);
                ScriptManager.SCRIPTS.clear();
                for (int i = 0; i < list.tagCount(); i++) {
                    try {
                        CarryOnScript script = ScriptParser.fromJson(list.getStringTagAt(i));
                        if (script.isValid()) ScriptManager.SCRIPTS.add(script);
                    } catch (RuntimeException error) {
                        Constants.LOG.warn("Invalid synchronized Carry On script", error);
                    }
                }
            }
        }
        // Tracking sync may precede vanilla's remote-player spawn packet. Apply on a later tick
        // rather than dropping the carry snapshot forever until the next carry action.
        Iterator<Map.Entry<Integer, ClientboundSyncCarryDataPacket>> pending = PENDING_CARRY.entrySet()
            .iterator();
        while (pending.hasNext()) {
            Map.Entry<Integer, ClientboundSyncCarryDataPacket> entry = pending.next();
            Entity entity = minecraft.theWorld.getEntityByID(entry.getKey());
            if (entity instanceof EntityPlayer) {
                CarryOnDataManager.setCarryData((EntityPlayer) entity, CarryOnData.fromNbt(entry.getValue().data));
                pending.remove();
                CARRY_EXPIRY.remove(entry.getKey());
            } else if (pendingTicks - CARRY_EXPIRY.get(entry.getKey()) >= 0) {
                pending.remove();
                CARRY_EXPIRY.remove(entry.getKey());
            }
        }
        Iterator<Map.Entry<Integer, PendingRiding>> riding = PENDING_RIDING.entrySet()
            .iterator();
        while (riding.hasNext()) {
            Map.Entry<Integer, PendingRiding> entry = riding.next();
            PendingRiding update = entry.getValue();
            Entity rider = minecraft.theWorld.getEntityByID(entry.getKey());
            Entity mount = minecraft.theWorld.getEntityByID(update.mount);
            if (rider != null && (!update.ride || mount != null)) {
                if (rider.ridingEntity != (update.ride ? mount : null)) rider.mountEntity(update.ride ? mount : null);
                riding.remove();
            } else if (pendingTicks - update.expires >= 0) riding.remove();
        }
    }

    private static void queueRiding(int rider, int mount, boolean ride) {
        if (PENDING_RIDING.size() >= 1024 && !PENDING_RIDING.containsKey(rider)) PENDING_RIDING.clear();
        // A newer dismount replaces any unresolved mount for this rider.
        PENDING_RIDING.put(rider, new PendingRiding(mount, ride, pendingTicks + 200));
    }

    private static final class PendingRiding {

        final int mount;
        final boolean ride;
        final int expires;

        PendingRiding(int mount, boolean ride, int expires) {
            this.mount = mount;
            this.ride = ride;
            this.expires = expires;
        }
    }
}
