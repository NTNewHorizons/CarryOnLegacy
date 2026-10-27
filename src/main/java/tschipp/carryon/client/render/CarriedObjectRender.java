/*
 * Carry On — GNU Lesser General Public License v3 or later.
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * See LICENSE for terms and attribution to the original Carry On authors.
 */
package tschipp.carryon.client.render;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import tschipp.carryon.Constants;
import tschipp.carryon.client.modeloverride.ModelOverride;
import tschipp.carryon.client.modeloverride.ModelOverrideHandler;
import tschipp.carryon.common.carry.CarryOnData;
import tschipp.carryon.common.carry.CarryOnDataManager;
import tschipp.carryon.common.carry.CarryType;
import tschipp.carryon.common.scripting.CarryOnScript.ScriptRender;

/** Renders detached carry snapshots; reconstructed objects are never added to the client world. */
public class CarriedObjectRender {

    private final RenderBlocks blocks = new RenderBlocks();

    @SubscribeEvent
    public void render(RenderWorldLastEvent event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.theWorld == null || minecraft.renderViewEntity == null) return;
        Entity camera = minecraft.renderViewEntity;
        double cx = interpolate(camera.lastTickPosX, camera.posX, event.partialTicks);
        double cy = interpolate(camera.lastTickPosY, camera.posY, event.partialTicks);
        double cz = interpolate(camera.lastTickPosZ, camera.posZ, event.partialTicks);
        for (Object object : minecraft.theWorld.playerEntities) {
            EntityPlayer player = (EntityPlayer) object;
            CarryOnData data = CarryOnDataManager.getCarryData(player);
            if (!data.isCarrying() || data.isCarrying(CarryType.PLAYER) || player.isInvisible()) continue;
            boolean firstPerson = player == camera && minecraft.gameSettings.thirdPersonView == 0;
            if (firstPerson && minecraft.gameSettings.hideGUI) continue;
            if (data.isCarrying(CarryType.BLOCK) && data.getBlock() == null) continue;
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            GL11.glPushMatrix();
            float previousLightX = OpenGlHelper.lastBrightnessX;
            float previousLightY = OpenGlHelper.lastBrightnessY;
            try {
                // Match RenderPlayer: legacy player posY is offset from the model's feet.
                double feetY = interpolate(player.lastTickPosY, player.posY, event.partialTicks) - player.yOffset;
                if (!firstPerson && player.isSneaking()
                    && !(player instanceof net.minecraft.client.entity.EntityPlayerSP)) feetY -= 0.125;
                // Legacy getEyeHeight() is a small offset (default 0.12), not height from feet.
                // First-person uses the view origin; only third-person uses the feet anchor.
                double originY = firstPerson
                    ? interpolate(player.lastTickPosY, player.posY, event.partialTicks) + player.getEyeHeight()
                        - player.getDefaultEyeHeight()
                    : feetY + 0.9;
                GL11.glTranslated(
                    interpolate(player.lastTickPosX, player.posX, event.partialTicks) - cx,
                    originY - cy,
                    interpolate(player.lastTickPosZ, player.posZ, event.partialTicks) - cz);
                float yaw = firstPerson
                    ? interpolateAngle(player.prevRotationYaw, player.rotationYaw, event.partialTicks)
                    : interpolateAngle(player.prevRenderYawOffset, player.renderYawOffset, event.partialTicks);
                GL11.glRotatef(-yaw, 0, 1, 0);
                if (firstPerson) {
                    // Hold relative to the view rather than the lagging third-person body yaw.
                    GL11.glRotatef(
                        interpolateAngle(player.prevRotationPitch, player.rotationPitch, event.partialTicks),
                        1,
                        0,
                        0);
                    GL11.glTranslated(0, -0.55, 0);
                }
                GL11.glTranslated(0, 0, 0.7);
                float swing = player.getSwingProgress(event.partialTicks);
                GL11.glTranslated(0, -0.04 * Math.sin(Math.PI * swing), 0);
                GL11.glColor4f(1, 1, 1, 1);
                GL11.glEnable(GL11.GL_NORMALIZE);
                int light = player.getBrightnessForRender(event.partialTicks);
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light % 65536, light / 65536);
                ScriptRender script = data.getActiveScript()
                    .isPresent()
                        ? data.getActiveScript()
                            .get().scriptRender
                        : null;
                if (firstPerson && Constants.CLIENT_CONFIG.renderArms
                    && player instanceof net.minecraft.client.entity.AbstractClientPlayer)
                    CarryRenderHelper
                        .renderFirstPersonArms((net.minecraft.client.entity.AbstractClientPlayer) player, script);
                if (script != null) {
                    Vec3 translation = script.renderTranslation.getVec(0, 0, 0);
                    Vec3 rotation = script.renderRotation.getVec(0, 0, 0);
                    Vec3 scale = script.renderScale.getVec(1, 1, 1);
                    GL11.glTranslated(translation.xCoord, translation.yCoord, translation.zCoord);
                    GL11.glRotated(rotation.xCoord, 1, 0, 0);
                    GL11.glRotated(rotation.yCoord, 0, 1, 0);
                    GL11.glRotated(rotation.zCoord, 0, 0, 1);
                    GL11.glScaled(scale.xCoord, scale.yCoord, scale.zCoord);
                }
                if (data.isCarrying(CarryType.BLOCK)) {
                    GL11.glScalef(0.65F, 0.65F, 0.65F);
                    if (Constants.CLIENT_CONFIG.facePlayer) GL11.glRotatef(180, 0, 1, 0);
                    minecraft.getTextureManager()
                        .bindTexture(TextureMap.locationBlocksTexture);
                    ModelOverride override = ModelOverrideHandler
                        .getModelOverride(data.getBlock(), data.getBlockMetadata(), data.getTileNbt());
                    if (override != null && override.item != null) {
                        renderItem(override.item, minecraft, event.partialTicks);
                        continue;
                    }
                    Block block = override == null ? data.getBlock() : override.block;
                    int metadata = override == null ? data.getBlockMetadata() : override.targetMetadata;
                    if (script != null && script.nameBlock != null && Block.blockRegistry.containsKey(script.nameBlock))
                        block = (Block) Block.blockRegistry.getObject(script.nameBlock);
                    TileEntity tile = override == null && block == data.getBlock()
                        ? data.getBlockEntity(minecraft.theWorld, 0, 0, 0)
                        : block.createTileEntity(minecraft.theWorld, metadata);
                    if (tile != null && script != null && script.renderNbt != null) {
                        net.minecraft.nbt.NBTTagCompound tag = new net.minecraft.nbt.NBTTagCompound();
                        tile.writeToNBT(tag);
                        for (Object key : script.renderNbt.func_150296_c()) tag.setTag(
                            (String) key,
                            script.renderNbt.getTag((String) key)
                                .copy());
                        tile.readFromNBT(tag);
                    }
                    if (tile != null && TileEntityRendererDispatcher.instance.hasSpecialRenderer(tile)) {
                        tile.setWorldObj(minecraft.theWorld);
                        tile.blockType = block;
                        tile.blockMetadata = metadata;
                        TileEntityRendererDispatcher.instance
                            .renderTileEntityAt(tile, -0.5, -0.5, -0.5, event.partialTicks);
                    } else {
                        blocks.renderBlockAsItem(block, metadata, player.getBrightness(event.partialTicks));
                    }
                } else if (data.isCarrying(CarryType.ENTITY)) {
                    Entity entity = data.getEntity(minecraft.theWorld);
                    if (script != null && script.nameEntity != null) entity = net.minecraft.entity.EntityList
                        .createEntityByName(script.nameEntity, minecraft.theWorld);
                    if (entity != null && script != null && script.renderNbt != null) {
                        net.minecraft.nbt.NBTTagCompound tag = new net.minecraft.nbt.NBTTagCompound();
                        entity.writeToNBT(tag);
                        for (Object key : script.renderNbt.func_150296_c()) tag.setTag(
                            (String) key,
                            script.renderNbt.getTag((String) key)
                                .copy());
                        entity.readFromNBT(tag);
                    }
                    if (entity != null) {
                        entity.setPosition(player.posX, player.posY, player.posZ);
                        float scale = Math.min(0.65F, 1.0F / Math.max(entity.width, entity.height));
                        GL11.glScalef(scale, scale, scale);
                        if (Constants.CLIENT_CONFIG.rotateEntitiesSideways) GL11.glRotatef(90, 0, 1, 0);
                        renderDetachedEntity(entity, 0, -0.5, 0, event.partialTicks);
                    }
                }
            } finally {
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, previousLightX, previousLightY);
                GL11.glPopMatrix();
                GL11.glPopAttrib();
            }
        }
    }

    @SubscribeEvent
    public void hand(RenderHandEvent event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.thePlayer == null || minecraft.gameSettings.thirdPersonView != 0
            || !CarryOnDataManager.getCarryData(minecraft.thePlayer)
                .isCarrying()
            || CarryOnDataManager.getCarryData(minecraft.thePlayer)
                .isCarrying(CarryType.PLAYER))
            return;
        // Vanilla renders an unrelated empty-hand swing over the carried object. Do not clear
        // the world-space carry object's depth buffer for that hand. Third-person arms use the pose delegate.
        event.setCanceled(true);
    }

    private static float interpolateAngle(float previous, float current, float partialTicks) {
        return previous + net.minecraft.util.MathHelper.wrapAngleTo180_float(current - previous) * partialTicks;
    }

    private static double interpolate(double previous, double current, float ticks) {
        return previous + (current - previous) * ticks;
    }

    private static void renderItem(ItemStack stack, Minecraft minecraft, float partialTicks) {
        EntityItem item = new EntityItem(
            minecraft.theWorld,
            minecraft.thePlayer.posX,
            minecraft.thePlayer.posY,
            minecraft.thePlayer.posZ,
            stack.copy());
        item.hoverStart = 0;
        renderDetachedEntity(item, 0, 0, 0, partialTicks);
    }

    private static void renderDetachedEntity(Entity entity, double x, double y, double z, float partialTicks) {
        // Direct renderer invocation omits world shadows, fire, and debug bounding boxes.
        net.minecraft.client.renderer.entity.Render renderer = RenderManager.instance.getEntityRenderObject(entity);
        if (renderer != null) renderer.doRender(entity, x, y, z, 0, partialTicks);
    }
}
