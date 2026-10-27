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

package tschipp.carryon.common.scripting;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import tschipp.carryon.Constants;
import tschipp.carryon.utils.SizeHelper;

public class ScriptManager {

    public static final List<CarryOnScript> SCRIPTS = new ArrayList<CarryOnScript>();

    public static Optional<CarryOnScript> inspectBlock(Block block, int meta, World world, int x, int y, int z,
        NBTTagCompound tag) {
        if (!Constants.COMMON_CONFIG.settings.useScripts) return Optional.empty();

        float hardness = block.getBlockHardness(world, x, y, z);
        float resistance = block.getExplosionResistance(null, world, x, y, z, x, y, z);

        for (CarryOnScript script : SCRIPTS) {
            if (script.isBlock() && matchesAll(script, block, hardness, resistance, tag)) return Optional.of(script);
        }

        return Optional.empty();
    }

    public static Optional<CarryOnScript> inspectEntity(Entity entity) {
        if (!Constants.COMMON_CONFIG.settings.useScripts) return Optional.empty();

        float height = SizeHelper.getEntityHeight(entity);
        float width = SizeHelper.getEntityWidth(entity);
        float health = entity instanceof EntityLivingBase ? ((EntityLivingBase) entity).getHealth() : 0.0f;
        NBTTagCompound tag = new NBTTagCompound();
        entity.writeToNBT(tag);

        for (CarryOnScript script : SCRIPTS) {
            if (script.isEntity() && matchesAll(script, entity, height, width, health, tag)) return Optional.of(script);
        }

        return Optional.empty();
    }

    private static boolean matchesAll(CarryOnScript script, Entity entity, float height, float width, float health,
        NBTTagCompound tag) {
        CarryOnScript.ScriptObjectEntity scEntity = script.scriptObject.entity;

        boolean matchname = true;
        if (scEntity.name != null)
            matchname = scEntity.name.equals(net.minecraft.entity.EntityList.getEntityString(entity));
        boolean matchheight = cond(scEntity.height).matches(Float.valueOf(height));
        boolean matchwidth = cond(scEntity.width).matches(Float.valueOf(width));
        boolean matchhealth = cond(scEntity.health).matches(Float.valueOf(health));
        boolean matchnbt = cond(scEntity.nbt).matches(tag);

        return matchname && matchheight && matchwidth && matchhealth && matchnbt;
    }

    private static boolean matchesAll(CarryOnScript script, Block block, float hardness, float resistance,
        NBTTagCompound nbt) {
        CarryOnScript.ScriptObjectBlock scBlock = script.scriptObject.block;

        boolean matchblock = true;
        if (scBlock.name != null) {
            matchblock = scBlock.name.equals(tschipp.carryon.common.config.ListHandler.getBlockName(block));
        }
        boolean matchnbt = cond(scBlock.nbt).matches(nbt);
        boolean matchhardness = cond(scBlock.hardness).matches(Float.valueOf(hardness));
        boolean matchresistance = cond(scBlock.resistance).matches(Float.valueOf(resistance));

        return matchnbt && matchblock && matchhardness && matchresistance;
    }

    private static Matchables.NumberBoundCondition cond(Matchables.NumberBoundCondition given) {
        return given == null ? Matchables.NumberBoundCondition.NONE : given;
    }

    private static Matchables.NBTCondition cond(Matchables.NBTCondition given) {
        return given == null ? Matchables.NBTCondition.NONE : given;
    }

}
