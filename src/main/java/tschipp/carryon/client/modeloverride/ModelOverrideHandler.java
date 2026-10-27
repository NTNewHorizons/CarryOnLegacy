/*
 * Carry On — GNU Lesser General Public License v3 or later.
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * See LICENSE for terms and attribution to the original Carry On authors.
 */
package tschipp.carryon.client.modeloverride;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;

import tschipp.carryon.Constants;

public final class ModelOverrideHandler {

    private static final List<ModelOverride> OVERRIDES = new ArrayList<ModelOverride>();

    public static void initialize() {
        OVERRIDES.clear();
        for (String raw : Constants.CLIENT_CONFIG.modelOverrides) {
            try {
                OVERRIDES.add(ModelOverride.of(raw));
            } catch (Exception error) {
                Constants.LOG.warn("Invalid Carry On model override: " + raw, error);
            }
        }
    }

    public static ModelOverride getModelOverride(Block block, int metadata, NBTTagCompound tag) {
        for (ModelOverride override : OVERRIDES) if (override.matches(block, metadata, tag)) return override;
        return null;
    }
}
