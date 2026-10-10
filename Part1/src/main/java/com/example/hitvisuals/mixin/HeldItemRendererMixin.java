package com.example.hitvisuals.mixin;

import com.example.hitvisuals.VisualsConfig;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Положение, размер и поворот рук от первого лица (у каждой руки свои настройки), стиль взмаха. */
@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {
    @ModifyVariable(method = "renderFirstPersonItem", at = @At("HEAD"), argsOnly = true, ordinal = 2, require = 0)
    private float hitvisuals$swing(float swingProgress) {
        int style = VisualsConfig.I.swingStyle;
        if (style == 1) {
            return swingProgress * swingProgress * (3f - 2f * swingProgress);
        }
        if (style == 2) {
            return (float) Math.sqrt(Math.max(0f, swingProgress));
        }
        return swingProgress;
    }

    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"), require = 0)
    private void hitvisuals$hand(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand,
                                 float swingProgress, ItemStack item, float equipProgress,
                                 MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                                 CallbackInfo ci) {
        VisualsConfig c = VisualsConfig.I;
        if (c.swordPose && hand == Hand.MAIN_HAND && item.isIn(ItemTags.SWORDS)) {
            // меч лежит почти горизонтально лезвием влево, крупно
            float sd = player.getMainArm() == Arm.RIGHT ? 1f : -1f;
            matrices.translate((float) c.swordX * sd, (float) c.swordY, (float) c.swordZ);
            matrices.translate(0.56f * sd, -0.52f, -0.72f);
            if (c.swordRotX != 0) matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float) c.swordRotX));
            if (c.swordRotY != 0) matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) c.swordRotY * sd));
            if (c.swordRotZ != 0) matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) c.swordRotZ * sd));
            float ss = (float) c.swordScale;
            matrices.scale(ss, ss, ss);
            matrices.translate(-0.56f * sd, 0.52f, 0.72f);
        }
        if (!c.smallHands) return;

        Arm arm = hand == Hand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        float side = arm == Arm.RIGHT ? 1f : -1f;

        float s;
        float dx;
        float dy;
        float dz;
        float rx;
        float ry;
        float rz;
        if (arm == Arm.LEFT && c.leftSeparate) {
            // у левой руки свои настройки
            s = (float) c.leftScale;
            dx = (float) c.leftX;
            dy = (float) c.leftY;
            dz = (float) c.leftZ;
            rx = (float) c.leftRotX;
            ry = (float) c.leftRotY;
            rz = (float) c.leftRotZ;
        } else {
            // зеркально повторяем настройки правой руки
            s = (float) c.handScale;
            dx = (float) c.handX * side;
            dy = (float) c.handY;
            dz = (float) c.handZ;
            rx = (float) c.handRotX;
            ry = (float) c.handRotY * side;
            rz = (float) c.handRotZ * side;
        }

        matrices.translate(dx, dy, dz);
        matrices.translate(0.64f * side, -0.6f, -0.72f);
        if (rx != 0) matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rx));
        if (ry != 0) matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(ry));
        if (rz != 0) matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rz));
        matrices.scale(s, s, s);
        matrices.translate(-0.64f * side, 0.6f, 0.72f);
    }
}
