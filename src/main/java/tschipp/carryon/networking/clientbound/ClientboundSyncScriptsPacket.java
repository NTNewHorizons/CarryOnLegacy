/*
 * Carry On — GNU Lesser General Public License v3 or later.
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * See LICENSE for terms and attribution to the original Carry On authors.
 */
package tschipp.carryon.networking.clientbound;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;
import tschipp.carryon.common.scripting.CarryOnScript;
import tschipp.carryon.common.scripting.ScriptManager;

public class ClientboundSyncScriptsPacket implements IMessage {

    public NBTTagCompound data = new NBTTagCompound();

    public static ClientboundSyncScriptsPacket current() {
        ClientboundSyncScriptsPacket packet = new ClientboundSyncScriptsPacket();
        NBTTagList list = new NBTTagList();
        for (CarryOnScript script : ScriptManager.SCRIPTS) list.appendTag(new NBTTagString(script.getRawJson()));
        packet.data.setTag("scripts", list);
        return packet;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        data = ByteBufUtils.readTag(buffer);
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        ByteBufUtils.writeTag(buffer, data);
    }
}
