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

package tschipp.carryon.common.scripting;

import java.util.Map;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import tschipp.carryon.common.scripting.CarryOnScript.ScriptConditions;
import tschipp.carryon.common.scripting.CarryOnScript.ScriptEffects;
import tschipp.carryon.common.scripting.CarryOnScript.ScriptObject;
import tschipp.carryon.common.scripting.CarryOnScript.ScriptObjectBlock;
import tschipp.carryon.common.scripting.CarryOnScript.ScriptObjectEntity;
import tschipp.carryon.common.scripting.CarryOnScript.ScriptRender;
import tschipp.carryon.common.scripting.Matchables.AchievementCondition;
import tschipp.carryon.common.scripting.Matchables.EffectsCondition;
import tschipp.carryon.common.scripting.Matchables.GamestageCondition;
import tschipp.carryon.common.scripting.Matchables.NBTCondition;
import tschipp.carryon.common.scripting.Matchables.NumberBoundCondition;
import tschipp.carryon.common.scripting.Matchables.OptionalVec3;
import tschipp.carryon.common.scripting.Matchables.PositionCondition;
import tschipp.carryon.common.scripting.Matchables.ScoreboardCondition;

/**
 * Parses Carry On scripts from JSON. The format is identical to the modern data-pack
 * scripts; on 1.7.10 they are loaded from config/carryon/scripts instead.
 */
public class ScriptParser {

    private static final JsonParser PARSER = new JsonParser();

    /**
     * Parses the given JSON string into a CarryOnScript. Throwing on invalid input lets
     * loaders report which file was malformed.
     */
    public static CarryOnScript fromJson(String json) {
        JsonElement root = PARSER.parse(json);
        if (root == null || !root.isJsonObject())
            throw new IllegalArgumentException("Carry On script must be a JSON object");
        return fromJsonObject(root.getAsJsonObject(), json);
    }

    public static String toJson(CarryOnScript script) {
        return script.getRawJson();
    }

    public static CarryOnScript fromJsonObject(JsonObject obj, String rawJson) {
        CarryOnScript script = new CarryOnScript(rawJson);

        JsonElement priority = get(obj, "priority");
        script.priority = priority != null && priority.isJsonPrimitive() ? priority.getAsLong() : 0L;

        ScriptObject object = null;
        JsonObject ob = getObject(obj, "object");
        if (ob != null) {
            ScriptObjectBlock block = parseBlockObject(getObject(ob, "block"));
            ScriptObjectEntity entity = parseEntityObject(getObject(ob, "entity"));
            if (block != null || entity != null) {
                object = new ScriptObject();
                object.block = block;
                object.entity = entity;
            }
        }
        script.scriptObject = object;

        JsonObject conditions = getObject(obj, "conditions");
        script.scriptConditions = conditions != null ? parseConditions(conditions) : null;

        JsonObject render = getObject(obj, "render");
        script.scriptRender = render != null ? parseRender(render) : null;

        JsonObject effects = getObject(obj, "effects");
        script.scriptEffects = effects != null ? parseEffects(effects) : null;

        JsonElement override = get(obj, "override_checks");
        script.overrideChecks = override != null && override.isJsonPrimitive() && override.getAsBoolean();

        return script;
    }

    private static ScriptObjectBlock parseBlockObject(JsonObject obj) {
        if (obj == null) return null;
        ScriptObjectBlock block = new ScriptObjectBlock();
        JsonElement name = get(obj, "name");
        block.name = name != null && name.isJsonPrimitive()
            && !name.getAsString()
                .isEmpty() ? name.getAsString() : null;
        block.hardness = parseNumberBound(get(obj, "hardness"));
        block.resistance = parseNumberBound(get(obj, "resistance"));
        JsonObject nbt = getObject(obj, "nbt");
        block.nbt = nbt != null ? new NBTCondition(jsonToNbt(nbt)) : null;
        return block;
    }

    private static ScriptObjectEntity parseEntityObject(JsonObject obj) {
        if (obj == null) return null;
        ScriptObjectEntity entity = new ScriptObjectEntity();
        JsonElement name = get(obj, "name");
        entity.name = name != null && name.isJsonPrimitive()
            && !name.getAsString()
                .isEmpty() ? name.getAsString() : null;
        entity.health = parseNumberBound(get(obj, "health"));
        entity.height = parseNumberBound(get(obj, "height"));
        entity.width = parseNumberBound(get(obj, "width"));
        JsonObject nbt = getObject(obj, "nbt");
        entity.nbt = nbt != null ? new NBTCondition(jsonToNbt(nbt)) : null;
        return entity;
    }

    private static ScriptConditions parseConditions(JsonObject obj) {
        ScriptConditions cond = new ScriptConditions();
        cond.conditionGamestage = new GamestageCondition(getOrDefaultString(obj, "gamestage", ""));
        cond.conditionAchievement = new AchievementCondition(getOrDefaultString(obj, "advancement", ""));
        cond.conditionXp = parseNumberBound(get(obj, "xp"));
        cond.conditionGamemode = parseNumberBound(get(obj, "gamemode"));
        cond.conditionScoreboard = new ScoreboardCondition(getOrDefaultString(obj, "scoreboard", ""));
        cond.conditionPosition = new PositionCondition(getOrDefaultString(obj, "position", ""));
        cond.conditionEffects = new EffectsCondition(getOrDefaultString(obj, "effects", ""));
        return cond;
    }

