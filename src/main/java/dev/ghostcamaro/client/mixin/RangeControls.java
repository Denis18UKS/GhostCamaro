package dev.ghostcamaro.client.mixin;
import dev.ghostcamaro.RangeItem;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(MinecraftClient.class)
abstract class RangeControls {
    private boolean holding(){var p=MinecraftClient.getInstance().player;return p!=null&&p.getMainHandStack().getItem() instanceof RangeItem;}
    @Inject(method="doAttack",at=@At("HEAD"),cancellable=true)
    private void click(CallbackInfoReturnable<Boolean> ci){if(holding())ci.setReturnValue(false);}
    @Inject(method="handleBlockBreaking",at=@At("HEAD"),cancellable=true)
    private void held(boolean breaking,CallbackInfo ci){if(holding())ci.cancel();}
}
