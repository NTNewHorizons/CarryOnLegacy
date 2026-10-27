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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import tschipp.carryon.Constants;

/**
 * Loads Carry On scripts from config/carryon/scripts on 1.7.10 (the modern version
 * uses data packs). Loaded at server start and synced to joining clients.
 */
public class ScriptLoader {

    /**
     * Reads all *.json files in config/carryon/scripts (directly or from a given modpack-specified folder).
     */
    public static void loadScripts(File configDir) {
        ScriptManager.SCRIPTS.clear();

        if (!Constants.COMMON_CONFIG.settings.useScripts) return;

        File scripts = new File(configDir, "carryon/scripts");
        if (!scripts.isDirectory()) return;

        File[] files = scripts.listFiles();
        if (files == null) return;
        // Stable priority ties must not depend on the platform's directory enumeration order.
        Arrays.sort(
            files,
            (left, right) -> left.getName()
                .compareTo(right.getName()));

        List<CarryOnScript> loaded = new ArrayList<CarryOnScript>();
        for (File f : files) {
            if (!f.isFile() || !f.getName()
                .endsWith(".json")) continue;
            try (InputStreamReader reader = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8)) {
                JsonElement root = new JsonParser().parse(reader);
                CarryOnScript script = ScriptParser.fromJsonObject(root.getAsJsonObject(), root.toString());
                if (script != null && script.isValid()) loaded.add(script);
                else Constants.LOG.warn(
                    "Carry On script " + f.getName()
                        + " is invalid (needs a valid object and at least one condition/render/effect)");
            } catch (FileNotFoundException e) {
                continue;
            } catch (Exception e) {
                Constants.LOG.warn("Error while parsing Carry On script " + f.getName() + ": " + e.getMessage());
            }
        }

        Collections.sort(loaded, new java.util.Comparator<CarryOnScript>() {

            @Override
            public int compare(CarryOnScript s1, CarryOnScript s2) {
                return Long.compare(s2.priority, s1.priority);
            }
        });

        ScriptManager.SCRIPTS.addAll(loaded);
        Constants.LOG.info("Loaded " + ScriptManager.SCRIPTS.size() + " Carry On scripts");
    }

}
