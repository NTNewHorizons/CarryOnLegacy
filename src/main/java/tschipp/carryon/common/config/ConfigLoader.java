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

import java.io.File;

import net.minecraftforge.common.config.Configuration;

import tschipp.carryon.Constants;
import tschipp.carryon.common.pickupcondition.PickupConditionHandler;

/**
 * Loads carryon-common.cfg and carryon-client.cfg through Forge's Configuration system.
 * Population mirrors the modern annotation-based config loader.
 */
public class ConfigLoader {

    public static Configuration commonConfig;
    public static Configuration clientConfig;

    public static void loadCommon(File file) {
        commonConfig = new Configuration(file);

        CarryConfig.Common cfg = Constants.COMMON_CONFIG;

        cfg.settings.maxDistance = commonConfig
            .get("settings", "maxDistance", 2.5, "Maximum distance from where Blocks and Entities can be picked up")
            .getDouble(2.5);

        cfg.settings.maxEntityWidth = commonConfig
            .get("settings", "maxEntityWidth", 1.5, "Max width of entities that can be picked up in survival mode")
            .getDouble(1.5);

        cfg.settings.maxEntityHeight = commonConfig
            .get("settings", "maxEntityHeight", 2.5, "Max height of entities that can be picked up in survival mode")
            .getDouble(2.5);

        cfg.settings.blockSlownessMultiplier = commonConfig
            .get("settings", "blockSlownessMultiplier", 1, "Slowness multiplier for blocks")
            .getDouble(1);

        cfg.settings.entitySlownessMultiplier = commonConfig
            .get("settings", "entitySlownessMultiplier", 1, "Slowness multiplier for entities")
            .getDouble(1);

        cfg.settings.maxEntityStackLimit = commonConfig
            .get("settings", "maxEntityStackLimit", 10, "Maximum stack limit for entities")
            .getInt();

        cfg.settings.heavyTiles = commonConfig
            .get("settings", "heavyTiles", true, "More complex Tile Entities slow down the player more")
            .getBoolean(true);

        cfg.settings.pickupAllBlocks = commonConfig
            .get(
                "settings",
                "pickupAllBlocks",
                false,
                "Allow all blocks to be picked up, not just Tile Entities. White/Blacklist will still be respected.")
            .getBoolean(false);

        cfg.settings.slownessInCreative = commonConfig
            .get(
                "settings",
                "slownessInCreative",
                true,
                "Whether Blocks and Entities slow the creative player down when carried")
            .getBoolean(true);

        cfg.settings.pickupHostileMobs = commonConfig
            .get(
                "settings",
                "pickupHostileMobs",
                false,
                "Whether hostile mobs should be able to be picked up in survival mode")
            .getBoolean(false);

        cfg.settings.heavyEntities = commonConfig
            .get("settings", "heavyEntities", true, "Larger Entities slow down the player more")
            .getBoolean(true);

        cfg.settings.allowBabies = commonConfig
            .get(
                "settings",
                "allowBabies",
                false,
                "Allow babies to be carried even when adult mob is blacklisted (or not whitelisted)")
            .getBoolean(false);

        cfg.settings.useWhitelistBlocks = commonConfig
            .get("settings", "useWhitelistBlocks", false, "Use Whitelist instead of Blacklist for Blocks")
            .getBoolean(false);

        cfg.settings.useWhitelistEntities = commonConfig
            .get("settings", "useWhitelistEntities", false, "Use Whitelist instead of Blacklist for Entities")
            .getBoolean(false);

        cfg.settings.useWhitelistStacking = commonConfig
            .get("settings", "useWhitelistStacking", false, "Use Whitelist instead of Blacklist for Stacking")
            .getBoolean(false);

        cfg.settings.hitWhileCarrying = commonConfig
            .get(
                "settings",
                "hitWhileCarrying",
                false,
                "Whether the player can hit blocks and entities while carrying or not")
            .getBoolean(false);

        cfg.settings.dropCarriedWhenHit = commonConfig
            .get("settings", "dropCarriedWhenHit", false, "Whether the player drops the carried object when hit or not")
            .getBoolean(false);

        cfg.settings.useScripts = commonConfig.get(
            "settings",
            "useScripts",
            false,
            "Use custom Pickup Scripts (config/carryon/scripts). Having this set to false will not allow you to run scripts, and will improve performance")
            .getBoolean(false);

        cfg.settings.stackableEntities = commonConfig
            .get("settings", "stackableEntities", true, "Allows entities to be stacked on top of each other")
            .getBoolean(true);

        cfg.settings.entitySizeMattersStacking = commonConfig.get(
            "settings",
            "entitySizeMattersStacking",
            true,
            "Whether entities' size matters when stacking or not. This means that larger entities cannot be stacked on smaller ones")
            .getBoolean(true);

        cfg.settings.pickupPlayers = commonConfig.get(
            "settings",
            "pickupPlayers",
            true,
            "Whether Players can be picked up. Creative players can't be picked up in Survival Mode. Note: player-on-player riding is fragile in vanilla 1.7.10")
            .getBoolean(true);

        cfg.settings.pickupUnbreakableBlocks = commonConfig
            .get(
                "settings",
                "pickupUnbreakableBlocks",
                false,
                "Whether players in Survival Mode can pick up unbreakable blocks. Creative players always can.")
            .getBoolean(false);

        cfg.whitelist.allowedEntities = commonConfig.get(
            "whitelist",
            "allowedEntities",
            new String[] {},
            "Entities that CAN be picked up (useWhitelistEntities must be true). Format: name or namespace:name patterns with * wildcards")
            .getStringList();

        cfg.whitelist.allowedBlocks = commonConfig
            .get(
                "whitelist",
                "allowedBlocks",
                new String[] {},
                "Blocks that CAN be picked up (useWhitelistBlocks must be true)")
            .getStringList();

        cfg.whitelist.allowedStacking = commonConfig
            .get(
                "whitelist",
                "allowedStacking",
                new String[] {},
                "Entities that CAN have other entities stacked on top of them (useWhitelistStacking must be true)")
            .getStringList();

        cfg.blacklist.forbiddenTiles = commonConfig.get(
            "blacklist",
            "forbiddenTiles",
            defaultForbiddenTiles(),
            "Blocks that cannot be picked up. # prefixed tag references are not supported on 1.7.10 and will be ignored")
            .getStringList();

        cfg.blacklist.forbiddenEntities = commonConfig
            .get(
                "blacklist",
                "forbiddenEntities",
                defaultForbiddenEntities(),
                "Entities that cannot be picked up (EntityList names, e.g. 'EnderDragon')")
            .getStringList();

        cfg.blacklist.forbiddenStacking = commonConfig
            .get(
                "blacklist",
                "forbiddenStacking",
                new String[] { "EntityHorse", "EnderDragon" },
                "Entities that cannot have other entities stacked on top of them")
            .getStringList();

        cfg.customPickupConditions.customPickupConditionsBlocks = commonConfig
            .get(
                "customPickupConditions",
                "customPickupConditionsBlocks",
                new String[] {},
                "Custom Pickup Conditions for Blocks. Format: blockpattern(conditionName)")
            .getStringList();

        cfg.customPickupConditions.customPickupConditionsEntities = commonConfig
            .get(
                "customPickupConditions",
                "customPickupConditionsEntities",
                new String[] {},
                "Custom Pickup Conditions for Entities")
            .getStringList();

        ListHandler.initConfigLists();
        PickupConditionHandler.initPickupConditions();

        if (commonConfig.hasChanged()) commonConfig.save();
    }

