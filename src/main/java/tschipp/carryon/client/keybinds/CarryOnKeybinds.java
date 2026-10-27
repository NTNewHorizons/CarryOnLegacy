/*
 * Carry On — GNU Lesser General Public License v3 or later.
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * See LICENSE for terms and attribution to the original Carry On authors.
 */
package tschipp.carryon.client.keybinds;

import net.minecraft.client.settings.KeyBinding;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.client.registry.ClientRegistry;

public final class CarryOnKeybinds {

    public static KeyBinding carryKey;

    public static void register() {
        carryKey = new CarryKeyBinding();
        ClientRegistry.registerKeyBinding(carryKey);
    }

    /** Carry is physically polled; do not steal events from bindings sharing its key. */
    private static final class CarryKeyBinding extends KeyBinding {

        private int carryCode = Keyboard.KEY_LSHIFT;

        private CarryKeyBinding() {
            // 1.7.10's event hash uses KeyBinding's private keyCode, including on rebuild.
            // Keep that code unbound while exposing the real binding to controls/configuration.
            super("key.carry.desc", Keyboard.KEY_NONE, "key.carry.category");
        }

        @Override
        public int getKeyCode() {
            return carryCode;
        }

        @Override
        public void setKeyCode(int keyCode) {
            carryCode = keyCode;
        }

        @Override
        public int getKeyCodeDefault() {
            return Keyboard.KEY_LSHIFT;
        }
    }
}
