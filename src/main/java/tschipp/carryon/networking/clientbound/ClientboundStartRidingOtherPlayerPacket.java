/*
 * Carry On — GNU Lesser General Public License v3 or later.
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * See LICENSE for terms and attribution to the original Carry On authors.
 */
package tschipp.carryon.networking.clientbound;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

public class ClientboundStartRidingOtherPlayerPacket implements IMessage {

    public int mount;
    public int rider;
    public boolean ride;

    public ClientboundStartRidingOtherPlayerPacket() {}

    public ClientboundStartRidingOtherPlayerPacket(int mount, int rider, boolean ride) {
        this.mount = mount;
        this.rider = rider;
        this.ride = ride;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        mount = buffer.readInt();
        rider = buffer.readInt();
        ride = buffer.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(mount);
        buffer.writeInt(rider);
        buffer.writeBoolean(ride);
    }
}
