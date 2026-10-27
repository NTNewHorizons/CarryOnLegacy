/*
 * Carry On — GNU Lesser General Public License v3 or later.
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * See LICENSE for terms and attribution to the original Carry On authors.
 */
package tschipp.carryon;

import java.io.File;

import net.minecraftforge.common.MinecraftForge;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppedEvent;
import tschipp.carryon.common.command.CommandCarryOn;
import tschipp.carryon.common.config.ConfigLoader;
import tschipp.carryon.common.config.ListHandler;
import tschipp.carryon.common.scripting.ScriptLoader;
import tschipp.carryon.events.CommonEvents;
import tschipp.carryon.networking.CarryNetwork;
import tschipp.carryon.proxy.CommonProxy;

@Mod(
    modid = Constants.MOD_ID,
    name = Constants.MOD_NAME,
    version = Tags.VERSION,
    acceptedMinecraftVersions = "[1.7.10]")
public class CarryOn {

    @SidedProxy(clientSide = "tschipp.carryon.proxy.ClientProxy", serverSide = "tschipp.carryon.proxy.CommonProxy")
    public static CommonProxy proxy;
    private File configDirectory;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        configDirectory = event.getModConfigurationDirectory();
        ConfigLoader.loadCommon(new File(configDirectory, "carryon-common.cfg"));
        CarryNetwork.register();
        CommonEvents events = new CommonEvents();
        MinecraftForge.EVENT_BUS.register(events);
        FMLCommonHandler.instance()
            .bus()
            .register(events);
        proxy.preInit(configDirectory);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init();
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        CarryNetwork.clearServerTasks();
        ListHandler.initConfigLists();
        ScriptLoader.loadScripts(configDirectory);
        event.registerServerCommand(new CommandCarryOn());
        Constants.LOG.info(
            "Carry On initialized; stone registry name: " + ListHandler.getBlockName(net.minecraft.init.Blocks.stone));
    }

    @Mod.EventHandler
    public void serverStopped(FMLServerStoppedEvent event) {
        CarryNetwork.clearServerTasks();
    }
}
