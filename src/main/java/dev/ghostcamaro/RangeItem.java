package dev.ghostcamaro;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.util.*;
import net.minecraft.util.hit.*;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.*;
/** Arcade test props. Server ray intersects the same local car sections as selection. */
public class RangeItem extends Item {
    final boolean repeat;RangeItem(boolean repeat){super(new Item.Settings().maxCount(1));this.repeat=repeat;}
    public TypedActionResult<ItemStack> use(World w,PlayerEntity p,Hand h){if(!w.isClient)fire((ServerWorld)w,p);if(repeat)p.setCurrentHand(h);return TypedActionResult.consume(p.getStackInHand(h));}
    public int getMaxUseTime(ItemStack s){return 72000;}
    public void usageTick(World w,LivingEntity living,ItemStack s,int left){if(repeat&&left<71999&&left%5==0&&!w.isClient&&living instanceof PlayerEntity p)fire((ServerWorld)w,p);}
    void fire(ServerWorld w,PlayerEntity p){
        Item ammo=repeat?GhostCamaro.RIFLE_AMMO:GhostCamaro.PISTOL_AMMO;
        if(p.getItemCooldownManager().isCoolingDown(this))return;
        if(!p.isCreative()){int i=p.getInventory().getSlotWithStack(new ItemStack(ammo));if(i<0)return;p.getInventory().removeStack(i,1);}
        p.getItemCooldownManager().set(this,repeat?4:8);
        Vec3d start=p.getEyePos(),dir=p.getRotationVec(1),far=start.add(dir.multiply(36));
        var block=w.raycast(new RaycastContext(start,far,RaycastContext.ShapeType.COLLIDER,RaycastContext.FluidHandling.NONE,p));Vec3d end=block.getPos(),best=end;GhostCamaroEntity chosen=null;
        for(var c:w.getEntitiesByClass(GhostCamaroEntity.class,new Box(start,end).expand(2),e->e!=p.getVehicle())){
            float rot=c.getYaw()*MathHelper.RADIANS_PER_DEGREE;
            var at=CarHull.select(start.subtract(c.getPos()).rotateY(rot),end.subtract(c.getPos()).rotateY(rot));
            if(at.isPresent()){Vec3d pos=c.getPos().add(at.get().rotateY(-rot));if(pos.squaredDistanceTo(start)<best.squaredDistanceTo(start)){chosen=c;best=pos;}}
        }
        if(chosen!=null)chosen.impact(best,repeat?GhostCamaro.CONFIG.rifleDamage:GhostCamaro.CONFIG.pistolDamage);
        if(chosen!=null||block.getType()!=HitResult.Type.MISS)w.spawnParticles(ParticleTypes.CRIT,best.x,best.y,best.z,5,.03,.03,.03,.06);
        Vec3d muzzle=start.add(dir.multiply(.65));w.spawnParticles(ParticleTypes.FLAME,muzzle.x,muzzle.y-.10,muzzle.z,2,.01,.01,.01,.015);
        w.playSound(null,p.getBlockPos(),SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST,SoundCategory.PLAYERS,.72f,repeat?1.45f:1.1f);
    }
}
