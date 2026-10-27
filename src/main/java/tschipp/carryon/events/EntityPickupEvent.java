/*
 * Carry On — GNU Lesser General Public License v3 or later.
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * See LICENSE for terms and attribution to the original Carry On authors.
 */
package tschipp.carryon.events;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;

import cpw.mods.fml.common.eventhandler.Cancelable;

@Cancelable
public class EntityPickupEvent extends PlayerEvent {

    public final Entity target;

    public EntityPickupEvent(EntityPlayer player, Entity target) {
        super(player);
        this.target = target;
    }
}
