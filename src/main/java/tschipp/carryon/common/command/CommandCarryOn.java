/*
 * Carry On — GNU Lesser General Public License v3 or later.
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * See LICENSE for terms and attribution to the original Carry On authors.
 */
package tschipp.carryon.common.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.potion.Potion;
import net.minecraft.util.ChatComponentText;

import tschipp.carryon.common.carry.CarryOnData;
import tschipp.carryon.common.carry.CarryOnDataManager;
import tschipp.carryon.common.carry.CarryType;
import tschipp.carryon.common.carry.PlacementHandler;

public class CommandCarryOn extends CommandBase {

    @Override
    public String getCommandName() {
        return "carryon";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/carryon debug|clear|place [player]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length < 1 || args.length > 2) throw new WrongUsageException(getCommandUsage(sender));
        String action = args[0];
        if (!action.equals("debug") && !action.equals("clear") && !action.equals("place"))
            throw new WrongUsageException(getCommandUsage(sender));
        if ((args.length == 2 || action.equals("place")) && !sender.canCommandSenderUseCommand(2, getCommandName()))
            throw new net.minecraft.command.CommandException("commands.generic.permission");
        EntityPlayerMP player = args.length == 2 ? getPlayer(sender, args[1]) : getCommandSenderAsPlayer(sender);
        CarryOnData data = CarryOnDataManager.getCarryData(player);
        if (action.equals("debug")) {
            sender.addChatMessage(new ChatComponentText("Carry type: " + data.getType() + "; NBT: " + data.getNbt()));
            sender.addChatMessage(
                new ChatComponentText(
                    "Locked slot: " + data.getSelected()
                        + "; active script: "
                        + (data.getActiveScript()
                            .isPresent()
                                ? data.getActiveScript()
                                    .get()
                                    .getRawJson()
                                : "none")));
            net.minecraft.util.MovingObjectPosition hit = player.rayTrace(5, 1);
            if (hit != null && hit.typeOfHit == net.minecraft.util.MovingObjectPosition.MovingObjectType.BLOCK) {
                net.minecraft.block.Block block = player.worldObj.getBlock(hit.blockX, hit.blockY, hit.blockZ);
                int metadata = player.worldObj.getBlockMetadata(hit.blockX, hit.blockY, hit.blockZ);
                net.minecraft.tileentity.TileEntity tile = player.worldObj
                    .getTileEntity(hit.blockX, hit.blockY, hit.blockZ);
                net.minecraft.nbt.NBTTagCompound tag = null;
                if (tile != null) {
                    tag = new net.minecraft.nbt.NBTTagCompound();
                    tile.writeToNBT(tag);
                }
                java.util.Optional<tschipp.carryon.common.scripting.CarryOnScript> script = tschipp.carryon.common.scripting.ScriptManager
                    .inspectBlock(block, metadata, player.worldObj, hit.blockX, hit.blockY, hit.blockZ, tag);
                sender.addChatMessage(
                    new ChatComponentText(
                        "Target: " + tschipp.carryon.common.config.ListHandler.getBlockName(block)
                            + ";"
                            + metadata
                            + "; permitted: "
                            + tschipp.carryon.common.config.ListHandler.isPermitted(block)
                            + "; override checks: "
                            + (script.isPresent() && script.get()
                                .isOverrideChecks())
                            + "; tile NBT: "
                            + tag));
            }
        } else if (action.equals("place")) {
            PlacementHandler.placeCarried(player);
            sender.addChatMessage(new ChatComponentText("Placement attempted; carrying: " + data.isCarrying()));
        } else {
            if (data.isCarrying(CarryType.PLAYER)) {
                net.minecraft.entity.player.EntityPlayer rider = data.getCarryingPlayer(player.worldObj);
                if (rider != null && rider.ridingEntity == player) {
                    rider.mountEntity(null);
                    tschipp.carryon.networking.CarryNetwork
                        .sendStartRidingOtherToAllInDimension(player, player.getEntityId(), rider.getEntityId(), false);
                }
            }
            data.clear();
            CarryOnDataManager.setCarryData(player, data);
            player.removePotionEffect(Potion.moveSlowdown.id);
            sender.addChatMessage(new ChatComponentText("Cleared carried object."));
        }
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return index == 1;
    }
}
