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

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;

/**
 * Reorients direction-encode metadata for a fixed set of vanilla directional blocks when a
 * carried block is placed. 1.7.10 uses block-specific metadata for facing which cannot be
 * generically updated like modern BlockState properties, so this covers the canonical
 * vanilla cases (chest/furnace-style metadata 2..5, pumpkins facing).
 */
public class MetadataReorientation {

    private static final String[] FACING_2_5 = new String[] { "minecraft:chest", "minecraft:trapped_chest",
        "minecraft:furnace", "minecraft:lit_furnace", "minecraft:dispenser", "minecraft:dropper",
        "minecraft:ender_chest" };

    public static int getReorientedMetadata(Block block, int meta, EntityPlayer player) {
        String name = tschipp.carryon.common.config.ListHandler.getBlockName(block);

        for (String facingName : FACING_2_5) {
            if (facingName.equals(name) && meta >= 2 && meta <= 5) {
                int l = MathHelper.floor_double((double) (player.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
                if (l == 0) return 2;
                if (l == 1) return 5;
                if (l == 2) return 3;
                return 4;
            }
        }

        if ("minecraft:pumpkin".equals(name) || "minecraft:lit_pumpkin".equals(name)) {
            return MathHelper.floor_double(player.rotationYaw * 4.0F / 360.0F + 0.5D) & 3;
        }

        return meta;
    }

}
