package com.example.hitvisuals.mixin;

import com.example.hitvisuals.VisualsConfig;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Положение, размер и поворот руки от первого лица, стиль взмаха. */
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
        if (!c.smallHands) return;
        float s = (float) c.handScale;
        Arm arm = hand == Hand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        float side = arm == Arm.RIGHT ? 1f : -1f;

        // сдвиг руки
        matrices.translate((float) c.handX * side, (float) c.handY, (float) c.handZ);

        // поворот и размер вокруг того места, где рука обычно находится
        matrices.translate(0.64f * side, -0.6f, -0.72f);
        if (c.handRotX != 0) matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float) c.handRotX));
        if (c.handRotY != 0) matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) c.handRotY * side));
        if (c.handRotZ != 0) matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) c.handRotZ * side));
        matrices.scale(s, s, s);
        matrices.translate(-0.64f * side, 0.6f, 0.72f);
    }
}
