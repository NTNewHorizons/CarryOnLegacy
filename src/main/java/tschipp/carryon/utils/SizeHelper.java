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

package tschipp.carryon.utils;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

/**
 * Consistent way to get entity size.
 *
 * 1.7.10 note: there is no player-scale attribute system (Attributes.SCALE does not exist),
 * so the player scale factor is always 1. Configs that depended on scaling are documented no-ops.
 */
public class SizeHelper {

    public static float getPlayerScaleFactor(EntityPlayer player) {
        return 1f;
    }

    public static float getEntityWidth(Entity entity) {
        return entity.width;
    }

    public static float getRelativeEntityWidth(EntityPlayer player, Entity entity) {
        return getEntityWidth(entity) * getPlayerScaleFactor(player);
    }

    public static float getEntityHeight(Entity entity) {
        return entity.height;
    }

    public static float getRelativeEntityHeight(EntityPlayer player, Entity entity) {
        return getEntityHeight(entity) * getPlayerScaleFactor(player);
    }

    public static float getRelativeEntityArea(EntityPlayer player, Entity entity) {
        float playerScaleFactor = getPlayerScaleFactor(player);
        return getEntityHeight(entity) * getEntityWidth(entity) * playerScaleFactor * playerScaleFactor;
    }
}
