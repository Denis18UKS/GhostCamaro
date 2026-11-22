package dev.ghostcamaro;

import net.minecraft.entity.*;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.hit.*;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import net.minecraft.nbt.NbtCompound;

/** Short-lived arcade projectile. Collision is swept over each tick's travel. */
public final class RangeRound extends ThrownItemEntity {
    private float damage;
    public RangeRound(EntityType<? extends RangeRound> type,World world){super(type,world);}
    protected Item getDefaultItem(){return GhostCamaro.ROUND_MODEL;}
    void launch(LivingEntity owner,float amount){
        damage=amount;setOwner(owner);setNoGravity(true);
        var direction=owner.getRotationVec(1);
        var start=owner.getEyePos().add(direction.multiply(.12));
        setPosition(start.x,start.y,start.z);setVelocity(direction.multiply(2.8));
    }
    public void tick(){
        baseTick();var start=getPos();var end=start.add(getVelocity());
        if(getWorld() instanceof ServerWorld world){
            if(age>=20){discard();return;}
            var block=world.raycast(new RaycastContext(start,end,RaycastContext.ShapeType.COLLIDER,RaycastContext.FluidHandling.NONE,this));
            Vec3d closest=block.getPos();Entity target=null;var owner=getOwner();
            for(var e:world.getOtherEntities(this,new Box(start,end).expand(2),e->e!=owner&&(owner==null||e!=owner.getVehicle())&&(e instanceof LivingEntity||e instanceof GhostCamaroEntity))){
                java.util.Optional<Vec3d> hit;
                if(e instanceof GhostCamaroEntity car){
                    float yaw=car.getYaw()*MathHelper.RADIANS_PER_DEGREE;
                    hit=CarHull.select(start.subtract(car.getPos()).rotateY(yaw),closest.subtract(car.getPos()).rotateY(yaw))
                            .map(p->car.getPos().add(p.rotateY(-yaw)));
                }else hit=e.getBoundingBox().raycast(start,closest);
                if(hit.isPresent()&&hit.get().squaredDistanceTo(start)<closest.squaredDistanceTo(start)){closest=hit.get();target=e;}
            }
            if(target!=null||block.getType()!=HitResult.Type.MISS){
                if(target instanceof GhostCamaroEntity car)car.impact(closest,damage);
                else if(target!=null)target.damage(getDamageSources().thrown(this,owner),damage);
                world.spawnParticles(ParticleTypes.CRIT,closest.x,closest.y,closest.z,5,.03,.03,.03,.05);
                discard();return;
            }
        }
        setPosition(end.x,end.y,end.z);
    }
    public void writeCustomDataToNbt(NbtCompound n){super.writeCustomDataToNbt(n);n.putFloat("Impact",damage);}
    public void readCustomDataFromNbt(NbtCompound n){super.readCustomDataFromNbt(n);damage=n.getFloat("Impact");}
}
