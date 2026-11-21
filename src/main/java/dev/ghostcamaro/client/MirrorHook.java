package dev.ghostcamaro.client.mixin;
import dev.ghostcamaro.client.*;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GameRenderer.class)
abstract class MirrorHook {
    @Inject(method="renderWorld",at=@At("HEAD"))
    private void ghostcamaro$mirrors(float delta,long limit,MatrixStack mats,CallbackInfo ci){MirrorSystem.update(delta);}
}