    private static ScriptRender parseRender(JsonObject obj) {
        ScriptRender r = new ScriptRender();
        r.nameBlock = getOrNullString(obj, "name_block");
        r.nameEntity = getOrNullString(obj, "name_entity");

        JsonObject nbt = getObject(obj, "nbt");
        r.renderNbt = nbt != null ? jsonToNbt(nbt) : null;

        r.renderTranslation = parseVec3(obj, "translation");
        r.renderRotation = parseVec3(obj, "rotation");
        r.renderScale = parseVec3(obj, "scale");
        r.renderRotationLeftArm = parseVec3(obj, "rotation_left_arm");
        r.renderRotationRightArm = parseVec3(obj, "rotation_right_arm");

        r.renderLeftArm = getOrDefaultBoolean(obj, "render_left_arm", true);
        r.renderRightArm = getOrDefaultBoolean(obj, "render_right_arm", true);
        return r;
    }

    private static ScriptEffects parseEffects(JsonObject obj) {
        ScriptEffects fx = new ScriptEffects();
        fx.commandPickup = getOrDefaultString(obj, "commandPickup", "");
        fx.commandLoop = getOrDefaultString(obj, "commandLoop", "");
        fx.commandPlace = getOrDefaultString(obj, "commandPlace", "");
        return fx;
    }

    /**
     * Parses a NumberBoundCondition from a JSON element (string expression or raw number).
     */
    private static NumberBoundCondition parseNumberBound(JsonElement e) {
        if (e == null || e.isJsonNull()) return NumberBoundCondition.NONE;
        if (e.isJsonPrimitive()) return new NumberBoundCondition(e.getAsString());
        return NumberBoundCondition.NONE;
    }

    private static OptionalVec3 parseVec3(JsonObject obj, String key) {
        JsonElement e = get(obj, key);
        if (e != null && e.isJsonPrimitive()) return new OptionalVec3(e.getAsString());
        return new OptionalVec3("");
    }

    private static String getOrDefaultString(JsonObject obj, String key, String def) {
        JsonElement e = get(obj, key);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : def;
    }

    private static String getOrNullString(JsonObject obj, String key) {
        JsonElement e = get(obj, key);
        if (e == null || !e.isJsonPrimitive()
            || e.getAsString()
                .isEmpty())
            return null;
        return e.getAsString();
    }

    private static boolean getOrDefaultBoolean(JsonObject obj, String key, boolean def) {
        JsonElement e = get(obj, key);
        return e != null && e.isJsonPrimitive() ? e.getAsBoolean() : def;
    }

    private static JsonObject getObject(JsonObject obj, String key) {
        JsonElement e = get(obj, key);
        if (e == null || !e.isJsonObject()) return null;
        return e.getAsJsonObject();
    }

    private static JsonElement get(JsonObject obj, String key) {
        if (!obj.has(key)) return null;
        return obj.get(key);
    }

    public static NBTTagCompound jsonToNbt(JsonObject obj) {
        NBTTagCompound tag = new NBTTagCompound();
        nbtFromJson(obj, tag);
        return tag;
    }

    private static void nbtFromJson(JsonObject obj, NBTTagCompound tag) {
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            String key = entry.getKey();
            JsonElement val = entry.getValue();

            if (val.isJsonPrimitive()) {
                JsonPrimitive prim = val.getAsJsonPrimitive();
                if (prim.isBoolean()) tag.setBoolean(key, prim.getAsBoolean());
                else if (prim.isNumber()) {
                    String number = prim.getAsString();
                    if (number.matches("-?[0-9]+")) {
                        long integer = prim.getAsLong();
                        if (integer >= Integer.MIN_VALUE && integer <= Integer.MAX_VALUE)
                            tag.setInteger(key, (int) integer);
                        else tag.setLong(key, integer);
                    } else tag.setDouble(key, prim.getAsDouble());
                } else tag.setString(key, prim.getAsString());
            } else if (val.isJsonObject()) {
                NBTTagCompound sub = new NBTTagCompound();
                nbtFromJson(val.getAsJsonObject(), sub);
                tag.setTag(key, sub);
            } else if (val.isJsonArray()) {
                NBTTagList list = new NBTTagList();
                for (JsonElement element : val.getAsJsonArray()) {
                    JsonObject wrapper = new JsonObject();
                    wrapper.add("value", element);
                    NBTBase entryTag = jsonToNbt(wrapper).getTag("value");
                    if (entryTag == null) throw new IllegalArgumentException("Null is not an NBT list element: " + key);
                    if (list.tagCount() > 0 && list.func_150303_d() != entryTag.getId())
                        throw new IllegalArgumentException("Mixed NBT list element types: " + key);
                    list.appendTag(entryTag);
                }
                tag.setTag(key, list);
            }
        }
    }

}
