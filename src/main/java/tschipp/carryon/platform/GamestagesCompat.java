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

package tschipp.carryon.platform;

import net.minecraft.entity.player.EntityPlayerMP;

/**
 * Placeholder hook for GameStages-like providers. No GameStages ecosystem exists on 1.7.10,
 * so every condition is considered fulfilled. Other mods may replace this class through an
 * interface provider later.
 */
public class GamestagesCompat {

    public static boolean hasStage(EntityPlayerMP player, String stage) {
        return true;
    }

}