    public static void loadClient(File file) {
        clientConfig = new Configuration(file);

        CarryConfig.Client cfg = Constants.CLIENT_CONFIG;

        cfg.facePlayer = clientConfig
            .get(
                "settings",
                "facePlayer",
                false,
                "If the front of the Tile Entities should face the player or should face outward")
            .getBoolean(false);

        cfg.rotateEntitiesSideways = clientConfig
            .get("settings", "rotateEntitiesSideways", false, "If Entities should be rotated sideways when carried")
            .getBoolean(false);

        cfg.renderArms = clientConfig.get(
            "settings",
            "renderArms",
            true,
            "Arms should render on sides when carrying. Set to false if you experience issues with mods that replace the player model")
            .getBoolean(true);

        cfg.modelOverrides = clientConfig.get(
            "settings",
            "modelOverrides",
            new String[] {},
            "Model Overrides based on NBT or Meta. Advanced users only! Format: source[meta]{nbt}->(item|block)target[meta]")
            .getStringList();

        if (clientConfig.hasChanged()) clientConfig.save();
    }

    private static String[] defaultForbiddenTiles() {
        return new String[] { "minecraft:end_portal", "minecraft:piston_head", "minecraft:piston_extension",
            "minecraft:nether_portal", "minecraft:tallgrass", "minecraft:double_plant", "minecraft:waterlily",
            "minecraft:cake", "minecraft:bed", "minecraft:*_door", "minecraft:water", "minecraft:flowing_water",
            "minecraft:lava", "minecraft:flowing_lava", "minecraft:fire", "minecraft:portal" };
    }

    private static String[] defaultForbiddenEntities() {
        return new String[] { "EnderCrystal", "EnderDragon", "Ghast", "Fireball", "SmallFireball", "WitherSkull",
            "ItemFrame", "Painting", "LeashKnot", "PrimedTnt", "Arrow" };
    }

}
