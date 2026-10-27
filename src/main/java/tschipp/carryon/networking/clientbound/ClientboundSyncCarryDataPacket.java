/*
 * Carry On — GNU Lesser General Public License v3 or later.
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * See LICENSE for terms and attribution to the original Carry On authors.
 */
package tschipp.carryon.networking.clientbound;

import net.minecraft.nbt.NBTTagCompound;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

public class ClientboundSyncCarryDataPacket implements IMessage {

    public int entityId;
    public NBTTagCompound data;

    public ClientboundSyncCarryDataPacket() {}

    public ClientboundSyncCarryDataPacket(int entityId, NBTTagCompound data) {
        this.entityId = entityId;
        this.data = data;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        entityId = buffer.readInt();
        data = ByteBufUtils.readTag(buffer);
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(entityId);
        ByteBufUtils.writeTag(buffer, data);
    }
}
