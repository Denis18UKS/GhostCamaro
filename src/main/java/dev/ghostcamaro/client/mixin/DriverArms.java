package dev.ghostcamaro.client.mixin;
import dev.ghostcamaro.GhostCamaroEntity;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(PlayerEntityModel.class)
abstract class DriverArms {
    @Inject(method="setAngles(Lnet/minecraft/entity/LivingEntity;FFFFF)V",at=@At("TAIL"))
    private void ghostcamaro$grip(LivingEntity rider,float limb,float distance,float age,float yaw,float pitch,CallbackInfo ci){
        if(!(rider.getVehicle() instanceof GhostCamaroEntity car)||!car.driver(rider))return;
        PlayerEntityModel<?> m=(PlayerEntityModel<?>)(Object)this;
        float turn=car.steering()*.24f;
        m.leftArm.pitch=-1.14f+turn;m.rightArm.pitch=-1.14f-turn;
        m.leftArm.yaw=.21f;m.rightArm.yaw=-.21f;
        m.leftSleeve.copyTransform(m.leftArm);m.rightSleeve.copyTransform(m.rightArm);
    }
}
