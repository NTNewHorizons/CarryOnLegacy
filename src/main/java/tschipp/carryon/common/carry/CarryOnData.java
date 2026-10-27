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

import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import tschipp.carryon.Constants;
import tschipp.carryon.common.scripting.CarryOnScript;

/**
 * Stores the object currently being carried by a player. Serializes itself into NBT.
 *
 * 1.7.10 deviations from modern Carry On:
 * - Blocks are stored as registry-name + metadata (there is no BlockState system here).
 * - The active script is stored as raw JSON (parsed with Gson server-side).
 */
public class CarryOnData {

    private CarryType type;
    private NBTTagCompound nbt;
    private boolean keyPressed = false;
    private CarryOnScript activeScript;
    private int selectedSlot = 0;

    public static final String SERIALIZATION_KEY = "CarryOnData";

    public CarryOnData() {
        this.type = CarryType.INVALID;
        this.nbt = new NBTTagCompound();
    }

    public static CarryOnData fromNbt(NBTTagCompound data) {
        CarryOnData carry = new CarryOnData();
        carry.nbt = (NBTTagCompound) data.copy();

        try {
            carry.type = data.hasKey("type") ? CarryType.valueOf(data.getString("type")) : CarryType.INVALID;
        } catch (IllegalArgumentException error) {
            Constants.LOG.warn("Unknown Carry On data type; retaining NBT for recovery", error);
            carry.type = CarryType.INVALID;
        }

        carry.keyPressed = data.getBoolean("keyPressed");
        carry.selectedSlot = Math.max(0, Math.min(8, data.getInteger("selected")));
        carry.nbt.setInteger("selected", carry.selectedSlot);

        if (data.hasKey("activeScript")) {
            try {
                carry.activeScript = tschipp.carryon.common.scripting.ScriptParser
                    .fromJson(data.getString("activeScript"));
            } catch (Exception e) {
                Constants.LOG
                    .warn("Failed to decode activeScript during CarryOnData deserialization: " + e.getMessage());
                carry.activeScript = null;
            }
        }

        return carry;
    }

    public NBTTagCompound getNbt() {
        NBTTagCompound copy = (NBTTagCompound) nbt.copy();
        return copy;
    }

    public CarryType getType() {
        return this.type;
    }

    /** Compound holding block id/meta, only valid for BLOCK carries. */
    public NBTTagCompound getBlockContentNbt() {
        if (type == CarryType.BLOCK && nbt.hasKey("block")) return (NBTTagCompound) nbt.getCompoundTag("block")
            .copy();
        return new NBTTagCompound();
    }

    /** Full TE NBT (with "id"), only valid for BLOCK carries. */
    public NBTTagCompound getTileNbt() {
        if (type == CarryType.BLOCK && nbt.hasKey("tile")) return (NBTTagCompound) nbt.getCompoundTag("tile")
            .copy();
        return null;
    }

    public void setBlock(Block block, int meta, NBTTagCompound tileNbt) {
        this.type = CarryType.BLOCK;
        NBTTagCompound blockData = new NBTTagCompound();
        String name = toRegistryName(block);
        if (name == null) name = "minecraft:air";
        blockData.setString("id", name);
        blockData.setInteger("meta", meta);
        this.nbt.setTag("block", blockData);
        if (tileNbt != null) this.nbt.setTag("tile", tileNbt.copy());
        else this.nbt.removeTag("tile");
        this.nbt.setString("type", this.type.toString());
    }

    public Block getBlock() {
        if (this.type != CarryType.BLOCK)
            throw new IllegalStateException("Called getBlock on data that contained " + this.type);
        String name = getBlockContentNbt().getString("id");
        // RegistryNamespacedDefaultedByKey silently returns air for removed mod blocks.
        if (!Block.blockRegistry.containsKey(name)) return null;
        Object o = Block.blockRegistry.getObject(name);
        return (Block) o;
    }

    public int getBlockMetadata() {
        if (this.type != CarryType.BLOCK) return 0;
        return getBlockContentNbt().getInteger("meta");
    }

