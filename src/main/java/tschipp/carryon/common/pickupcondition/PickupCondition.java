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

package tschipp.carryon.common.pickupcondition;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;

import tschipp.carryon.common.config.ListHandler;
import tschipp.carryon.platform.GamestagesCompat;
import tschipp.carryon.utils.StringHelper;

/**
 * 1.7.10 port of the modern PickupCondition. Grammar has no BlockState properties here,
 * so block conditions are "blockpattern[meta](condition)". Meta tests are optional.
 * Entity conditions are "entitylistname(condition)". Wildcards are supported.
 */
public class PickupCondition {

    private final String str;
    private final String cond;
    private final String match;
    private boolean wildcards;

    private PickupCondition(String str, String cond, String match) {
        this.str = str;
        this.cond = cond;
        this.match = match;
    }

    /**
     * Parses a condition string like "minecraft:chest[3](mysage)" or "EntityHorse(stage)".
     * Returns null when the format is invalid (missing brackets or parentheses).
     */
    public static PickupCondition of(String str) {
        if (!str.contains("(") || !str.endsWith(")")) return null;

        String cond = str.substring(str.indexOf("(") + 1, str.length() - 1);
        String match = str.substring(0, str.indexOf("("));

        PickupCondition condition = new PickupCondition(str, cond, match);
        if (match.contains("*")) condition.wildcards = true;

        return condition;
    }

    public boolean matches(Block block, int meta) {
        if (block == null) return false;

        String name = ListHandler.getBlockName(block);
        if (name == null || name.isEmpty()) return false;

        if (wildcards) {
            String namePart = match.contains("[") ? match.substring(0, match.indexOf("[")) : match;
            String[] split = namePart.replace("*", ",")
                .split(",");

            boolean nameOk = StringHelper.matchesWildcards(name, split)
                || StringHelper.matchesWildcards(stripMinecraft(name), split);

            if (!nameOk) return false;

            if (match.contains("[") && !namePart.contains("[")) return matchMeta(meta);
            return true;
        } else {
            if (!matchesName(name)) return false;

            if (match.contains("[")) return matchMeta(meta);
            return true;
        }
    }

    public boolean matches(Entity entity) {
        if (entity == null) return false;

        String entityName = net.minecraft.entity.EntityList.getEntityString(entity);
        if (entityName == null) entityName = "";

        if (wildcards) {
            String[] split = match.replace("*", ",")
                .split(",");
            return StringHelper.matchesWildcards(entityName, split);
        }

        return entityName.equals(match);
    }

    private boolean matchesName(String registryName) {
        String name = match.contains("[") ? match.substring(0, match.indexOf("[")) : match;
        if (registryName.equals(name)) return true;
        // Allow plain names for vanilla blocks ("chest" instead of "minecraft:chest")
        return !name.contains(":") && registryName.equals("minecraft:" + name);
    }

    private boolean matchMeta(int meta) {
        try {
            int wanted = Integer.parseInt(match.substring(match.indexOf("[") + 1, match.indexOf("]")));
            return wanted == meta;
        } catch (Exception e) {
            return false;
        }
    }

    private static String stripMinecraft(String name) {
        return name.startsWith("minecraft:") ? name.substring("minecraft:".length()) : name;
    }

    public String getCondition() {
        return cond;
    }

    public boolean isFulfilled(EntityPlayerMP player) {
        return GamestagesCompat.hasStage(player, cond);
    }

    public String getRaw() {
        return str;
    }

}
