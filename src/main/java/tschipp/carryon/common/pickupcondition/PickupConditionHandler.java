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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;

import tschipp.carryon.Constants;

public class PickupConditionHandler {

    private static final List<PickupCondition> BLOCK_CONDITIONS = new ArrayList<PickupCondition>();
    private static final List<PickupCondition> ENTITY_CONDITIONS = new ArrayList<PickupCondition>();

    public static void initPickupConditions() {
        BLOCK_CONDITIONS.clear();
        ENTITY_CONDITIONS.clear();

        for (String cond : Constants.COMMON_CONFIG.customPickupConditions.customPickupConditionsBlocks) {
            PickupCondition pickupCondition = PickupCondition.of(cond);
            if (pickupCondition != null) BLOCK_CONDITIONS.add(pickupCondition);
            else Constants.LOG.warn("Error while parsing Pickup Conditions: " + cond);
        }

        for (String cond : Constants.COMMON_CONFIG.customPickupConditions.customPickupConditionsEntities) {
            PickupCondition pickupCondition = PickupCondition.of(cond);
            if (pickupCondition != null) ENTITY_CONDITIONS.add(pickupCondition);
            else Constants.LOG.warn("Error while parsing Pickup Conditions: " + cond);
        }
    }

    public static Optional<PickupCondition> getPickupCondition(Block block, int meta) {
        for (PickupCondition cond : BLOCK_CONDITIONS) {
            if (cond.matches(block, meta)) return Optional.of(cond);
        }
        return Optional.empty();
    }

    public static Optional<PickupCondition> getPickupCondition(Entity entity) {
        for (PickupCondition cond : ENTITY_CONDITIONS) {
            if (cond.matches(entity)) return Optional.of(cond);
        }
        return Optional.empty();
    }

}
