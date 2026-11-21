package dev.ghostcamaro.client.mixin;
import dev.ghostcamaro.GhostCamaroEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(HeldItemRenderer.class)
abstract class DrivingHands {
    @Inject(method="renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V",at=@At("HEAD"),cancellable=true)
    private void ghostcamaro$wheel(float dt,MatrixStack m,VertexConsumerProvider.Immediate v,ClientPlayerEntity player,int light,CallbackInfo ci){
        if(player.getVehicle() instanceof GhostCamaroEntity car&&car.driver(player))ci.cancel();
    }
}
