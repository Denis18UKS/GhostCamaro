package dev.ghostcamaro.client.mixin;
import dev.ghostcamaro.client.*;
import dev.ghostcamaro.*;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.*;
import net.minecraft.util.math.*;
import net.minecraft.util.hit.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Camera.class)
abstract class VehicleCamera {
    @Shadow protected abstract void setPos(Vec3d p);
    @Shadow protected abstract void setRotation(float yaw,float pitch);
    @Inject(method="update",at=@At("RETURN"))
    private void ghostcamaro$camera(BlockView area,Entity e,boolean third,boolean inverse,float t,CallbackInfo ci){
        if(e!=CamaroClient.mc().player||!(e.getVehicle() instanceof GhostCamaroEntity c))return;
        double left=c.driver(e)?.398:-.398;
        Vec3d pivot=c.getLerpedPos(t).add(c.offset(left,CamaroClient.config.eyeHeight,-.18));
        float yaw=e.getYaw(t),pitch=e.getPitch(t);
        if(third){Vec3d center=c.getLerpedPos(t).add(0,.94,0);double dist=MathHelper.clamp(CamaroClient.config.chaseDistance,2.5,10)+Math.abs(c.speed());if(inverse){yaw+=180;pitch=-pitch;}
            Vec3d end=center.subtract(Vec3d.fromPolar(pitch,yaw).multiply(dist)).add(0,1.05,0);var hit=c.getWorld().raycast(new RaycastContext(center,end,RaycastContext.ShapeType.VISUAL,RaycastContext.FluidHandling.NONE,e));
            setPos(hit.getType()==HitResult.Type.MISS?end:hit.getPos().lerp(center,.08));setRotation(yaw,pitch+7);}
        else if(CamaroClient.bonnet){setPos(c.getLerpedPos(t).add(c.offset(0,.99,1.32)));setRotation(yaw,pitch);}
        else {setPos(pivot);if(CamaroClient.glance>=0&&CamaroClient.glanceBlend>0){Vec3d dir=MirrorSystem.center(c,CamaroClient.glance,t).subtract(pivot);float a=(float)Math.toDegrees(Math.atan2(-dir.x,dir.z)),b=(float)-Math.toDegrees(Math.atan2(dir.y,dir.horizontalLength()));setRotation(MathHelper.lerpAngleDegrees(CamaroClient.glanceBlend,yaw,a),MathHelper.lerp(CamaroClient.glanceBlend,pitch,b));}}
    }
}
