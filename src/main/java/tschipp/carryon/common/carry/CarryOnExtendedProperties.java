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

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.IExtendedEntityProperties;

/**
 * 1.7.10 replacement for the modern Forge capability. Persisted into the player's NBT
 * under the "CarryOnData" tag via IExtendedEntityProperties.
 */
public class CarryOnExtendedProperties implements IExtendedEntityProperties {

    private CarryOnData carry = new CarryOnData();

    @Override
    public void init(Entity entity, World world) {}

    @Override
    public void saveNBTData(NBTTagCompound compound) {
        compound.setTag(CarryOnData.SERIALIZATION_KEY, carry.getNbt());
    }

    @Override
    public void loadNBTData(NBTTagCompound compound) {
        if (compound.hasKey(CarryOnData.SERIALIZATION_KEY))
            carry = CarryOnData.fromNbt(compound.getCompoundTag(CarryOnData.SERIALIZATION_KEY));
        else carry = new CarryOnData();
        // Input is connection-local, not persistent. A saved pickup tick belongs to an old entity.
        carry.setKeyPressed(false);
        carry.setTick(-1);
    }

    public CarryOnData getCarryData() {
        return carry;
    }

    public void setCarryData(CarryOnData data) {
        this.carry = data;
    }

    /** Copies data from another player's properties (used for death clones with keepInventory). */
    public void copyFrom(CarryOnExtendedProperties other) {
        this.carry = other.carry.clone();
        this.carry.setKeyPressed(false);
        this.carry.setTick(-1);
    }

}
