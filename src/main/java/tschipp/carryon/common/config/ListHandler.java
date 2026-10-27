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

package tschipp.carryon.common.config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;

import tschipp.carryon.Constants;
import tschipp.carryon.utils.StringHelper;

/**
 * Blacklist/Whitelist logic. 1.7.10 has no registry tags, so only plain registry names and
 * '*' wildcard patterns are supported; entries starting with '#' are ignored (with a warning).
 * Block names include the namespace ("minecraft:cobblestone"); entity names are EntityList names
 * ("EnderDragon", "EntityHorse", ...).
 */
public class ListHandler {

    private static Set<String> FORBIDDEN_TILES = new HashSet<String>();
    private static Set<String> FORBIDDEN_ENTITIES = new HashSet<String>();
    private static Set<String> ALLOWED_ENTITIES = new HashSet<String>();
    private static Set<String> ALLOWED_TILES = new HashSet<String>();
    private static Set<String> FORBIDDEN_STACKING = new HashSet<String>();
    private static Set<String> ALLOWED_STACKING = new HashSet<String>();

    public static boolean isPermitted(Block block) {
        if (Constants.COMMON_CONFIG.settings.useWhitelistBlocks) return doCheck(block, ALLOWED_TILES);
        return !doCheck(block, FORBIDDEN_TILES);
    }

    public static boolean isPermitted(Entity entity) {
        if (Constants.COMMON_CONFIG.settings.useWhitelistEntities) return doCheck(entity, ALLOWED_ENTITIES);
        return !doCheck(entity, FORBIDDEN_ENTITIES);
    }

    public static boolean isStackingPermitted(Entity entity) {
        if (Constants.COMMON_CONFIG.settings.useWhitelistStacking) return doCheck(entity, ALLOWED_STACKING);
        return !doCheck(entity, FORBIDDEN_STACKING);
    }

    private static boolean doCheck(Block block, Set<String> regular) {
        String name = getBlockName(block);
        if (name == null || name.isEmpty()) return false;
        return regular.contains(name);
    }

    private static boolean doCheck(Entity entity, Set<String> regular) {
        String name = net.minecraft.entity.EntityList.getEntityString(entity);
        if (name == null || name.isEmpty()) return false;
        return regular.contains(name);
    }

    public static String getBlockName(Block block) {
        try {
            return (String) Block.blockRegistry.getNameForObject(block);
        } catch (Exception e) {
            return "";
        }
    }

    public static List<String> getAllBlockNames() {
        List<String> names = new ArrayList<String>();
        for (int id = 0; id < 4096; id++) {
            Block block = Block.getBlockById(id);
            if (block == null) continue;
            String name = getBlockName(block);
            if (name != null && !name.isEmpty() && !names.contains(name)) names.add(name);
        }
        return names;
    }

    private static List<String> getAllEntityNames() {
        List<String> names = new ArrayList<String>();
        for (Object o : net.minecraft.entity.EntityList.classToStringMapping.values()) names.add(String.valueOf(o));
        return names;
    }

    public static void initConfigLists() {
        FORBIDDEN_TILES.clear();
        FORBIDDEN_ENTITIES.clear();
        FORBIDDEN_STACKING.clear();
        ALLOWED_TILES.clear();
        ALLOWED_ENTITIES.clear();
        ALLOWED_STACKING.clear();

        addWithWildcards(toList(Constants.COMMON_CONFIG.blacklist.forbiddenTiles), FORBIDDEN_TILES, true);
        addWithWildcards(toList(Constants.COMMON_CONFIG.blacklist.forbiddenEntities), FORBIDDEN_ENTITIES, false);
        addWithWildcards(toList(Constants.COMMON_CONFIG.blacklist.forbiddenStacking), FORBIDDEN_STACKING, false);
        addWithWildcards(toList(Constants.COMMON_CONFIG.whitelist.allowedStacking), ALLOWED_STACKING, false);
        addWithWildcards(toList(Constants.COMMON_CONFIG.whitelist.allowedEntities), ALLOWED_ENTITIES, false);
        addWithWildcards(toList(Constants.COMMON_CONFIG.whitelist.allowedBlocks), ALLOWED_TILES, true);
    }

    private static List<String> toList(String[] arr) {
        List<String> list = new ArrayList<String>();
        for (String s : arr) list.add(s);
        return list;
    }

    private static void addWithWildcards(List<String> entries, Set<String> toAddTo, boolean blocks) {
        List<String> known = blocks ? getAllBlockNames() : getAllEntityNames();
        for (String curr : entries) {
            if (curr.startsWith("#")) {
                Constants.LOG.warn(
                    "Ignoring tag entry '" + curr + "' in Carry On config: registry tags are not supported on 1.7.10");
                continue;
            }
            if (curr.contains("*")) {
                String[] filter = curr.replace("*", ",")
                    .split(",");
                for (String key : known) {
                    if (containsAll(key, filter)) {
                        toAddTo.add(key);
                    }
                }
            } else {
                toAddTo.add(curr);
            }
        }
    }

    public static boolean containsAll(String str, String... strings) {
        return StringHelper.matchesWildcards(str, strings);
    }

    public static void addForbiddenTiles(String toAdd) {
        FORBIDDEN_TILES.add(toAdd);
    }

    public static void addForbiddenEntities(String toAdd) {
        FORBIDDEN_ENTITIES.add(toAdd);
    }

    public static void addForbiddenStacking(String toAdd) {
        FORBIDDEN_STACKING.add(toAdd);
    }

    public static void addAllowedTiles(String toAdd) {
        ALLOWED_TILES.add(toAdd);
    }

    public static void addAllowedEntities(String toAdd) {
        ALLOWED_ENTITIES.add(toAdd);
    }

    public static void addAllowedStacking(String toAdd) {
        ALLOWED_STACKING.add(toAdd);
    }

}
