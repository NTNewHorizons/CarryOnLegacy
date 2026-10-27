/*
 * Carry On — GNU Lesser General Public License v3 or later.
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * See LICENSE for terms and attribution to the original Carry On authors.
 */
package tschipp.carryon.client.render;

import java.util.IdentityHashMap;
import java.util.Map;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderPlayerEvent;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import tschipp.carryon.Constants;
import tschipp.carryon.common.carry.CarryOnData;
import tschipp.carryon.common.carry.CarryOnDataManager;
import tschipp.carryon.common.scripting.CarryOnScript.ScriptRender;

/** Event-scoped arm delegates apply the pose after vanilla setRotationAngles, without a mixin. */
public final class CarryRenderHelper {

    /** First-person sleeves use the player's skin and the same script visibility/rotation controls. */
    public static void renderFirstPersonArms(net.minecraft.client.entity.AbstractClientPlayer player,
        ScriptRender script) {
        net.minecraft.client.Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(player.getLocationSkin());
        ModelBiped model = new ModelBiped();
        org.lwjgl.opengl.GL11.glPushMatrix();
        try {
            org.lwjgl.opengl.GL11.glTranslated(0, -0.4, 0.05);
            org.lwjgl.opengl.GL11.glScalef(1, -1, -1);
            poseArm(
                model.bipedLeftArm,
                script == null || script.renderLeftArm,
                script == null ? Vec3.createVectorHelper(-69, -14, 0)
                    : script.renderRotationLeftArm.getVec(-69, -14, 0));
            poseArm(
                model.bipedRightArm,
                script == null || script.renderRightArm,
                script == null ? Vec3.createVectorHelper(-69, 14, 0)
                    : script.renderRotationRightArm.getVec(-69, 14, 0));
        } finally {
            org.lwjgl.opengl.GL11.glPopMatrix();
        }
    }

    private static void poseArm(ModelRenderer arm, boolean visible, Vec3 degrees) {
        if (!visible) return;
        Vec3 angles = radians(degrees);
        arm.rotateAngleX = (float) angles.xCoord;
        arm.rotateAngleY = (float) angles.yCoord;
        arm.rotateAngleZ = (float) angles.zCoord;
        arm.render(0.0625F);
    }

    private final Map<ModelBiped, ModelRenderer[]> saved = new IdentityHashMap<ModelBiped, ModelRenderer[]>();

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void before(RenderPlayerEvent.Pre event) {
        if (!Constants.CLIENT_CONFIG.renderArms) return;
        CarryOnData data = CarryOnDataManager.getCarryData(event.entityPlayer);
        if (!data.isCarrying()) return;
        ScriptRender script = data.getActiveScript()
            .isPresent()
                ? data.getActiveScript()
                    .get().scriptRender
                : null;
        install(event.renderer.modelBipedMain, script);
        install(event.renderer.modelArmor, script);
        install(event.renderer.modelArmorChestplate, script);
    }

    private void install(ModelBiped model, ScriptRender script) {
        if (saved.containsKey(model)) return;
        ModelRenderer left = model.bipedLeftArm;
        ModelRenderer right = model.bipedRightArm;
        saved.put(model, new ModelRenderer[] { left, right });
        model.bipedLeftArm = new Arm(
            model,
            left,
            script == null || script.renderLeftArm,
            script == null ? Vec3.createVectorHelper(-1.2, -0.25, 0)
                : radians(script.renderRotationLeftArm.getVec(-69, -14, 0)));
        model.bipedRightArm = new Arm(
            model,
            right,
            script == null || script.renderRightArm,
            script == null ? Vec3.createVectorHelper(-1.2, 0.25, 0)
                : radians(script.renderRotationRightArm.getVec(-69, 14, 0)));
    }

    private static Vec3 radians(Vec3 vec) {
        return Vec3
            .createVectorHelper(Math.toRadians(vec.xCoord), Math.toRadians(vec.yCoord), Math.toRadians(vec.zCoord));
    }

    @SubscribeEvent
    public void after(RenderPlayerEvent.Post event) {
        restore();
    }

    /** Also restore when another listener cancels rendering after our Pre event. */
    @SubscribeEvent
    public void frame(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.END) restore();
    }

    public void restore() {
        for (Map.Entry<ModelBiped, ModelRenderer[]> entry : saved.entrySet()) {
            entry.getKey().bipedLeftArm = entry.getValue()[0];
            entry.getKey().bipedRightArm = entry.getValue()[1];
        }
        saved.clear();
    }

    private static final class Arm extends ModelRenderer {

        private final ModelRenderer original;
        private final boolean visible;
        private final Vec3 angles;

        Arm(ModelBiped model, ModelRenderer original, boolean visible, Vec3 angles) {
            super(model);
            model.boxList.remove(this);
            this.original = original;
            this.visible = visible;
            this.angles = angles;
        }

        @Override
        public void render(float scale) {
            if (!visible) return;
            float x = original.rotateAngleX, y = original.rotateAngleY, z = original.rotateAngleZ;
            try {
                original.rotateAngleX = (float) angles.xCoord;
                original.rotateAngleY = (float) angles.yCoord;
                original.rotateAngleZ = (float) angles.zCoord;
                original.render(scale);
            } finally {
                original.rotateAngleX = x;
                original.rotateAngleY = y;
                original.rotateAngleZ = z;
            }
        }
    }
}
