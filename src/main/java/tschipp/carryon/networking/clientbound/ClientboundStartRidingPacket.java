/*
 * Carry On — GNU Lesser General Public License v3 or later.
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * See LICENSE for terms and attribution to the original Carry On authors.
 */
package tschipp.carryon.networking.clientbound;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

public class ClientboundStartRidingPacket implements IMessage {

    public int rider;
    public boolean ride;

    public ClientboundStartRidingPacket() {}

    public ClientboundStartRidingPacket(int rider, boolean ride) {
        this.rider = rider;
        this.ride = ride;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        rider = buffer.readInt();
        ride = buffer.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(rider);
        buffer.writeBoolean(ride);
    }
}
