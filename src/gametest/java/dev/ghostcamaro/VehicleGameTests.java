package dev.ghostcamaro;
import net.minecraft.block.Blocks;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.test.*;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
public class VehicleGameTests {
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
        for(int i=0;i<12;i++)c.tick();
        t.assertTrue(c.flag(GhostCamaroEntity.MOTOR),"ignition");
        double initialZ=c.getZ();
        for(int i=0;i<19;i++){c.input(p,1,0,0,0,0);c.tick();}
        t.assertTrue(c.getZ()>initialZ+.5&&c.speed()>.1&&c.fuel()<GhostCamaro.CONFIG.tank,"forward movement and fuel");
        for(int i=0;i<6;i++){c.input(p,5,0,0,0,0);c.tick();}
        t.assertTrue(c.getYaw()<0&&c.steering()>0,"left steering");
        float v=c.speed();c.input(p,2,0,0,0,0);c.tick();float normal=v-c.speed();v=c.speed();c.input(p,16,0,0,0,0);c.tick();
        t.assertTrue(normal>0&&v-c.speed()>normal&&c.flag(GhostCamaroEntity.STOP),"normal and emergency braking");
        c.input(p,16,14,2,11,-3);c.tick();t.assertTrue(other.mirror(2,0)==0,"independent second vehicle");
        c.input(p,16,2,0,0,0);c.tick();c.input(p,16,3,0,0,0);c.tick();c.input(p,16,5,0,0,0);c.tick();
        t.assertTrue(c.flag(GhostCamaroEntity.LIGHTS)&&c.flag(GhostCamaroEntity.LEFT)&&c.flag(GhostCamaroEntity.HAZARD),"independent light state");
        c.cargo.setStack(18,new ItemStack(Items.BREAD,24));
        other.readNbt(c.writeNbt(new NbtCompound()));t.assertTrue(other.mirror(2,0)==11&&other.cargo.getStack(18).getCount()==24,"cargo and calibration reload");
        c.hurtCar(60);t.assertTrue(c.health()==40&&c.failure()==0,"minor breakdown");
        c.hurtCar(33);t.assertTrue(c.failure()>0&&!c.flag(GhostCamaroEntity.MOTOR),"critical engine shutdown");
        for(int i=0;i<150;i++)c.tick();
        t.assertTrue(c.flag(GhostCamaroEntity.HOOD),"critical burst");c.repair();c.refuel();
        t.assertTrue(c.failure()==0&&c.health()==100&&c.fuel()==GhostCamaro.CONFIG.tank,"repair and refuel");t.complete();
    }
}
