package dev.ghostcamaro;
import net.minecraft.entity.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.*;
import net.minecraft.entity.player.*;
import net.minecraft.fluid.FluidState;
import net.minecraft.inventory.*;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.screen.*;
import net.minecraft.sound.*;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import java.util.*;
public class GhostCamaroEntity extends Entity {
    private static final TrackedData<Integer> FLAGS=DataTracker.registerData(GhostCamaroEntity.class,TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> SPEED=floating(), FUEL=floating(), HP=floating(), STEER=floating(), ROLL=floating();
    private static TrackedData<Float> floating(){return DataTracker.registerData(GhostCamaroEntity.class,TrackedDataHandlerRegistry.FLOAT);}
    private static final TrackedData<NbtCompound> MIRRORS=DataTracker.registerData(GhostCamaroEntity.class,TrackedDataHandlerRegistry.NBT_COMPOUND);
    private static final TrackedData<Optional<UUID>> DRIVER=DataTracker.registerData(GhostCamaroEntity.class,TrackedDataHandlerRegistry.OPTIONAL_UUID);
    private static final TrackedData<Integer> FAILURE=DataTracker.registerData(GhostCamaroEntity.class,TrackedDataHandlerRegistry.INTEGER);
    public static final int MOTOR=1,LIGHTS=2,LEFT=4,RIGHT=8,HAZARD=16,STOP=32,DOOR_L=64,DOOR_R=128,HOOD=256,TRUNK=512,WIN_L=1024,WIN_R=2048,WIPER=4096,PARK=8192,THROTTLE=16384,GLOVEBOX=32768;
    public final CargoInventory cargo=new CargoInventory();
    public final class CargoInventory extends SimpleInventory {
        private boolean batching; CargoInventory(){super(27);}
        void replace(SimpleInventory next){batching=true;try{for(int i=0;i<size();i++)setStack(i,next.getStack(i));}finally{batching=false;}markDirty();}
        public void markDirty(){if(!batching)super.markDirty();}
        @Override public boolean canPlayerUse(PlayerEntity p){return isAlive()&&p.squaredDistanceTo(GhostCamaroEntity.this)<36&&flag(TRUNK)&&Math.abs(speed())<.012;}}
    private int input,lastInput=-100,lastAction=-1,startTicks,interpolation;
    private double tx,ty,tz; private float ta;
    private boolean dropped;
    public final float[] openings=new float[7];public float lastRoll; public int lastGear,shiftAge=-100; public int gear(){return speed()<-.01?-1:(int)Math.min(4,1+Math.floor(Math.abs(speed())/.23));}
    public GhostCamaroEntity(EntityType<? extends GhostCamaroEntity> type,World w){super(type,w);setStepHeight(.66f);}
    protected void initDataTracker(){dataTracker.startTracking(FLAGS,0);dataTracker.startTracking(SPEED,0f);dataTracker.startTracking(FUEL,GhostCamaro.CONFIG.tank);
        dataTracker.startTracking(HP,100f);dataTracker.startTracking(STEER,0f);dataTracker.startTracking(ROLL,0f);dataTracker.startTracking(FAILURE,0);dataTracker.startTracking(DRIVER,Optional.empty());dataTracker.startTracking(MIRRORS,new NbtCompound());}
    public boolean flag(int f){return (dataTracker.get(FLAGS)&f)!=0;}
    private void set(int f,boolean on){int old=dataTracker.get(FLAGS);dataTracker.set(FLAGS,on?old|f:old&~f);}
    public float speed(){return dataTracker.get(SPEED);}public float fuel(){return dataTracker.get(FUEL);}public float health(){return dataTracker.get(HP);}
    public float steering(){return dataTracker.get(STEER);}public float roll(){return dataTracker.get(ROLL);}
    public int failure(){return dataTracker.get(FAILURE);}public boolean driver(Entity p){return dataTracker.get(DRIVER).filter(p.getUuid()::equals).isPresent();}
    public float mirror(int i,int axis){return dataTracker.get(MIRRORS).getFloat("m"+i+axis);}
    public String status(){return String.format(Locale.ROOT,"%.0f km/h | %.1f L | %.0f%% | %d rpm",Math.abs(speed()*72),fuel(),health(),rpm());}
    public int rpm(){return flag(MOTOR)?(int)(800+Math.abs(speed())*3200):0;}
    public void repair(){dataTracker.set(HP,100f);dataTracker.set(FAILURE,0);}
    public void refuel(){dataTracker.set(FUEL,GhostCamaro.CONFIG.tank);}
    public void disableEngine(){set(MOTOR,false);startTicks=0;}
    public boolean canHit(){return true;}public boolean isCollidable(){return true;} public boolean isPushable(){return false;}
    public Vec3d offset(double left,double up,double front){return new Vec3d(left,up,front).rotateY(-getYaw()*MathHelper.RADIANS_PER_DEGREE);}
    protected Box calculateBoundingBox(){
        double a=Math.toRadians(getYaw()),x=Math.abs(Math.cos(a))*.98+Math.abs(Math.sin(a))*2.47,z=Math.abs(Math.sin(a))*.98+Math.abs(Math.cos(a))*2.47;
        return new Box(getX()-x,getY(),getZ()-z,getX()+x,getY()+1.30,getZ()+z);
    }
    protected boolean canAddPassenger(Entity e){return e instanceof PlayerEntity&&getPassengerList().size()<2;}
    protected void updatePassengerPosition(Entity e,PositionUpdater u){Vec3d p=getPos().add(offset(driver(e)?.398:-.398,.10,-.15));u.accept(e,p.x,p.y,p.z);}
    protected void removePassenger(Entity e){super.removePassenger(e);if(driver(e)&&!getWorld().isClient){dataTracker.set(DRIVER,Optional.empty());input=0;}}
    public Vec3d updatePassengerForDismount(LivingEntity e){
        for(double lateral:new double[]{driver(e)?1.55:-1.55,driver(e)?-1.55:1.55,2.5,-2.5})for(double fore:new double[]{0,1,-1,2.9,-2.9})for(int y:new int[]{0,1,-1}){
            Vec3d p=getPos().add(offset(lateral,y,fore));BlockPos b=BlockPos.ofFloored(p);Box bb=e.getDimensions(EntityPose.STANDING).getBoxAt(p);
            if(!getBoundingBox().intersects(bb)&&getWorld().isSpaceEmpty(e,bb)&&!getWorld().getFluidState(b).isIn(FluidTags.LAVA)&&getWorld().getBlockState(b.down()).isSolidBlock(getWorld(),b.down()))return p;
        }return super.updatePassengerForDismount(e);
    }
    public ActionResult interactAt(PlayerEntity p,Vec3d hit,Hand hand){
        if(hand!=Hand.MAIN_HAND)return ActionResult.PASS;
        if(getWorld().isClient)return ActionResult.SUCCESS;
        ItemStack stack=p.getStackInHand(hand);
        if(stack.isOf(GhostCamaro.CAN)||stack.isOf(GhostCamaro.KIT)){
            if(Math.abs(speed())>.012)return ActionResult.FAIL;
            if(stack.isOf(GhostCamaro.CAN)){if(fuel()>=GhostCamaro.CONFIG.tank)return ActionResult.FAIL;refuel();}else{if(health()>=100)return ActionResult.FAIL;repair();}
            if(!p.getAbilities().creativeMode)stack.decrement(1);return ActionResult.CONSUME;
        }
        if(Math.abs(speed())>.04)return ActionResult.FAIL;
        Vec3d local=hit.rotateY(getYaw()*MathHelper.RADIANS_PER_DEGREE);
        if(local.z < -1.58){if(p.isSneaking()||!flag(TRUNK))toggle(TRUNK);
            if(flag(TRUNK))p.openHandledScreen(new SimpleNamedScreenHandlerFactory((id,inv,who)->GenericContainerScreenHandler.createGeneric9x3(id,inv,cargo),Text.translatable("container.ghostcamaro.trunk")));
        }else if(local.z>1.05){toggle(HOOD);}
        else if(p.isSneaking()){toggle(local.x>=0?DOOR_L:DOOR_R);}
        else {boolean left=local.x>=0;
            if(left&&getPassengerList().stream().anyMatch(this::driver)||!left&&getPassengerList().stream().anyMatch(e->!driver(e)))return ActionResult.FAIL;
            if(!p.startRiding(this))return ActionResult.FAIL;
            if(left)dataTracker.set(DRIVER,Optional.of(p.getUuid()));p.setYaw(getYaw());}
        return ActionResult.CONSUME;
    }
    private void toggle(int f){set(f,!flag(f));if(f>=DOOR_L&&f<=TRUNK)playSound(SoundEvents.BLOCK_IRON_TRAPDOOR_CLOSE,.45f,flag(f)?.85f:1.1f);}
    public void input(PlayerEntity p,int mask,int a,int m,float yaw,float pitch){
        if(!driver(p))return;input=mask&31;lastInput=age;
        if(a==0||age==lastAction)return;lastAction=age;
        if(a==1){if(flag(MOTOR)||startTicks>0)disableEngine();else if(fuel()>0&&failure()==0){startTicks=10;playSound(GhostCamaro.START,.8f,1);}}
        if(a>=2&&a<=5){int f=new int[]{LIGHTS,LEFT,RIGHT,HAZARD}[a-2];toggle(f);if(f==LEFT)set(RIGHT,false);if(f==RIGHT)set(LEFT,false);}
        if(a==6)playSound(GhostCamaro.HORN,1,1);if(a==7)toggle(WIPER);
        if(a>=8&&a<=13){int f=new int[]{WIN_L,WIN_R,DOOR_L,DOOR_R,HOOD,TRUNK}[a-8];if(a<10||Math.abs(speed())<.012)toggle(f);}
        if(a==15)toggle(GLOVEBOX);
        if(a==14&&m<3&&Float.isFinite(yaw)&&Float.isFinite(pitch)){NbtCompound n=dataTracker.get(MIRRORS).copy();n.putFloat("m"+m+"0",MathHelper.clamp(yaw,-28,28));n.putFloat("m"+m+"1",MathHelper.clamp(pitch,-18,18));dataTracker.set(MIRRORS,n);}
    }
    public void tick(){
        super.tick();
        int gear=gear();if(gear!=lastGear){shiftAge=age;lastGear=gear;}
        int[] bits={DOOR_L,DOOR_R,HOOD,TRUNK,WIN_L,WIN_R,GLOVEBOX};
        for(int i=0;i<bits.length;i++)openings[i]=MathHelper.stepTowards(openings[i],flag(bits[i])?1:0,i==4||i==5?.042f:.065f);
        if(getWorld().isClient){if(interpolation>0){setPosition(getX()+(tx-getX())/interpolation,getY()+(ty-getY())/interpolation,getZ()+(tz-getZ())/interpolation);setYaw(getYaw()+MathHelper.wrapDegrees(ta-getYaw())/interpolation);interpolation--;}return;}
        if(age-lastInput>7||getPassengerList().stream().noneMatch(this::driver))input=0;
        if(startTicks>0&&--startTicks==0&&fuel()>0&&failure()==0)set(MOTOR,true);
        double v=speed(),before=v;Settings c=GhostCamaro.CONFIG;boolean gas=(input&1)!=0,back=(input&2)!=0,park=(input&16)!=0;
        float turn=MathHelper.stepTowards(steering(),((input&4)!=0?1:0)-((input&8)!=0?1:0),.12f);dataTracker.set(STEER,turn);
        boolean braking=back&&v>.004||park;set(STOP,braking);set(PARK,park);set(THROTTLE,gas&&flag(MOTOR));
        double grip=MathHelper.clamp(.6/getWorld().getBlockState(getBlockPos().down()).getBlock().getSlipperiness(),.45,1.25);
        if(park||back&&v>0)v=MathHelper.stepTowards((float)v,0,(float)((park?c.emergencyBrake:c.normalBrake)*grip));
        else if(flag(MOTOR)&&(gas||back))v+=c.acceleration*(gas?1:-.52)*(health()<45?.37:1)*grip;
        else v=MathHelper.stepTowards((float)v,0,.0018f);
        v=MathHelper.clamp(v,-.27,c.maxSpeed);
        if(flag(TRUNK)||flag(HOOD))v=0;
        float yaw=getYaw();setYaw(yaw-(float)(Math.toDegrees(v/2.74*Math.tan(Math.toRadians(29*turn/(1+1.6*Math.abs(v)))))));
        Box rotated=calculateBoundingBox();if(!getWorld().isSpaceEmpty(this,rotated))setYaw(yaw);setBoundingBox(calculateBoundingBox());
        Vec3d delta=offset(0,0,v);double xx=getX(),zz=getZ();
        double vy=Math.max(-1.2,getVelocity().y-.08);move(MovementType.SELF,new Vec3d(delta.x,vy,delta.z));setVelocity(0,isOnGround()?0:vy*.98,0);
        double travel=Math.hypot(getX()-xx,getZ()-zz);if(horizontalCollision&&travel<Math.abs(v)*.65){if(Math.abs(before)>.15)hurtCar((float)(Math.abs(before)*c.collisionDamage));v=0;}
        dataTracker.set(SPEED,(float)v);dataTracker.set(ROLL,(float)((roll()+Math.copySign(travel,v==0?before:v)/.345)%(Math.PI*2)));
        if(flag(MOTOR)){dataTracker.set(FUEL,Math.max(0,fuel()-c.idleConsumption-(float)Math.abs(v)*c.driveConsumption));if(fuel()==0)disableEngine();}
        if((flag(LEFT)||flag(RIGHT)||flag(HAZARD))&&age%14==0)playSound(GhostCamaro.CLICK,.26f,1);
        if(failure()>0&&failure()<182){int f=failure()+1;dataTracker.set(FAILURE,f);if(f==143){playSound(GhostCamaro.BURST,1.25f,1);set(HOOD,true);}}
        if(flag(MOTOR)&&age%12==0)playSound(GhostCamaro.ENGINE,.55f,.8f+Math.abs(speed())*1.6f);
    }
    public void updateTrackedPositionAndAngles(double x,double y,double z,float a,float p,int steps,boolean interp){tx=x;ty=y;tz=z;ta=a;interpolation=3;}
    public void hurtCar(float amount){if(getWorld().isClient||amount<=0||!Float.isFinite(amount))return;dataTracker.set(HP,Math.max(0,health()-amount));if(health()<=10&&failure()==0){disableEngine();dataTracker.set(FAILURE,1);}}
    public boolean damage(DamageSource source,float amount){if(isInvulnerableTo(source))return false;hurtCar(amount);return true;}
    public void remove(RemovalReason reason){if(!getWorld().isClient&&!dropped&&reason.shouldDestroy()){dropped=true;ItemScatterer.spawn(getWorld(),this,cargo);cargo.clear();}super.remove(reason);}
    protected void writeCustomDataToNbt(NbtCompound n){n.putInt("Flags",dataTracker.get(FLAGS));n.putFloat("Fuel",fuel());n.putFloat("Health",health());n.putFloat("Speed",speed());n.putInt("Failure",failure());n.put("Mirrors",dataTracker.get(MIRRORS).copy());dataTracker.get(DRIVER).ifPresent(u->n.putUuid("Driver",u));
        DefaultedList<ItemStack> l=DefaultedList.ofSize(27,ItemStack.EMPTY);for(int i=0;i<27;i++)l.set(i,cargo.getStack(i));Inventories.writeNbt(n,l);}
    protected void readCustomDataFromNbt(NbtCompound n){dataTracker.set(FLAGS,n.getInt("Flags"));if(n.contains("Fuel"))dataTracker.set(FUEL,n.getFloat("Fuel"));if(n.contains("Health"))dataTracker.set(HP,n.getFloat("Health"));dataTracker.set(SPEED,n.getFloat("Speed"));dataTracker.set(FAILURE,n.getInt("Failure"));dataTracker.set(MIRRORS,n.getCompound("Mirrors").copy());if(n.containsUuid("Driver"))dataTracker.set(DRIVER,Optional.of(n.getUuid("Driver")));
        DefaultedList<ItemStack> l=DefaultedList.ofSize(27,ItemStack.EMPTY);Inventories.readNbt(n,l);for(int i=0;i<27;i++)cargo.setStack(i,l.get(i));}
}
