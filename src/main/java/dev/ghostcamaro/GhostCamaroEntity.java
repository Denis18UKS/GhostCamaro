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
    private static final TrackedData<Integer> LOAD=DataTracker.registerData(GhostCamaroEntity.class,TrackedDataHandlerRegistry.INTEGER);
    public int cargoDisplay(){return dataTracker.get(LOAD);}
    public final CargoInventory cargo=new CargoInventory();
    public final class CargoInventory extends SimpleInventory {
        private boolean batching; CargoInventory(){super(27);}
        void replace(SimpleInventory next){batching=true;try{for(int i=0;i<size();i++)setStack(i,next.getStack(i));}finally{batching=false;}markDirty();}
        public void markDirty(){if(!batching){if(!getWorld().isClient)dataTracker.set(LOAD,CargoDisplay.summarize(this));super.markDirty();}}
        @Override public ItemStack removeStack(int slot){ItemStack result=super.removeStack(slot);if(!result.isEmpty())markDirty();return result;}
        @Override public boolean canPlayerUse(PlayerEntity p){return isAlive()&&p.squaredDistanceTo(GhostCamaroEntity.this)<36&&flag(TRUNK)&&Math.abs(speed())<.012;}}
    static final int CAP=65536, FILLING=131072;
    private static final TrackedData<Integer> MODULES=DataTracker.registerData(GhostCamaroEntity.class,TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<NbtCompound> MARKS=DataTracker.registerData(GhostCamaroEntity.class,TrackedDataHandlerRegistry.NBT_COMPOUND);
    private static final TrackedData<Integer> SERVICE=DataTracker.registerData(GhostCamaroEntity.class,TrackedDataHandlerRegistry.INTEGER);
    public float capPhase;private UUID fillingPlayer;private BlockPos fillingPump;private Hand fillingHand;
    public boolean hasModule(int i){return (dataTracker.get(MODULES)&(1<<i))!=0;}
    public int level(){return Integer.bitCount(dataTracker.get(MODULES));}
    public void module(int i,boolean on){if(i<0||i>2)return;int mask=dataTracker.get(MODULES);dataTracker.set(MODULES,on?mask|1<<i:mask&~(1<<i));service(on?1:2);}
    public String buildStats(){return String.format(Locale.ROOT,"Lv %d | +%d kg | %.0f%% accel | +%d%% fuel",level(),level()*85,100/(1+.09*level()),level()*7);}
    public NbtCompound damageState(){return dataTracker.get(MARKS);}
    public NbtList marks(){return dataTracker.get(MARKS).getList("List",10);}
    public boolean damaged(String zone){return dataTracker.get(MARKS).getBoolean(zone);}
    void storeMarks(NbtList list){var state=new NbtCompound();state.put("List",list);for(int i=0;i<list.size();i++)state.putBoolean(list.getCompound(i).getString("Zone"),true);dataTracker.set(MARKS,state);}
    public static boolean isPane(String zone){return zone.equals("Windshield")||zone.equals("Window_L")||zone.equals("Window_R");}
    public int glassGrade(){return hasModule(1)?2:hasModule(0)?1:0;}
    public int glassStage(String zone){
        var panel=damageState().getCompound("Panes").getCompound(zone);
        return panel.contains("Stage")?panel.getInt("Stage"):damaged(zone)?(zone.startsWith("Window")?5:2):0;
    }
    public boolean laminated(String zone){return damageState().getCompound("Panes").getCompound(zone).getBoolean("Held");}
    public void repairPane(String zone){
        var state=damageState().copy();var list=state.getList("List",10);
        list.removeIf(e->zone.equals(((NbtCompound)e).getString("Zone")));
        state.getCompound("Panes").remove(zone);state.remove(zone);dataTracker.set(MARKS,state);
    }
    private void recordImpact(String zone,Vec3d q,float force){
        NbtCompound state=damageState().copy(),mark=new NbtCompound();
        mark.putString("Zone",zone);mark.putFloat("X",(float)q.x);mark.putFloat("Y",(float)q.y);mark.putFloat("Z",(float)q.z);
        if(isPane(zone)){
            var panes=state.getCompound("Panes");var panel=panes.getCompound(zone);
            int grade=glassGrade();
            float threshold=Math.max(1,switch(grade){
                case 2->GhostCamaro.CONFIG.laminatedGlassStep;
                case 1->GhostCamaro.CONFIG.reinforcedGlassStep;
                default->GhostCamaro.CONFIG.standardGlassStep;});
            float progress=Math.min(5,(panel.contains("Progress")?panel.getFloat("Progress"):glassStage(zone))+Math.min(1.6f,Math.max(.01f,force)/threshold));
            int stage=Math.min(5,(int)Math.ceil(progress));
            if(glassStage(zone)<5&&stage==5){
                panel.putLong("BreakTick",getWorld().getTime());panel.putBoolean("Held",grade==2);
            }
            panel.putInt("Stage",stage);panel.putFloat("Progress",progress);
            panel.put("Contact",mark.copy());panes.put(zone,panel);state.put("Panes",panes);
        }
        var list=state.getList("List",10);
        if(list.size()>=24)list.remove(0);
        list.add(mark);state.put("List",list);state.putBoolean(zone,true);dataTracker.set(MARKS,state);
    }
    void collisionDamage(double speed){
        boolean front=speed>0;double z=front?2.58:-2.38;
        Vec3d contact=getPos().add(offset(0,.55,z));
        impact(contact,(float)(Math.abs(speed)*GhostCamaro.CONFIG.collisionDamage));
        if(front && Math.abs(speed)>.32)recordImpact("Hood",new Vec3d(0,.78,1.72),12);
        if(front && Math.abs(speed)>.56&&!hasModule(1))recordImpact("Windshield",new Vec3d(.15,1,.35),12);
        if(getWorld() instanceof net.minecraft.server.world.ServerWorld sw)
            sw.spawnParticles(net.minecraft.particle.ParticleTypes.ELECTRIC_SPARK,contact.x,contact.y,contact.z,8,.2,.1,.2,.04);
    }
    public void impact(Vec3d pos,float damage){
        if(getWorld().isClient)return;Vec3d q=pos.subtract(getPos()).rotateY(getYaw()*MathHelper.RADIANS_PER_DEGREE);
        String side=q.x>=0?"L":"R",zone="Body";double lateral=Math.abs(q.x);
        if(q.z>1.9&&lateral>.53&&q.y>.40)zone="Lamp_"+side;
        else if(lateral>.65&&q.y<.62&&(Math.abs(q.z-1.46)<.43||Math.abs(q.z+1.28)<.43))zone="Tire_"+(q.z>0?"F":"R")+side;
        else if(lateral>.80&&Math.abs(q.z-.43)<.16&&q.y>.82)zone="Mirror_"+side;
        else if(q.y>.84&&q.z>0&&q.z<.7&&lateral<.70)zone="Windshield";
        else if(q.y>.82&&lateral>.60&&q.z>-.95&&q.z<.55)zone="Window_"+side;
        else if(q.z>2.1||q.z< -2.15)zone="Bumper";
        else if(q.z>.62&&q.z<2.2&&q.y>.60)zone="Hood";
        else if(hasModule(0)&&lateral>.6)zone="Armor";
        boolean protectedZone=hasModule(1)&&(zone.equals("Windshield")||zone.startsWith("Window")||zone.equals("Hood"))
            ||hasModule(2)&&(zone.startsWith("Tire")||zone.equals("Bumper"))||zone.equals("Armor");
        if(protectedZone && damage<6 && zone.startsWith("Tire"))zone="Body";
        recordImpact(zone,q,damage);service(zone.equals("Armor")?4:5);
        hurtCar(damage/(1+.17f*level())*(protectedZone?.65f:1));

    }
    void service(int type){dataTracker.set(SERVICE,(int)((getWorld().getTime()&0xffffff)<<4)|type);}
    public int serviceType(){return dataTracker.get(SERVICE)&15;}
    public float serviceTime(){return (((int)getWorld().getTime()-(dataTracker.get(SERVICE)>>>4))&0xffffff)/20f;}
    public boolean serviceReady(){return flag(CAP)&&!flag(MOTOR)&&Math.abs(speed())<.012&&fuel()<GhostCamaro.CONFIG.tank;}
    public void startPump(PlayerEntity p,BlockPos pos){if(!serviceReady())return;fillingPlayer=p.getUuid();fillingPump=pos;set(FILLING,true);}
    public static float canLiters(ItemStack s){return s.hasNbt()&&s.getNbt().contains("Liters")?MathHelper.clamp(s.getNbt().getFloat("Liters"),0,12):12;}
    void fuelTick(){
        if(!flag(FILLING))return;
        var p=fillingPlayer==null?null:getWorld().getPlayerByUuid(fillingPlayer);
        if(!serviceReady()||p==null||p.squaredDistanceTo(this)>36){set(FILLING,false);return;}
        if(age%4!=0)return;
        float amount=Math.min(.25f,GhostCamaro.CONFIG.tank-fuel());
        if(fillingPump!=null){if(!fillingPump.isWithinDistance(getPos(),6)||!(getWorld().getBlockEntity(fillingPump) instanceof RouteWorkshop.Reserve r)){set(FILLING,false);return;}amount=r.take(amount);}
        else {var s=p.getStackInHand(fillingHand);if(!s.isOf(GhostCamaro.CAN)||s.getCount()!=1){set(FILLING,false);return;}amount=Math.min(amount,canLiters(s));s.getOrCreateNbt().putFloat("Liters",canLiters(s)-amount);}
        if(amount<=0){set(FILLING,false);return;}dataTracker.set(FUEL,fuel()+amount);
        if(age%20==0){playSound(SoundEvents.ITEM_BUCKET_FILL,.35f,.85f);p.sendMessage(Text.literal(String.format(Locale.ROOT,"%.1f / %.0f L",fuel(),GhostCamaro.CONFIG.tank)),true);}
    }
    private int input,lastInput=-100,lastAction=-1,startTicks,interpolation;
    private double tx,ty,tz; private float ta;
    private boolean dropped;
    public final float[] openings=new float[7];public float loadPitch,loadRoll,lastLoadPitch,lastLoadRoll;private float previousSpeed; public float lastRoll; public int lastGear,shiftAge=-100; public int gear(){return speed()<-.01?-1:(int)Math.min(4,1+Math.floor(Math.abs(speed())/.23));}
    public GhostCamaroEntity(EntityType<? extends GhostCamaroEntity> type,World w){super(type,w);setStepHeight(.66f);}
    protected void initDataTracker(){dataTracker.startTracking(FLAGS,0);dataTracker.startTracking(LOAD,0);dataTracker.startTracking(MODULES,0);dataTracker.startTracking(MARKS,new NbtCompound());dataTracker.startTracking(SERVICE,0);dataTracker.startTracking(SPEED,0f);dataTracker.startTracking(FUEL,GhostCamaro.CONFIG.tank);
        dataTracker.startTracking(HP,100f);dataTracker.startTracking(STEER,0f);dataTracker.startTracking(ROLL,0f);dataTracker.startTracking(FAILURE,0);dataTracker.startTracking(DRIVER,Optional.empty());dataTracker.startTracking(MIRRORS,new NbtCompound());}
    public boolean flag(int f){return (dataTracker.get(FLAGS)&f)!=0;}
    private void set(int f,boolean on){int old=dataTracker.get(FLAGS);dataTracker.set(FLAGS,on?old|f:old&~f);}
    public float speed(){return dataTracker.get(SPEED);}public float fuel(){return dataTracker.get(FUEL);}public float health(){return dataTracker.get(HP);}
    public float steering(){return dataTracker.get(STEER);}public float roll(){return dataTracker.get(ROLL);}
    public int failure(){return dataTracker.get(FAILURE);}public boolean driver(Entity p){return dataTracker.get(DRIVER).filter(p.getUuid()::equals).isPresent();}
    public float mirror(int i,int axis){return dataTracker.get(MIRRORS).getFloat("m"+i+axis);}
    public String status(){return String.format(Locale.ROOT,"%.0f km/h | %.1f L | %.0f%% | %d rpm",Math.abs(speed()*72),fuel(),health(),rpm());}
    public int rpm(){return flag(MOTOR)?(int)(800+Math.abs(speed())*3200):0;}
    public void repair(){storeMarks(new NbtList());dataTracker.set(HP,100f);dataTracker.set(FAILURE,0);}
    public void refuel(){dataTracker.set(FUEL,GhostCamaro.CONFIG.tank);}
    public void disableEngine(){set(MOTOR,false);startTicks=0;}
    public boolean canHit(){return true;}public boolean isCollidable(){return true;} public boolean isPushable(){return false;}
    public Vec3d offset(double left,double up,double front){return new Vec3d(left,up,front).rotateY(-getYaw()*MathHelper.RADIANS_PER_DEGREE);}
    protected Box calculateBoundingBox(){
        return CarHull.bounds(this);
    }
    protected boolean canAddPassenger(Entity e){return e instanceof PlayerEntity&&getPassengerList().size()<2;}
    protected void updatePassengerPosition(Entity e,PositionUpdater u){Vec3d p=getPos().add(offset(driver(e)?.398:-.398,.035,-.20));u.accept(e,p.x,p.y,p.z);}
    protected void removePassenger(Entity e){super.removePassenger(e);if(driver(e)&&!getWorld().isClient){dataTracker.set(DRIVER,Optional.empty());input=0;}}
    public Vec3d updatePassengerForDismount(LivingEntity e){
        for(double lateral:new double[]{driver(e)?1.55:-1.55,driver(e)?-1.55:1.55,2.5,-2.5})for(double fore:new double[]{0,1,-1,2.9,-2.9})for(int y:new int[]{0,1,-1}){
            Vec3d p=getPos().add(offset(lateral,y,fore));BlockPos b=BlockPos.ofFloored(p);Box bb=e.getDimensions(EntityPose.STANDING).getBoxAt(p);
            if(!getBoundingBox().intersects(bb)&&getWorld().isSpaceEmpty(e,bb)&&!getWorld().getFluidState(b).isIn(FluidTags.LAVA)&&getWorld().getBlockState(b.down()).isSolidBlock(getWorld(),b.down()))return p;
        }return super.updatePassengerForDismount(e);
    }
    public ActionResult interactAt(PlayerEntity p,Vec3d hit,Hand hand){
        if(hand!=Hand.MAIN_HAND)return ActionResult.PASS;
        float inverse=getYaw()*MathHelper.RADIANS_PER_DEGREE;
        Vec3d local=hit.rotateY(inverse);
        if(CarHull.select(local,local).isEmpty()){
            Vec3d eye=p.getEyePos().subtract(getPos()).rotateY(inverse);
            var precise=CarHull.select(eye,eye.add(p.getRotationVec(1).rotateY(inverse).multiply(6)));
            if(precise.isEmpty())return ActionResult.PASS;
            local=precise.get();
        }
        if(getWorld().isClient)return ActionResult.SUCCESS;
        ItemStack stack=p.getStackInHand(hand);
        if(stack.isOf(GhostCamaro.WRENCH)||stack.isOf(GhostCamaro.PLATE)){
            if(Math.abs(speed())>.012||flag(MOTOR))return ActionResult.FAIL;
            p.openHandledScreen(new SimpleNamedScreenHandlerFactory((id,inv,who)->new WorkshopMenu(id,inv,this),Text.translatable("ghostcamaro.workshop")));return ActionResult.CONSUME;
        }
        if(stack.isOf(GhostCamaro.KIT)){
            String pane=local.y>.82&&local.y<1.30?(Math.abs(local.x)<.7&&local.z>0&&local.z<.72?"Windshield":
                    Math.abs(local.x)>.6&&local.z>-.95&&local.z<.55?(local.x>=0?"Window_L":"Window_R"):null):null;
            if(pane!=null){
                if(Math.abs(speed())>.012||glassStage(pane)==0)return ActionResult.FAIL;
                repairPane(pane);service(3);if(!p.isCreative())stack.decrement(1);return ActionResult.CONSUME;
            }
            if(Math.abs(speed())>.012||(health()>=100&&marks().isEmpty()&&damageState().getKeys().stream().noneMatch(k->!k.equals("List")&&!k.equals("Panes"))))return ActionResult.FAIL;
            float hp=Math.min(100,health()+25);dataTracker.set(HP,hp);if(hp>10)dataTracker.set(FAILURE,0);
            var state=damageState().copy();var l=state.getList("List",10);
            for(int i=0;i<4&&!l.isEmpty();i++)l.remove(l.size()-1);
            // Retain faults whose contact decal aged out. Each kit can repair one such zone.
            var represented=new java.util.HashSet<String>();
            for(int i=0;i<l.size();i++)represented.add(l.getCompound(i).getString("Zone"));
            state.getKeys().stream().filter(k->!k.equals("List")&&!k.equals("Panes")&&!isPane(k)&&!represented.contains(k)).sorted().findFirst().ifPresent(state::remove);
            state.put("List",l);dataTracker.set(MARKS,state);service(3);
            if(!p.isCreative())stack.decrement(1);return ActionResult.CONSUME;
        }
        if(local.z< -1.58 && (stack.isOf(GhostCamaro.CAN)||(p.isSneaking()&&Math.abs(local.x)<.30&&local.y<.59))){
            if(Math.abs(speed())>.012||flag(MOTOR))return ActionResult.FAIL;
            if(!flag(CAP)||!stack.isOf(GhostCamaro.CAN)){toggle(CAP);return ActionResult.CONSUME;}
            if(serviceReady()&&canLiters(stack)>0){if(stack.getCount()>1){ItemStack spare=stack.split(stack.getCount()-1);p.getInventory().offerOrDrop(spare);}fillingHand=hand;fillingPump=null;fillingPlayer=p.getUuid();set(FILLING,true);}return ActionResult.CONSUME;
        }
        if(Math.abs(speed())>.04)return ActionResult.FAIL;
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
        super.tick();capPhase=MathHelper.stepTowards(capPhase,flag(CAP)?1:0,.09f);if(!getWorld().isClient)fuelTick();
        int gear=gear();if(gear!=lastGear){shiftAge=age;lastGear=gear;}
        int[] bits={DOOR_L,DOOR_R,HOOD,TRUNK,WIN_L,WIN_R,GLOVEBOX};
        for(int i=0;i<bits.length;i++)openings[i]=MathHelper.stepTowards(openings[i],flag(bits[i])?1:0,i==4||i==5?.042f:.065f);
        if(getWorld().isClient){lastLoadPitch=loadPitch;lastLoadRoll=loadRoll;
            loadPitch=MathHelper.lerp(.28f,loadPitch,MathHelper.clamp((speed()-previousSpeed)*105,-2.2f,2.2f));
            loadRoll=MathHelper.lerp(.22f,loadRoll,steering()*speed()*2.8f);previousSpeed=speed();
            if(interpolation>0){setPosition(getX()+(tx-getX())/interpolation,getY()+(ty-getY())/interpolation,getZ()+(tz-getZ())/interpolation);setYaw(getYaw()+MathHelper.wrapDegrees(ta-getYaw())/interpolation);interpolation--;}return;}
        if(age-lastInput>7||getPassengerList().stream().noneMatch(this::driver))input=0;
        if(startTicks>0&&--startTicks==0&&fuel()>0&&failure()==0)set(MOTOR,true);
        double v=speed(),before=v;Settings c=GhostCamaro.CONFIG;boolean gas=(input&1)!=0,back=(input&2)!=0,park=(input&16)!=0;
        float turn=MathHelper.stepTowards(steering(),((input&4)!=0?1:0)-((input&8)!=0?1:0),.12f);dataTracker.set(STEER,turn);
        boolean braking=back&&v>.004||park;set(STOP,braking);set(PARK,park);set(THROTTLE,gas&&flag(MOTOR));
        double grip=MathHelper.clamp(.6/getWorld().getBlockState(getBlockPos().down()).getBlock().getSlipperiness(),.45,1.25);
        if(park||back&&v>0)v=MathHelper.stepTowards((float)v,0,(float)((park?c.emergencyBrake:c.normalBrake)*grip));
        else if(flag(MOTOR)&&(gas||back))v+=c.acceleration/(1+.09*level())*(gas?1:-.52)*(health()<45?.37:health()<85?.88:1)*grip;
        else v=MathHelper.stepTowards((float)v,0,.0018f);
        boolean tire=damaged("Tire_FL")||damaged("Tire_FR")||damaged("Tire_RL")||damaged("Tire_RR");v=MathHelper.clamp(v,-.27,c.maxSpeed*(tire?.58:1));if(tire)turn*=.76f;
        if(flag(TRUNK)||flag(HOOD)||flag(CAP))v=0;
        float yaw=getYaw();setYaw(yaw-(float)(Math.toDegrees(v/2.74*Math.tan(Math.toRadians(29*turn/(1+1.6*Math.abs(v)))))));
        if(getYaw()!=yaw&&!CarHull.clear(this))setYaw(yaw);setBoundingBox(calculateBoundingBox());
        Vec3d delta=offset(0,0,v);double xx=getX(),zz=getZ();
        double vy=Math.max(-1.2,getVelocity().y-.08);move(MovementType.SELF,new Vec3d(delta.x,vy,delta.z));setVelocity(0,isOnGround()?0:vy*.98,0);
        double travel=Math.hypot(getX()-xx,getZ()-zz);if(horizontalCollision&&travel<Math.abs(v)*.65){if(Math.abs(before)>.15)collisionDamage(before);v=0;}
        dataTracker.set(SPEED,(float)v);dataTracker.set(ROLL,(float)((roll()+Math.copySign(travel,v==0?before:v)/.345)%(Math.PI*2)));
        if(flag(MOTOR)){dataTracker.set(FUEL,Math.max(0,fuel()-c.idleConsumption-(float)Math.abs(v)*c.driveConsumption*(1+.07f*level())));if(fuel()==0)disableEngine();}
        if((flag(LEFT)||flag(RIGHT)||flag(HAZARD))&&age%14==0)playSound(GhostCamaro.CLICK,.26f,1);
        if(failure()>0&&failure()<182){int f=failure()+1;dataTracker.set(FAILURE,f);if(f==143){playSound(GhostCamaro.BURST,1.25f,1);set(HOOD,true);}}
        if(flag(MOTOR)&&age%12==0)playSound(GhostCamaro.ENGINE,.55f,.8f+Math.abs(speed())*1.6f);
    }
    public void updateTrackedPositionAndAngles(double x,double y,double z,float a,float p,int steps,boolean interp){tx=x;ty=y;tz=z;ta=a;interpolation=3;}
    public void hurtCar(float amount){if(getWorld().isClient||amount<=0||!Float.isFinite(amount))return;dataTracker.set(HP,Math.max(0,health()-amount));if(health()<=10&&failure()==0){disableEngine();dataTracker.set(FAILURE,1);}}
    public boolean damage(DamageSource source,float amount){if(isInvulnerableTo(source))return false;if(source.getSource() instanceof net.minecraft.entity.projectile.ProjectileEntity shot)impact(shot.getPos(),amount);else hurtCar(amount);return true;}
    public void remove(RemovalReason reason){if(!getWorld().isClient&&!dropped&&reason.shouldDestroy()){dropped=true;ItemScatterer.spawn(getWorld(),this,cargo);cargo.clear();}super.remove(reason);}
    protected void writeCustomDataToNbt(NbtCompound n){n.putInt("Flags",dataTracker.get(FLAGS)&~FILLING);n.putInt("Modules",dataTracker.get(MODULES));n.put("Marks",marks().copy());n.put("DamageState",damageState().copy());n.putFloat("Fuel",fuel());n.putFloat("Health",health());n.putFloat("Speed",speed());n.putInt("Failure",failure());n.put("Mirrors",dataTracker.get(MIRRORS).copy());dataTracker.get(DRIVER).ifPresent(u->n.putUuid("Driver",u));
        DefaultedList<ItemStack> l=DefaultedList.ofSize(27,ItemStack.EMPTY);for(int i=0;i<27;i++)l.set(i,cargo.getStack(i));Inventories.writeNbt(n,l);}
    protected void readCustomDataFromNbt(NbtCompound n){dataTracker.set(FLAGS,n.getInt("Flags")&~FILLING);dataTracker.set(MODULES,n.contains("Modules")?n.getInt("Modules")&7:7);if(n.contains("DamageState",10))dataTracker.set(MARKS,n.getCompound("DamageState").copy());else storeMarks(n.getList("Marks",10).copy());if(n.contains("Fuel"))dataTracker.set(FUEL,n.getFloat("Fuel"));if(n.contains("Health"))dataTracker.set(HP,n.getFloat("Health"));dataTracker.set(SPEED,n.getFloat("Speed"));dataTracker.set(FAILURE,n.getInt("Failure"));dataTracker.set(MIRRORS,n.getCompound("Mirrors").copy());if(n.containsUuid("Driver"))dataTracker.set(DRIVER,Optional.of(n.getUuid("Driver")));
        DefaultedList<ItemStack> l=DefaultedList.ofSize(27,ItemStack.EMPTY);Inventories.readNbt(n,l);for(int i=0;i<27;i++)cargo.setStack(i,l.get(i));}
}
