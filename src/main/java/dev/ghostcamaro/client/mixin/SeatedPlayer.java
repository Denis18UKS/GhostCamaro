package dev.ghostcamaro.client.mixin;
import dev.ghostcamaro.GhostCamaroEntity;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(PlayerEntityRenderer.class)
abstract class SeatedPlayer {
    @Inject(method="scale(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/client/util/math/MatrixStack;F)V",at=@At("TAIL"))
    private void ghostcamaro$seat(AbstractClientPlayerEntity player,MatrixStack matrices,float tick,CallbackInfo ci){
        if(player.getVehicle() instanceof GhostCamaroEntity)matrices.scale(.70f,.63f,.70f);
    }
}
