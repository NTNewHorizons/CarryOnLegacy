/*
 * Carry On — GNU Lesser General Public License v3 or later.
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * See LICENSE for terms and attribution to the original Carry On authors.
 */
package tschipp.carryon.client.modeloverride;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTTagCompound;

import tschipp.carryon.common.config.ListHandler;
import tschipp.carryon.common.scripting.Matchables.NBTCondition;

/** Legacy model grammar: namespace:block;meta{SNBT}->(item|block)namespace:target;meta. Brackets also accepted. */
public final class ModelOverride {

    private final String source;
    private final int metadata;
    private final NBTCondition condition;
    public final Block block;
    public final ItemStack item;
    public final int targetMetadata;

    private ModelOverride(String source, int metadata, NBTCondition condition, Block block, ItemStack item,
        int targetMetadata) {
        this.source = source;
        this.metadata = metadata;
        this.condition = condition;
        this.block = block;
        this.item = item;
        this.targetMetadata = targetMetadata;
    }

    public static ModelOverride of(String raw) throws Exception {
        String[] sides = raw.split("->", -1);
        if (sides.length != 2) throw new IllegalArgumentException("Expected one ->");
        String left = sides[0].trim();
        NBTCondition condition = null;
        int nbt = left.indexOf('{');
        if (nbt >= 0) {
            condition = new NBTCondition((NBTTagCompound) JsonToNBT.func_150315_a(left.substring(nbt)));
            left = left.substring(0, nbt)
                .trim();
        }
        int metadata = metadata(left, -1);
        String source = name(left);
        if (!source.contains(":")) source = "minecraft:" + source;
        String right = sides[1].trim();
        boolean isBlock = right.startsWith("(block)");
        if (right.startsWith("(item)")) right = right.substring(6);
        else if (isBlock) right = right.substring(7);
        right = right.trim();
        int targetMeta = metadata(right, 0);
        String target = name(right);
        if (!target.contains(":")) target = "minecraft:" + target;
        if (isBlock) {
            if (!Block.blockRegistry.containsKey(target)) throw new IllegalArgumentException("Unknown block " + target);
            return new ModelOverride(
                source,
                metadata,
                condition,
                (Block) Block.blockRegistry.getObject(target),
                null,
                targetMeta);
        }
        Item item = (Item) Item.itemRegistry.getObject(target);
        if (item == null) throw new IllegalArgumentException("Unknown item " + target);
        return new ModelOverride(source, metadata, condition, null, new ItemStack(item, 1, targetMeta), targetMeta);
    }

    public boolean matches(Block block, int metadata, NBTTagCompound tag) {
        return source.equals(ListHandler.getBlockName(block)) && (this.metadata < 0 || this.metadata == metadata)
            && (condition == null || condition.matches(tag));
    }

    private static String name(String value) {
        int index = value.indexOf('[');
        if (index < 0) index = value.indexOf(';');
        return (index < 0 ? value : value.substring(0, index)).trim();
    }

    private static int metadata(String value, int fallback) {
        int index = value.indexOf('[');
        boolean bracket = index >= 0;
        if (!bracket) index = value.indexOf(';');
        if (index < 0) return fallback;
        if (bracket && !value.endsWith("]")) throw new IllegalArgumentException("Unclosed metadata bracket");
        int result = Integer.parseInt(
            value.substring(index + 1, bracket ? value.length() - 1 : value.length())
                .trim());
        if (result < 0) throw new IllegalArgumentException("Negative metadata");
        return result;
    }
}
