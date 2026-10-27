/*
 * Carry On — GNU Lesser General Public License v3 or later.
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * See LICENSE for terms and attribution to the original Carry On authors.
 */
package tschipp.carryon;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.settings.KeyBinding;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import tschipp.carryon.client.keybinds.CarryOnKeybinds;
import tschipp.carryon.common.carry.CarryOnData;
import tschipp.carryon.common.carry.CarryOnDataManager;
import tschipp.carryon.networking.CarryNetwork;
import tschipp.carryon.networking.serverbound.ServerboundCarryKeyPressedPacket;

public final class CarryOnCommonClient {

    private static boolean lastPressed;
    private static Object lastPlayer;

    public static void tick() {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.thePlayer == null) {
            lastPlayer = null;
            lastPressed = false;
            return;
        }
        KeyBinding key = CarryOnKeybinds.carryKey.getKeyCode() == 0 ? minecraft.gameSettings.keyBindSneak
            : CarryOnKeybinds.carryKey;
        int code = key.getKeyCode();
        boolean pressed = minecraft.currentScreen == null
            && (code < 0 ? Mouse.isButtonDown(code + 100) : code > 0 && Keyboard.isKeyDown(code));
        if (lastPlayer != minecraft.thePlayer || pressed != lastPressed) {
            CarryNetwork.CHANNEL.sendToServer(new ServerboundCarryKeyPressedPacket(pressed));
            lastPressed = pressed;
            lastPlayer = minecraft.thePlayer;
        }
        CarryOnData data = CarryOnDataManager.getCarryData(minecraft.thePlayer);
        if (data.isCarrying()) {
            if (data.getSelected() >= 0 && data.getSelected() < 9)
                minecraft.thePlayer.inventory.currentItem = data.getSelected();
            if (minecraft.currentScreen instanceof GuiContainer) minecraft.thePlayer.closeScreen();
        }
    }
}
