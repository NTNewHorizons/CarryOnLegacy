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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.stats.Achievement;
import net.minecraft.stats.AchievementList;
import net.minecraft.stats.StatFileWriter;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

import tschipp.carryon.Constants;
import tschipp.carryon.platform.GamestagesCompat;

/**
 * 1.7.10 port of the modern Matchables. All conditions keep the same string formats.
 * Gamestages always match (no gamestages ecosystem on 1.7.10; hook kept for future providers).
 */
public final class Matchables {

    public interface Matchable<T> {

        boolean matches(T elem);
    }

    private static float getValueFromStringOrDefault(String toGetFrom, String key, float defaultVal) {
        Float val = getValueFromString(toGetFrom, key);
        return val == null ? defaultVal : val;
    }

    private static Float getValueFromString(String toGetFrom, String key) {
        if (toGetFrom == null || toGetFrom.isEmpty()) return null;

        String[] s = toGetFrom.split(",");
        for (String string : s) {
            String[] pair = string.trim()
                .split("=", 2);
            if (pair.length == 2 && pair[0].trim()
                .equals(key)) {
                float value = Float.parseFloat(pair[1].trim());
                if (Float.isNaN(value) || Float.isInfinite(value))
                    throw new IllegalArgumentException("Non-finite vector component: " + key);
                return value;
            }
        }

        return null;
    }

    public static class NumberBoundCondition implements Matchable<Number> {

        public final String bounds;

        public static final NumberBoundCondition NONE = new NumberBoundCondition("");

        public NumberBoundCondition(String bounds) {
            this.bounds = bounds;
            if (bounds != null && !bounds.isEmpty()) {
                String value = bounds.trim()
                    .replaceFirst("^(<=|>=|<|>|=)", "")
                    .trim();
                double parsed = Double.parseDouble(value);
                if (Double.isNaN(parsed) || Double.isInfinite(parsed))
                    throw new IllegalArgumentException("Non-finite number bound: " + bounds);
            }
        }

        @Override
        public boolean matches(Number num) {
            double number = num.doubleValue();

            if (bounds == null || bounds.isEmpty()) return true;

            try {
                if (bounds.contains("<=")) return number < Double.parseDouble(bounds.replace("<=", ""))
                    || number == Double.parseDouble(bounds.replace("<=", ""));
                if (bounds.contains(">=")) return number > Double.parseDouble(bounds.replace(">=", ""))
                    || number == Double.parseDouble(bounds.replace(">=", ""));
                if (bounds.contains("<")) return number < Double.parseDouble(bounds.replace("<", ""));
                if (bounds.contains(">")) return number > Double.parseDouble(bounds.replace(">", ""));
                if (bounds.contains("=")) return number == Double.parseDouble(bounds.replace("=", ""));
                return number == Double.parseDouble(bounds);
            } catch (Exception e) {
                throw new RuntimeException(
                    "Error while parsing Number bound for string: " + bounds + ". Error: " + e.getMessage());
            }
        }
    }

    /** 1.7.10 replacement for the modern advancement condition: uses vanilla achievements. */
    public static class AchievementCondition implements Matchable<EntityPlayerMP> {

        public final String achievement;

        public static final AchievementCondition NONE = new AchievementCondition("");

        public AchievementCondition(String achievement) {
            this.achievement = achievement;
        }

        @Override
        public boolean matches(EntityPlayerMP player) {
            if (achievement == null || achievement.isEmpty()) return true;

            Achievement found = null;
            for (Achievement a : AchievementList.achievementList) {
                if (achievement.equals(a.statId)) {
                    found = a;
                    break;
                }
            }

            if (found == null) {
                Constants.LOG.debug("Cannot resolve achievement '" + achievement + "', condition treated as fulfilled");
                return true;
            }

            StatFileWriter stats = ((EntityPlayerMP) player).func_147099_x();
            return stats.hasAchievementUnlocked(found);
        }
    }

    public static class GamestageCondition implements Matchable<EntityPlayerMP> {

        public final String gamestage;

        public static final GamestageCondition NONE = new GamestageCondition("");

        public GamestageCondition(String gamestage) {
            this.gamestage = gamestage;
        }

        @Override
        public boolean matches(EntityPlayerMP player) {
            if (gamestage == null || gamestage.isEmpty()) return true;

            return GamestagesCompat.hasStage(player, gamestage);
        }
    }

    public static class ScoreboardCondition implements Matchable<EntityPlayerMP> {

        public final String cond;

        private final String objectiveName;
        private final NumberBoundCondition bounds;

        public static final ScoreboardCondition NONE = new ScoreboardCondition("");

