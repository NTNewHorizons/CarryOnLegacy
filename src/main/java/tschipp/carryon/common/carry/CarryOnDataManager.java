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

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

import tschipp.carryon.Constants;
import tschipp.carryon.networking.CarryNetwork;

public class CarryOnDataManager {

    public static CarryOnData getCarryData(EntityPlayer player) {
        CarryOnExtendedProperties props = (CarryOnExtendedProperties) player
            .getExtendedProperties(Constants.CARRY_DATA_PROPERTY_KEY);
        if (props == null) {
            props = new CarryOnExtendedProperties();
            player.registerExtendedProperties(Constants.CARRY_DATA_PROPERTY_KEY, props);
        }
        return props.getCarryData();
    }

    public static void setCarryData(EntityPlayer player, CarryOnData data) {
        // Client synchronization must preserve the server's locked slot and pickup tick.
        if (!player.worldObj.isRemote) {
            data.setSelected(player.inventory.currentItem != -1 ? player.inventory.currentItem : 0);
            data.setTick(player.ticksExisted);
        }

        CarryOnExtendedProperties props = (CarryOnExtendedProperties) player
            .getExtendedProperties(Constants.CARRY_DATA_PROPERTY_KEY);
        if (props == null) {
            props = new CarryOnExtendedProperties();
            player.registerExtendedProperties(Constants.CARRY_DATA_PROPERTY_KEY, props);
        }
        props.setCarryData(data);

        if (!player.worldObj.isRemote) CarryNetwork.sendSyncCarryDataToDimension((EntityPlayerMP) player);
    }

    /** Syncs the carry data of a specific player (used for join/tracking buffs). */
    public static void syncCarryDataTo(EntityPlayerMP target, EntityPlayerMP owner) {
        CarryNetwork.sendSyncCarryDataToPlayer(target, owner);
    }

}
