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

package tschipp.carryon;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tschipp.carryon.common.config.CarryConfig;

public class Constants {

    public static final String MOD_ID = "carryon";
    public static final String MOD_NAME = "Carry On";
    public static final Logger LOG = LogManager.getLogger(MOD_ID);

    public static final CarryConfig.Common COMMON_CONFIG = new CarryConfig.Common();
    public static final CarryConfig.Client CLIENT_CONFIG = new CarryConfig.Client();

    /** Extended-properties key used to persist the carried object on the player. */
    public static final String CARRY_DATA_PROPERTY_KEY = "CarryOnData";

}
