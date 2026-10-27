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

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;

import tschipp.carryon.common.scripting.Matchables.AchievementCondition;
import tschipp.carryon.common.scripting.Matchables.EffectsCondition;
import tschipp.carryon.common.scripting.Matchables.GamestageCondition;
import tschipp.carryon.common.scripting.Matchables.NBTCondition;
import tschipp.carryon.common.scripting.Matchables.NumberBoundCondition;
import tschipp.carryon.common.scripting.Matchables.OptionalVec3;
import tschipp.carryon.common.scripting.Matchables.PositionCondition;
import tschipp.carryon.common.scripting.Matchables.ScoreboardCondition;

/**
 * 1.7.10 port of the modern CarryOnScript. Parses the same JSON format (loaded from
 * config/carryon/scripts) with manual extraction instead of Mojang codecs.
 */
public class CarryOnScript {

    public long priority;
    public ScriptObject scriptObject;
    public ScriptConditions scriptConditions;
    public ScriptRender scriptRender;
    public ScriptEffects scriptEffects;
    public boolean overrideChecks;

    private final String rawJson;

    public CarryOnScript(String rawJson) {
        this.rawJson = rawJson == null ? "" : rawJson;
    }

    public boolean isValid() {
        return (isBlock() ^ isEntity())
            && (scriptConditions != null || scriptRender != null || scriptEffects != null || overrideChecks);
    }

    public boolean isBlock() {
        return scriptObject != null && scriptObject.block != null;
    }

    public boolean isEntity() {
        return scriptObject != null && scriptObject.entity != null;
    }

    public boolean fulfillsConditions(EntityPlayerMP player) {
        if (scriptConditions == null) return true;

        boolean achievement = cond(scriptConditions.conditionAchievement).matches(player);
        boolean gamemode = cond(scriptConditions.conditionGamemode).matches(
            Integer.valueOf(
                player.theItemInWorldManager.getGameType()
                    .getID()));
        boolean gamestage = cond(scriptConditions.conditionGamestage).matches(player);
        boolean position = cond(scriptConditions.conditionPosition).matches(player);
        boolean xp = cond(scriptConditions.conditionXp).matches(Integer.valueOf(player.experienceLevel));
        boolean scoreboard = cond(scriptConditions.conditionScoreboard).matches(player);
        boolean effects = cond(scriptConditions.conditionEffects).matches(player);

        return achievement && gamemode && gamestage && position && xp && scoreboard && effects;
    }

    private static NumberBoundCondition cond(NumberBoundCondition given) {
        return given == null ? NumberBoundCondition.NONE : given;
    }

    private static GamestageCondition cond(GamestageCondition given) {
        return given == null ? GamestageCondition.NONE : given;
    }

    private static AchievementCondition cond(AchievementCondition given) {
        return given == null ? AchievementCondition.NONE : given;
    }

    private static ScoreboardCondition cond(ScoreboardCondition given) {
        return given == null ? ScoreboardCondition.NONE : given;
    }

    private static PositionCondition cond(PositionCondition given) {
        return given == null ? PositionCondition.NONE : given;
    }

    private static EffectsCondition cond(EffectsCondition given) {
        return given == null ? EffectsCondition.NONE : given;
    }

    public static class ScriptObject {

        public ScriptObjectBlock block;
        public ScriptObjectEntity entity;
    }

    public static class ScriptObjectBlock {

        public String name;
        public NumberBoundCondition hardness;
        public NumberBoundCondition resistance;
        public NBTCondition nbt;
    }

    public static class ScriptObjectEntity {

        public String name;
        public NumberBoundCondition health;
        public NumberBoundCondition height;
        public NumberBoundCondition width;
        public NBTCondition nbt;
    }

    public static class ScriptConditions {

        public GamestageCondition conditionGamestage;
        public AchievementCondition conditionAchievement;
        public NumberBoundCondition conditionXp;
        public NumberBoundCondition conditionGamemode;
        public ScoreboardCondition conditionScoreboard;
        public PositionCondition conditionPosition;
        public EffectsCondition conditionEffects;
    }

    public static class ScriptRender {

        public String nameBlock;
        public String nameEntity;
        public NBTTagCompound renderNbt;
        public OptionalVec3 renderTranslation;
        public OptionalVec3 renderRotation;
        public OptionalVec3 renderScale;
        public OptionalVec3 renderRotationLeftArm;
        public OptionalVec3 renderRotationRightArm;
        public boolean renderLeftArm = true;
        public boolean renderRightArm = true;
    }

    public static class ScriptEffects {

        public String commandPickup = "";
        public String commandLoop = "";
        public String commandPlace = "";
    }

    public boolean isOverrideChecks() {
        return overrideChecks;
    }

    public String getRawJson() {
        return rawJson;
    }

}
