package dev.ghostcamaro.mixin;
import dev.ghostcamaro.*;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
abstract class HullMovement {
    @Shadow private Vec3d adjustMovementForCollisions(Vec3d movement){throw new AssertionError();}
    @Unique private boolean ghostcamaro$checking;
    @Inject(method="adjustMovementForCollisions(Lnet/minecraft/util/math/Vec3d;)Lnet/minecraft/util/math/Vec3d;",at=@At("HEAD"),cancellable=true)
    private void ghostcamaro$hull(Vec3d request,CallbackInfoReturnable<Vec3d> cir){
        if(ghostcamaro$checking || !((Object)this instanceof GhostCamaroEntity car))return;
        Box broad=car.getBoundingBox();ghostcamaro$checking=true;
        try{
            Vec3d delta=request;
            for(Box part:CarHull.pieces(car)){
                car.setBoundingBox(part);
                Vec3d allowed=adjustMovementForCollisions(delta);
                if(allowed.y>delta.y+1e-7){
                    // A step must have headroom over the whole car.
                    boolean fits=true;
                    for(Box b:CarHull.pieces(car))if(!car.getWorld().isSpaceEmpty(car,b.offset(allowed))){fits=false;break;}
                    if(!fits)allowed=new Vec3d(0,delta.y,0);
                }
                delta=allowed;
            }
            cir.setReturnValue(delta);
        }finally{car.setBoundingBox(broad);ghostcamaro$checking=false;}
    }
}
