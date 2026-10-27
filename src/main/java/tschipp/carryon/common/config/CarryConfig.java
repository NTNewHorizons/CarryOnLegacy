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

/**
 * Plain field holders for Carry On settings on 1.7.10. Field names mirror the modern mod's
 * config schema so configs translate over. Values are populated by {@link ConfigLoader}
 * through Forge's Configuration system (carryon-common.cfg / carryon-client.cfg).
 *
 * Documented 1.7.10 deviations (also see BACKPORT_PLAN.md §8):
 * - scaleDistance / relativePlayerScale do not exist: 1.7.10 has no player scale/attribute system.
 * - pickupDistance always uses the plain config distance.
 */
public class CarryConfig {

    public static class Common {

        public Settings settings = new Settings();

        public static class Settings {

            /** Maximum distance from where Blocks and Entities can be picked up */
            public double maxDistance = 2.5;

            /** Max width of entities that can be picked up in survival mode */
            public double maxEntityWidth = 1.5;

            /** Max height of entities that can be picked up in survival mode */
            public double maxEntityHeight = 2.5;

            /** Slowness multiplier for blocks */
            public double blockSlownessMultiplier = 1;

            /** Slowness multiplier for entities */
            public double entitySlownessMultiplier = 1;

            /** Maximum stack limit for entities */
            public int maxEntityStackLimit = 10;

            /** More complex Tile Entities slow down the player more */
            public boolean heavyTiles = true;

            /** Allow all blocks to be picked up, not just Tile Entities. White/Blacklist will still be respected. */
            public boolean pickupAllBlocks = false;

            /** Whether Blocks and Entities slow the creative player down when carried */
            public boolean slownessInCreative = true;

            /** Whether hostile mobs should be able to picked up in survival mode */
            public boolean pickupHostileMobs = false;

            /** Larger Entities slow down the player more */
            public boolean heavyEntities = true;

            /** Allow babies to be carried even when adult mob is blacklisted (or not whitelisted) */
            public boolean allowBabies = false;

            /** Use Whitelist instead of Blacklist for Blocks */
            public boolean useWhitelistBlocks = false;

            /** Use Whitelist instead of Blacklist for Entities */
            public boolean useWhitelistEntities = false;

            /** Use Whitelist instead of Blacklist for Stacking */
            public boolean useWhitelistStacking = false;

            /** Whether the player can hit blocks and entities while carrying or not */
            public boolean hitWhileCarrying = false;

            /** Whether the player drops the carried object when hit or not */
            public boolean dropCarriedWhenHit = false;

            /** Use custom Pickup Scripts. Having this set to false will not allow you to run scripts */
            public boolean useScripts = false;

            /** Allows entities to be stacked on top of each other */
            public boolean stackableEntities = true;

            /** Whether entities' size matters when stacking or not */
            public boolean entitySizeMattersStacking = true;

            /**
             * Whether Players can be picked up. Creative players can't be picked up in Survival Mode.
             * 1.7.10 note: player-on-player riding is fragile in vanilla; disable if it misbehaves in your pack.
             */
            public boolean pickupPlayers = true;

            /** Whether players in Survival Mode can pick up unbreakable blocks. Creative players always can. */
            public boolean pickupUnbreakableBlocks = false;
        }

        public Whitelist whitelist = new Whitelist();

        public static class Whitelist {

            /** Entities that CAN be picked up (useWhitelistEntities must be true) */
            public String[] allowedEntities = {};

            /** Blocks that CAN be picked up (useWhitelistBlocks must be true) */
            public String[] allowedBlocks = {};

            /** Entities that CAN have other entities stacked on top of them (useWhitelistStacking must be true) */
            public String[] allowedStacking = {};
        }

        public Blacklist blacklist = new Blacklist();

        public static class Blacklist {

            /** Blocks that cannot be picked up */
            public String[] forbiddenTiles = { "minecraft:end_portal", "minecraft:piston_head",
                "minecraft:piston_extension", "minecraft:nether_portal", "minecraft:tallgrass",
                "minecraft:double_plant", "minecraft:waterlily", "minecraft:cake", "minecraft:bed", "minecraft:*_door",
                "minecraft:water", "minecraft:flowing_water", "minecraft:lava", "minecraft:flowing_lava",
                "minecraft:fire", "minecraft:portal" };

            /** Entities that cannot be picked up */
            public String[] forbiddenEntities = { "EnderCrystal", "EnderDragon", "Ghast", "Fireball", "SmallFireball",
                "WitherSkull", "ItemFrame", "Painting", "LeashKnot", "PrimedTnt", "Arrow" };

            /** Entities that cannot have other entities stacked on top of them */
            public String[] forbiddenStacking = { "EntityHorse", "EnderDragon" };
        }

        public CustomPickupConditions customPickupConditions = new CustomPickupConditions();

        public static class CustomPickupConditions {

            /** Custom Pickup Conditions for Blocks (format: block[id,meta] or block(id) with (conditionName)) */
            public String[] customPickupConditionsBlocks = {};

            /** Custom Pickup Conditions for Entities */
            public String[] customPickupConditionsEntities = {};
        }
    }

    public static class Client {

        /** If the front of the Tile Entities should face the player or should face outward */
        public boolean facePlayer = false;

        /** If Entities should be rotated sideways when carried */
        public boolean rotateEntitiesSideways = false;

        /**
         * Arms should render on sides when carrying. Set to false if you experience issues with mods that replace the
         * player model
         */
        public boolean renderArms = true;

        /**
         * Model Overrides based on NBT or Meta. Advanced users only! Format:
         * source[blockmeta]{nbt}->(item|block)target[gamemeta]
         */
        public String[] modelOverrides = {};
    }
}
