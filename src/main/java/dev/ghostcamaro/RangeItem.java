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
    public int capacity(){return repeat?24:8;}
    public int loaded(ItemStack stack){return stack.hasNbt()?MathHelper.clamp(stack.getNbt().getInt("Rounds"),0,capacity()):0;}
    public boolean automatic(){return repeat;}
    public TypedActionResult<ItemStack> use(World world,PlayerEntity player,Hand hand){
        player.setCurrentHand(hand);return TypedActionResult.consume(player.getStackInHand(hand));
    }
    public int getMaxUseTime(ItemStack stack){return 72000;}
    public void reload(PlayerEntity player,ItemStack stack){
        var n=stack.getOrCreateNbt();if(n.contains("ReloadEnd")||loaded(stack)==capacity())return;
        if(!player.isCreative()&&!player.getInventory().contains(new ItemStack(repeat?GhostCamaro.RIFLE_AMMO:GhostCamaro.PISTOL_AMMO)))return;
        n.putLong("ReloadEnd",player.getWorld().getTime()+30);
    }
    public void inventoryTick(ItemStack stack,World world,net.minecraft.entity.Entity entity,int slot,boolean selected){
        if(world.isClient||!(entity instanceof PlayerEntity p)||!stack.hasNbt()||!stack.getNbt().contains("ReloadEnd"))return;
        var n=stack.getNbt();if(!selected){n.remove("ReloadEnd");return;}
        if(world.getTime()<n.getLong("ReloadEnd"))return;
        n.remove("ReloadEnd");int previous=loaded(stack),need=capacity()-previous,added=0;
        var ammo=repeat?GhostCamaro.RIFLE_AMMO:GhostCamaro.PISTOL_AMMO;
        if(p.isCreative())added=need;
        else for(int i=0;i<p.getInventory().size()&&added<need;i++){
            var spare=p.getInventory().getStack(i);if(spare.isOf(ammo)){int take=Math.min(spare.getCount(),need-added);spare.decrement(take);added+=take;}
        }
        n.putInt("Rounds",previous+added);p.getInventory().markDirty();
    }
    void fire(ServerWorld w,PlayerEntity p){
        var stack=p.getMainHandStack();if(!stack.isOf(this)||p.isSpectator()||p.getItemCooldownManager().isCoolingDown(this))return;
        if(stack.hasNbt()&&stack.getNbt().contains("ReloadEnd"))return;
        if(!p.isCreative()&&loaded(stack)==0)return;
        if(!p.isCreative())stack.getOrCreateNbt().putInt("Rounds",loaded(stack)-1);
        stack.getOrCreateNbt().putLong("LastFire",w.getTime());
        p.getItemCooldownManager().set(this,repeat?4:8);
        var round=new RangeRound(GhostCamaro.ROUND,w);round.launch(p,repeat?GhostCamaro.CONFIG.rifleDamage:GhostCamaro.CONFIG.pistolDamage);w.spawnEntity(round);
        Vec3d start=p.getEyePos(),dir=p.getRotationVec(1);
        Vec3d muzzle=start.add(dir.multiply(.65));w.spawnParticles(ParticleTypes.FLAME,muzzle.x,muzzle.y-.10,muzzle.z,2,.01,.01,.01,.015);
        w.playSound(null,p.getBlockPos(),SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST,SoundCategory.PLAYERS,.72f,repeat?1.45f:1.1f);
    }
}