        public ScoreboardCondition(String cond) {
            this.cond = cond;
            if (cond == null || cond.isEmpty()) {
                objectiveName = "";
                bounds = NumberBoundCondition.NONE;
            } else {
                java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("^([^<>=]+)([<>=].*)$")
                    .matcher(cond);
                if (!matcher.matches()) throw new IllegalArgumentException("Invalid scoreboard condition: " + cond);
                objectiveName = matcher.group(1)
                    .trim();
                if (objectiveName.isEmpty()) throw new IllegalArgumentException("Missing scoreboard objective");
                bounds = new NumberBoundCondition(matcher.group(2));
            }
        }

        @Override
        public boolean matches(EntityPlayerMP elem) {
            if (cond == null || cond.isEmpty()) return true;

            net.minecraft.scoreboard.Scoreboard score = elem.worldObj.getScoreboard();

            int scoreVal = 0;
            try {
                if (score.getObjective(objectiveName) == null) return false;
                net.minecraft.scoreboard.Score objective = score
                    .func_96529_a(elem.getCommandSenderName(), score.getObjective(objectiveName));
                scoreVal = objective.getScorePoints();
            } catch (Exception e) {
                Constants.LOG.debug("Cannot read scoreboard objective '" + objectiveName + "'");
                return false;
            }

            return bounds.matches(Integer.valueOf(scoreVal));
        }
    }

    public static class PositionCondition implements Matchable<EntityPlayer> {

        public final String cond;

        public static final PositionCondition NONE = new PositionCondition("");

        public PositionCondition(String cond) {
            this.cond = cond;
        }

        @Override
        public boolean matches(EntityPlayer elem) {
            if (cond == null || cond.isEmpty()) return true;

            int bx = (int) getValueFromStringOrDefault(cond, "x", 0);
            int by = (int) getValueFromStringOrDefault(cond, "y", 0);
            int bz = (int) getValueFromStringOrDefault(cond, "z", 0);
            int ex = (int) getValueFromStringOrDefault(cond, "dx", 0);
            int ey = (int) getValueFromStringOrDefault(cond, "dy", 0);
            int ez = (int) getValueFromStringOrDefault(cond, "dz", 0);
            int px = MathHelper.floor_double(elem.posX);
            int py = MathHelper.floor_double(elem.posY);
            int pz = MathHelper.floor_double(elem.posZ);

            boolean x = getValueFromString(cond, "x") == null
                || px >= Math.min(bx, bx + ex) && px <= Math.max(bx, bx + ex);
            boolean y = getValueFromString(cond, "y") == null
                || py >= Math.min(by, by + ey) && py <= Math.max(by, by + ey);
            boolean z = getValueFromString(cond, "z") == null
                || pz >= Math.min(bz, bz + ez) && pz <= Math.max(bz, bz + ez);

            return x && y && z;
        }
    }

    public static class EffectsCondition implements Matchable<EntityPlayerMP> {

        public final String effects;

        public static final EffectsCondition NONE = new EffectsCondition("");

        public EffectsCondition(String effects) {
            this.effects = effects;
        }

        @Override
        public boolean matches(EntityPlayerMP player) {
            if (effects == null || effects.isEmpty()) return true;

            Collection<PotionEffect> fx = player.getActivePotionEffects();
            String[] potions = effects.split(",");

            List<String> names = new ArrayList<String>();
            List<Integer> levels = new ArrayList<Integer>();

            for (String pot : potions) {
                if (pot.contains("#")) {
                    String level = pot.substring(pot.indexOf("#"));
                    String name = pot.substring(0, pot.indexOf("#"));
                    level = level.replace("#", "");
                    int lev = 0;
                    try {
                        lev = Integer.parseInt(level);
                    } catch (Exception e) {}

                    levels.add(Integer.valueOf(lev));
                    names.add(name);
                } else {
                    levels.add(Integer.valueOf(0));
                    names.add(pot);
                }
            }

            int matches = 0;
            for (PotionEffect effect : fx) {
                int amp = effect.getAmplifier();
                String name = getPotionName(effect.getPotionID());

                if (names.contains(name)) {
                    int idx = names.indexOf(name);
                    int lev = levels.get(idx)
                        .intValue();

                    if (lev <= amp) matches++;
                }
            }

            return matches == potions.length;
        }

        private static String getPotionName(int id) {
            // Stable unlocalized names survive production obfuscation; Java field names do not.
            if (id >= 0 && id < Potion.potionTypes.length && Potion.potionTypes[id] != null) {
                String name = Potion.potionTypes[id].getName();
                if (name.startsWith("potion.")) return name.substring(7)
                    .toLowerCase(java.util.Locale.ROOT);
            }
            Map<String, Potion> byName = new HashMap<String, Potion>();
            Field[] fields = Potion.class.getFields();
            for (Field f : fields) {
                try {
                    Object o = f.get(null);
                    if (o instanceof Potion) {
                        byName.put(
                            f.getName()
                                .toLowerCase(),
                            (Potion) o);
                    }
                } catch (Exception e) {}
            }

            for (Map.Entry<String, Potion> e : byName.entrySet()) if (e.getValue().id == id) return e.getKey();

            return "";
        }
    }

