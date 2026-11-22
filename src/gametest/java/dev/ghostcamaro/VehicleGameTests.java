package dev.ghostcamaro;
import net.minecraft.block.Blocks;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.test.*;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
public class VehicleGameTests {
    // ServerWorld increments Entity.age immediately before it invokes tick().
    private static void tick(GhostCamaroEntity c){c.age++;c.tick();}
    @GameTest(templateName="fabric-gametest-api-v1:empty")
    public void driveAndRecover(TestContext t) {
        var w=t.getWorld();var p=t.createMockCreativePlayer();var q=t.createMockCreativePlayer();
        var base=t.getAbsolutePos(new BlockPos(2,14,2));
        for(int x=-5;x<=5;x++)for(int z=-5;z<=18;z++)w.setBlockState(base.add(x,-1,z),Blocks.STONE.getDefaultState());
        var c=new GhostCamaroEntity(GhostCamaro.CAR,w);c.refreshPositionAndAngles(base.getX()+.5,base.getY(),base.getZ()+.5,0,0);
        var other=new GhostCamaroEntity(GhostCamaro.CAR,w);other.refreshPositionAndAngles(base.getX()+5,base.getY(),base.getZ(),0,0);
        t.assertTrue(w.spawnEntity(c)&&w.spawnEntity(other),"two vehicles in dedicated world");
        c.interactAt(p,new Vec3d(.8,.6,0),Hand.MAIN_HAND);c.interactAt(q,new Vec3d(-.8,.6,0),Hand.MAIN_HAND);
        t.assertTrue(c.driver(p)&&q.getVehicle()==c&&!c.driver(q),"driver and passenger");
        c.input(q,1,1,0,0,0);c.input(p,0,1,0,0,0);
        for(int i=0;i<12;i++)tick(c);
        t.assertTrue(c.flag(GhostCamaroEntity.MOTOR),"ignition");
        double initialZ=c.getZ();
        for(int i=0;i<19;i++){c.input(p,1,0,0,0,0);tick(c);}
        t.assertTrue(c.getZ()>initialZ+.5&&c.speed()>.1&&c.fuel()<GhostCamaro.CONFIG.tank,"forward movement and fuel");
        for(int i=0;i<6;i++){c.input(p,5,0,0,0,0);tick(c);}
        t.assertTrue(c.getYaw()<0&&c.steering()>0,"left steering");
        float v=c.speed();c.input(p,2,0,0,0,0);tick(c);float normal=v-c.speed();v=c.speed();c.input(p,16,0,0,0,0);tick(c);
        t.assertTrue(normal>0&&v-c.speed()>normal&&c.flag(GhostCamaroEntity.STOP),"normal and emergency braking");
        c.input(p,16,14,2,11,-3);tick(c);t.assertTrue(other.mirror(2,0)==0,"independent second vehicle");
        c.input(p,16,2,0,0,0);tick(c);c.input(p,16,3,0,0,0);tick(c);c.input(p,16,5,0,0,0);tick(c);
        t.assertTrue(c.flag(GhostCamaroEntity.LIGHTS)&&c.flag(GhostCamaroEntity.LEFT)&&c.flag(GhostCamaroEntity.HAZARD),"independent light state");
        c.cargo.setStack(18,new ItemStack(Items.BREAD,24));
        other.readNbt(c.writeNbt(new NbtCompound()));t.assertTrue(other.mirror(2,0)==11&&other.cargo.getStack(18).getCount()==24,"cargo and calibration reload");
        c.hurtCar(60);t.assertTrue(c.health()==40&&c.failure()==0,"minor breakdown");
        c.hurtCar(33);t.assertTrue(c.failure()>0&&!c.flag(GhostCamaroEntity.MOTOR),"critical engine shutdown");
        for(int i=0;i<150;i++)tick(c);
        t.assertTrue(c.flag(GhostCamaroEntity.HOOD),"critical burst");c.repair();c.refuel();
        t.assertTrue(c.failure()==0&&c.health()==100&&c.fuel()==GhostCamaro.CONFIG.tank,"repair and refuel");t.complete();
    }