    /** Reconstructs the TileEntity at the given position from stored NBT, or null if none/no id. */
    public TileEntity getBlockEntity(World world, int x, int y, int z) {
        if (this.type != CarryType.BLOCK)
            throw new IllegalStateException("Called getBlockEntity on data that contained " + this.type);
        if (!nbt.hasKey("tile")) return null;
        NBTTagCompound data = (NBTTagCompound) nbt.getCompoundTag("tile")
            .copy();
        data.setInteger("x", x);
        data.setInteger("y", y);
        data.setInteger("z", z);
        try {
            return TileEntity.createAndLoadEntity(data);
        } catch (Exception e) {
            Constants.LOG.warn("Failed to load Carried TileEntity: " + e.getMessage());
            return null;
        }
    }

    public void setEntity(Entity entity) {
        this.type = CarryType.ENTITY;
        NBTTagCompound entityData = new NBTTagCompound();
        entity.writeToNBT(entityData);
        String name = net.minecraft.entity.EntityList.getEntityString(entity);
        if (name == null) throw new IllegalArgumentException("Cannot serialize unregistered entity " + entity);
        entityData.setString("id", name);
        this.nbt.setTag("entity", entityData);
        this.nbt.setString("type", this.type.toString());
    }

    /** Reconstructs the carried entity (NOT spawned in the world). */
    public Entity getEntity(World world) {
        if (this.type != CarryType.ENTITY)
            throw new IllegalStateException("Called getEntity on data that contained " + this.type);
        NBTTagCompound data = (NBTTagCompound) nbt.getCompoundTag("entity")
            .copy();
        Entity entity = net.minecraft.entity.EntityList.createEntityFromNBT(data, world);
        if (entity == null) {
            Constants.LOG
                .warn("Called EntityList#createEntityFromNBT even though no matching entity was found. Data: " + data);
        }
        return entity;
    }

    public void setCarryingPlayer(EntityPlayer player) {
        this.type = CarryType.PLAYER;
        this.nbt.setString(
            "player",
            player.getUniqueID()
                .toString());
        this.nbt.setString("type", this.type.toString());
    }

    public EntityPlayer getCarryingPlayer(World world) {
        if (this.type != CarryType.PLAYER)
            throw new IllegalStateException("Called getCarryingPlayer on data that contained " + this.type);
        if (!nbt.hasKey("player")) return null;
        UUID uuid;
        try {
            uuid = UUID.fromString(nbt.getString("player"));
        } catch (IllegalArgumentException error) {
            return null;
        }
        EntityPlayer player = world.func_152378_a(uuid);
        return player;
    }

    public java.util.Optional<CarryOnScript> getActiveScript() {
        if (activeScript == null) return java.util.Optional.empty();
        return java.util.Optional.of(activeScript);
    }

    public void setActiveScript(CarryOnScript script) {
        this.activeScript = script;
        if (activeScript != null && Constants.COMMON_CONFIG.settings.useScripts) {
            String json = activeScript.getRawJson();
            this.nbt.setString("activeScript", json);
        } else {
            this.nbt.removeTag("activeScript");
        }
    }

    public boolean isCarrying() {
        return this.type != CarryType.INVALID;
    }

    public boolean isCarrying(CarryType type) {
        return this.type == type;
    }

    public boolean isKeyPressed() {
        return this.keyPressed;
    }

    public void setKeyPressed(boolean val) {
        this.keyPressed = val;
        this.nbt.setBoolean("keyPressed", val);
    }

    public void setSelected(int selectedSlot) {
        this.selectedSlot = selectedSlot;
        this.nbt.setInteger("selected", selectedSlot);
    }

    public int getSelected() {
        return this.selectedSlot;
    }

    public void clear() {
        this.type = CarryType.INVALID;
        this.nbt = new NBTTagCompound();
        this.activeScript = null;
        this.nbt.setBoolean("keyPressed", this.keyPressed);
    }

    public CarryOnData clone() {
        return CarryOnData.fromNbt(this.getNbt());
    }

    public int getTick() {
        return this.nbt.getInteger("tick");
    }

    public void setTick(int tick) {
        this.nbt.setInteger("tick", tick);
    }

    private String toRegistryName(Block block) {
        try {
            return (String) Block.blockRegistry.getNameForObject(block);
        } catch (Exception e) {
            return null;
        }
    }

}