    /** Containment NBT matching (replacement for NbtUtils.compareNbt). */
    public static class NBTCondition implements Matchable<NBTTagCompound> {

        public final NBTTagCompound tag;

        public static final NBTCondition NONE = new NBTCondition(new NBTTagCompound());

        public NBTCondition(NBTTagCompound tag) {
            this.tag = tag;
        }

        @Override
        public boolean matches(NBTTagCompound other) {
            if (other == null) return tag.hasNoTags();
            return compareNbt(tag, other);
        }

        private boolean compareNbt(NBTTagCompound expected, NBTTagCompound actual) {
            for (Object keyStr : expected.func_150296_c()) {
                String key = String.valueOf(keyStr);
                if (!actual.hasKey(key)) return false;
                if (!tagMatches(expected.getTag(key), actual.getTag(key))) return false;
            }
            return true;
        }

        private boolean tagMatches(net.minecraft.nbt.NBTBase expected, net.minecraft.nbt.NBTBase actual) {
            if (expected == null) return true;
            if (actual == null || expected.getId() != actual.getId()) return false;
            if (expected instanceof NBTTagCompound)
                return compareNbt((NBTTagCompound) expected, (NBTTagCompound) actual);
            if (expected instanceof net.minecraft.nbt.NBTTagList) {
                net.minecraft.nbt.NBTTagList le = (net.minecraft.nbt.NBTTagList) expected;
                net.minecraft.nbt.NBTTagList la = (net.minecraft.nbt.NBTTagList) actual;
                if (le.tagCount() == 0) return la.tagCount() == 0;
                // 1.7.10 has no generic list getter. Drain copies, never the original payload.
                java.util.List<net.minecraft.nbt.NBTBase> candidates = listElements(la);
                for (net.minecraft.nbt.NBTBase required : listElements(le)) {
                    boolean found = false;
                    for (net.minecraft.nbt.NBTBase candidate : candidates) {
                        if (tagMatches(required, candidate)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) return false;
                }
                return true;
            }
            return tagEquals(expected, actual);
        }

        private boolean tagEquals(net.minecraft.nbt.NBTBase a, net.minecraft.nbt.NBTBase b) {
            NBTTagCompound ca = new NBTTagCompound();
            NBTTagCompound cb = new NBTTagCompound();
            ca.setTag("k", a);
            cb.setTag("k", b);
            return ca.equals(cb);
        }

        private static java.util.List<net.minecraft.nbt.NBTBase> listElements(net.minecraft.nbt.NBTTagList source) {
            net.minecraft.nbt.NBTTagList copy = (net.minecraft.nbt.NBTTagList) source.copy();
            java.util.List<net.minecraft.nbt.NBTBase> elements = new java.util.ArrayList<net.minecraft.nbt.NBTBase>();
            while (copy.tagCount() > 0) elements.add(copy.removeTag(0));
            return elements;
        }
    }

    public static class OptionalVec3 {

        public static final OptionalVec3 NONE = new OptionalVec3("");

        String source;
        Vec3 vec;

        boolean x, y, z;

        public OptionalVec3(String source) {
            this.source = source == null ? "" : source;
            Optional<Float> xOpt = Optional.ofNullable(getValueFromString(this.source, "x"));
            Optional<Float> yOpt = Optional.ofNullable(getValueFromString(this.source, "y"));
            Optional<Float> zOpt = Optional.ofNullable(getValueFromString(this.source, "z"));

            float fX = 0, fY = 0, fZ = 0;

            if (xOpt.isPresent()) {
                fX = xOpt.get();
                this.x = true;
            }
            if (yOpt.isPresent()) {
                fY = yOpt.get();
                this.y = true;
            }
            if (zOpt.isPresent()) {
                fZ = zOpt.get();
                this.z = true;
            }

            vec = Vec3.createVectorHelper(fX, fY, fZ);
        }

        /**
         * Gets the contained optional vector. Nonexisting numbers are set to 0.
         */
        public Vec3 getVec() {
            return vec;
        }

        public Vec3 getVec(double dX, double dY, double dZ) {
            double vx = !this.x ? dX : vec.xCoord;
            double vy = !this.y ? dY : vec.yCoord;
            double vz = !this.z ? dZ : vec.zCoord;
            return Vec3.createVectorHelper(vx, vy, vz);
        }
    }

}