    @GameTest(templateName="fabric-gametest-api-v1:empty")
    public void trunkAndHull(TestContext t){
        var world=t.getWorld();var pos=t.getAbsolutePos(new BlockPos(4,12,4));
        var car=new GhostCamaroEntity(GhostCamaro.CAR,world);car.refreshPositionAndAngles(pos.getX()+.5,pos.getY(),pos.getZ()+.5,45,0);
        t.assertTrue(world.spawnEntity(car),"spawn angled car");
        var broad=CarHull.bounds(car);
        var corner=new Box(broad.maxX-.15,broad.minY+.2,broad.maxZ-.15,broad.maxX-.02,broad.minY+.4,broad.maxZ-.02);
        t.assertTrue(broad.intersects(corner)&&world.getEntityCollisions(null,corner).isEmpty(),"diagonal broad-phase corner is physically empty");
        var center=Box.of(car.getPos().add(0,.5,0),.12,.12,.12);
        t.assertTrue(!world.getEntityCollisions(null,center).isEmpty(),"actual car body blocks contact");
        t.assertTrue(CarHull.select(new Vec3d(1.6,.6,2),new Vec3d(1.6,.6,-2)).isEmpty(),"ray alongside body misses");
        t.assertTrue(CarHull.select(new Vec3d(.23,2,-2.04),new Vec3d(.23,.5,-2.04)).isPresent(),"trunk ray from above");
        var a=t.createMockCreativePlayer();var b=t.createMockCreativePlayer();
        var invA=net.minecraft.screen.GenericContainerScreenHandler.createGeneric9x3(1,a.getInventory(),car.cargo);
        var invB=net.minecraft.screen.GenericContainerScreenHandler.createGeneric9x3(2,b.getInventory(),car.cargo);
        car.cargo.setStack(6,new ItemStack(Items.BREAD,15));
        t.assertTrue(!invA.quickMove(a,6).isEmpty()&&invB.quickMove(b,6).isEmpty(),"only first removal from shared slot succeeds");
        t.assertTrue(a.getInventory().count(Items.BREAD)==15&&b.getInventory().count(Items.BREAD)==0&&car.cargoDisplay()==0,"no duplicated stacks after competing clicks");
        for(int i=0;i<27;i++)car.cargo.setStack(i,new ItemStack(GhostCamaro.CAN));
        int summary=car.cargoDisplay();
        for(int i=0;i<27;i++)t.assertTrue(CargoDisplay.x(i)-.09>1.663 && CargoDisplay.x(i)+.09<2.348
            &&Math.abs(CargoDisplay.z(i))+.11<.679 && CargoDisplay.y(i,summary)+.126<.795,"cargo clears floor, walls and closed lid");
        int before=world.getEntitiesByClass(net.minecraft.entity.ItemEntity.class,broad.expand(3),e->true).stream().mapToInt(e->e.getStack().getCount()).sum();
        t.assertTrue(before==0,"display creates no item entities");
        car.remove(net.minecraft.entity.Entity.RemovalReason.KILLED);car.remove(net.minecraft.entity.Entity.RemovalReason.KILLED);
        int dropped=world.getEntitiesByClass(net.minecraft.entity.ItemEntity.class,broad.expand(3),e->e.getStack().isOf(GhostCamaro.CAN)).stream().mapToInt(e->e.getStack().getCount()).sum();
        t.assertTrue(dropped==27,"destruction drops exactly the original inventory once");t.complete();
    }
    @GameTest(templateName="fabric-gametest-api-v1:empty")
    public void workshopAndDamage(TestContext t){
        var w=t.getWorld();var base=t.getAbsolutePos(new BlockPos(2,8,2));var c=new GhostCamaroEntity(GhostCamaro.CAR,w);
        c.refreshPositionAndAngles(base.getX()+.5,base.getY(),base.getZ()+.5,0,0);
        var tag=c.writeNbt(new NbtCompound());tag.putFloat("Fuel",10);c.readNbt(tag);
        var p=t.createMockCreativeServerPlayerInWorld();p.refreshPositionAndAngles(c.getX(),c.getY(),c.getZ()-2.9,0,0);
        t.assertTrue(w.spawnEntity(c),"spawn V7 Camaro");p.setStackInHand(Hand.MAIN_HAND,new ItemStack(GhostCamaro.CAN));
        c.interactAt(p,new Vec3d(0,.50,-2.2),Hand.MAIN_HAND);t.assertTrue(c.serviceReady(),"fuel door opens");
        c.interactAt(p,new Vec3d(0,.50,-2.2),Hand.MAIN_HAND);c.age=4;c.fuelTick();
        t.assertTrue(c.fuel()==10.25f&&GhostCamaroEntity.canLiters(p.getMainHandStack())==11.75f,"conserved can-to-tank transfer");
        var pp=base.add(3,0,0);w.setBlockState(pp,RouteWorkshop.pump.getDefaultState());
        var store=(RouteWorkshop.Reserve)w.getBlockEntity(pp);c.startPump(p,pp);c.fuelTick();
        t.assertTrue(c.fuel()==10.5f&&store.liters==119.75f,"finite pump transfer");
        p.refreshPositionAndAngles(c.getX(),c.getY()-.95,c.getZ()-4,0,0);
        int count=c.marks().size();((RangeItem)GhostCamaro.PISTOL).fire(w,p);
        t.assertTrue(c.marks().size()==count+1,"arcade projectile hits rear panel");
        p.refreshPositionAndAngles(c.getX(),c.getY(),c.getZ()-2.9,0,0);
        var ui=new WorkshopMenu(3,p.getInventory(),c);
        ui.onSlotClick(0,0,net.minecraft.screen.slot.SlotActionType.PICKUP,p);t.assertTrue(c.hasModule(0),"install door panels");
        ui.onSlotClick(0,0,net.minecraft.screen.slot.SlotActionType.PICKUP,p);t.assertTrue(!c.hasModule(0),"remove panels");
        c.impact(c.getPos().add(c.offset(.88,.70,2.1)),8);c.impact(c.getPos().add(c.offset(.9,.4,1.45)),7);
        t.assertTrue(c.damaged("Lamp_L")&&c.damaged("Tire_FL")&&!c.damaged("Lamp_R"),"localized lamp and tire");
        var copy=new GhostCamaroEntity(GhostCamaro.CAR,w);copy.readNbt(c.writeNbt(new NbtCompound()));
        t.assertTrue(copy.damaged("Lamp_L")&&copy.damaged("Tire_FL")&&copy.fuel()==10.5f,"V7 NBT persisted");
        for(int i=0;i<28;i++)c.impact(c.getPos().add(0,.5,0),.25f);
        t.assertTrue(c.marks().size()==24&&c.damaged("Tire_FL"),"bounded decals retain old mechanical damage");
        copy.readNbt(c.writeNbt(new NbtCompound()));t.assertTrue(copy.damaged("Tire_FL"),"expired tire decal does not repair the tire on reload");
        copy.repair();copy.collisionDamage(.64);t.assertTrue(copy.damaged("Bumper")&&copy.damaged("Hood")&&copy.damaged("Windshield")&&copy.health()<65,"high speed frontal collision damage");
        copy.repair();copy.collisionDamage(-.20);t.assertTrue(copy.damaged("Bumper")&&!copy.damaged("Hood")&&copy.health()>80,"low speed rear impact");
        var lane=base.add(8,0,12);
        for(int x=-2;x<=2;x++)for(int z=-3;z<=5;z++)w.setBlockState(lane.add(x,-1,z),Blocks.STONE.getDefaultState());
        for(int x=-2;x<=2;x++)for(int y=0;y<=1;y++)w.setBlockState(lane.add(x,y,4),Blocks.STONE.getDefaultState());
        var crash=new GhostCamaroEntity(GhostCamaro.CAR,w);
        var moving=crash.writeNbt(new NbtCompound());moving.putFloat("Speed",.65f);crash.readNbt(moving);
        crash.refreshPositionAndAngles(lane.getX()+.5,lane.getY(),lane.getZ()+.5,0,0);w.spawnEntity(crash);
        for(int i=0;i<8;i++)tick(crash);
        t.assertTrue(crash.damaged("Bumper")&&crash.speed()==0,"driving into real wall causes damage and stops car");
        p.setStackInHand(Hand.MAIN_HAND,new ItemStack(GhostCamaro.KIT));float hp=c.health();c.interactAt(p,new Vec3d(.8,.7,0),Hand.MAIN_HAND);
        t.assertTrue(c.health()>hp&&c.marks().size()==20,"incremental repair");
        t.assertTrue(RouteWorkshop.catalog.size()==40,"40 architectural items");t.complete();
    }
}
