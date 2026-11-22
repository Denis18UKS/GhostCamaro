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
    @Inject(method="renderFirstPersonItem",at=@At("HEAD"))
    private void ghostcamaro$pose(net.minecraft.client.network.AbstractClientPlayerEntity p,float dt,float pitch,net.minecraft.util.Hand hand,float swing,net.minecraft.item.ItemStack stack,float equip,MatrixStack m,VertexConsumerProvider v,int light,CallbackInfo ci){
        m.push();
        if(stack.getItem() instanceof dev.ghostcamaro.RangeItem){
            float aim=net.minecraft.util.math.MathHelper.lerp(dt,dev.ghostcamaro.client.CamaroClient.previousAim,dev.ghostcamaro.client.CamaroClient.aim);
            float side=(hand==net.minecraft.util.Hand.MAIN_HAND)==(p.getMainArm()==net.minecraft.util.Arm.RIGHT)?1:-1;
            m.translate(-side*.55*aim,.17*aim,-.13*aim);
            if(stack.hasNbt()){
                var n=stack.getNbt();float time=p.getWorld().getTime()+dt;
                if(n.contains("ReloadEnd")){
                    float frac=net.minecraft.util.math.MathHelper.clamp((n.getLong("ReloadEnd")-time)/30f,0,1);
                    m.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(side*46*(float)Math.sin(Math.PI*frac)));
                }
                if(n.contains("LastFire")){
                    float kick=Math.max(0,1-(time-n.getLong("LastFire"))/4f);
                    m.translate(0,0,.10*kick);m.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(-8*kick));
                }
            }
        }
    }
    @Inject(method="renderFirstPersonItem",at=@At("RETURN"))
    private void ghostcamaro$endPose(net.minecraft.client.network.AbstractClientPlayerEntity p,float dt,float pitch,net.minecraft.util.Hand h,float swing,net.minecraft.item.ItemStack stack,float equip,MatrixStack m,VertexConsumerProvider v,int light,CallbackInfo ci){m.pop();}
    @Inject(method="renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V",at=@At("HEAD"),cancellable=true)
    private void ghostcamaro$wheel(float dt,MatrixStack m,VertexConsumerProvider.Immediate v,ClientPlayerEntity player,int light,CallbackInfo ci){
        if(player.getVehicle() instanceof GhostCamaroEntity car&&car.driver(player))ci.cancel();
    }
}
