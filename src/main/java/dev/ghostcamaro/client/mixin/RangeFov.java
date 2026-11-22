package dev.ghostcamaro.client.mixin;
import dev.ghostcamaro.client.CamaroClient;
import net.minecraft.client.render.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(GameRenderer.class)
abstract class RangeFov {
    @Inject(method="getFov",at=@At("RETURN"),cancellable=true)
    private void focus(Camera cam,float dt,boolean changing,CallbackInfoReturnable<Double> result){
        if(changing&&CamaroClient.aim>0&&CamaroClient.mc().options.getPerspective().isFirstPerson())
            result.setReturnValue(result.getReturnValue()*(1-.18*net.minecraft.util.math.MathHelper.lerp(dt,CamaroClient.previousAim,CamaroClient.aim)));
    }
}
