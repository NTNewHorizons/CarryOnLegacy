/*
 * Carry On — GNU Lesser General Public License v3 or later.
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * See LICENSE for terms and attribution to the original Carry On authors.
 */
package tschipp.carryon.proxy;

import java.io.File;

import cpw.mods.fml.common.network.simpleimpl.IMessage;

public class CommonProxy {

    public void preInit(File configDirectory) {}

    public void init() {}

    public void receiveClientPacket(IMessage message) {}
}
