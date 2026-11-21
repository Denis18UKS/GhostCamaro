package dev.ghostcamaro.client;
import dev.ghostcamaro.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.*;
import net.minecraft.client.gui.screen.*;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.block.Blocks;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.resource.DataConfiguration;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import net.minecraft.world.gen.*;
import net.minecraft.world.level.LevelInfo;
import java.nio.file.*;
import java.util.*;
public class VehicleSmoke implements ClientModInitializer {
    int stage,ticks;static volatile Throwable failure;static volatile boolean serverDone;
    static void check(boolean b,String m){if(!b)throw new AssertionError(m);System.out.println("[GHOST TEST] "+m);}
    public void onInitializeClient(){ClientTickEvents.END_CLIENT_TICK.register(c->{try{tick(c);}catch(Throwable e){e.printStackTrace();throw new RuntimeException(e);}});}
    void tick(MinecraftClient c)throws Exception{
        if(failure!=null)throw new RuntimeException(failure);
        if(stage==0&&(c.currentScreen instanceof TitleScreen||c.currentScreen instanceof AccessibilityOnboardingScreen)){
            stage=1;c.options.onboardAccessibility=false;c.options.getViewDistance().setValue(5);c.options.getSimulationDistance().setValue(5);
            var info=new LevelInfo("Vehicle integration",GameMode.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),DataConfiguration.SAFE_MODE);
            c.createIntegratedServerLoader().createAndStart("vehicle-smoke-"+System.currentTimeMillis(),info,new GeneratorOptions(1979L,false,false),WorldPresets::createDemoOptions);return;
        }
        if(stage==1&&c.player!=null&&c.world!=null&&c.getServer()!=null&&++ticks>40){
            stage=2;var srv=c.getServer();var uuid=c.player.getUuid();srv.execute(()->{try{
                ServerPlayerEntity p=srv.getPlayerManager().getPlayer(uuid);var w=p.getServerWorld();var origin=p.getBlockPos();int x=origin.getX(),z=origin.getZ(),y=242;
                for(int dx=-9;dx<=9;dx++)for(int dz=-14;dz<=18;dz++)w.setBlockState(new BlockPos(x+dx,y-1,z+dz),Blocks.STONE.getDefaultState());
                for(int dx=-7;dx<=7;dx++)for(int y1=0;y1<4;y1++)w.setBlockState(new BlockPos(x+dx,y+y1,z-6), (dx< -2?Blocks.RED_CONCRETE:dx>2?Blocks.BLUE_CONCRETE:Blocks.YELLOW_CONCRETE).getDefaultState());
                var car=new GhostCamaroEntity(GhostCamaro.CAR,w);car.refreshPositionAndAngles(x+.5,y,z+.5,0,0);check(w.spawnEntity(car),"spawn vehicle");
                car.cargo.setStack(12,new ItemStack(Items.BREAD,17));check(Cargo.exchange(car,s->s.isOf(Items.BREAD),4,new ItemStack(Items.IRON_INGOT,2)),"cargo transfer commits");
                check(!Cargo.exchange(car,s->s.isOf(Items.BREAD),64,new ItemStack(Items.DIAMOND)),"failed transfer rolls back");
                var cp=new GhostCamaroEntity(GhostCamaro.CAR,w);cp.readNbt(car.writeNbt(new NbtCompound()));check(cp.cargo.getStack(12).getCount()==13&&Cargo.count(cp,s->s.isOf(Items.IRON_INGOT))==2,"NBT slot persistence");
                p.requestTeleport(x+.8,y,z+.5);car.interactAt(p,new Vec3d(.9,.7,0),Hand.MAIN_HAND);check(car.driver(p)&&p.getVehicle()==car,"seat authorization");
                car.input(p,0,14,1,12,-4);cp.readNbt(car.writeNbt(new NbtCompound()));check(cp.mirror(1,0)==12&&cp.mirror(1,1)==-4&&cp.mirror(0,0)==0,"independent mirror angles persist");serverDone=true;
            }catch(Throwable e){failure=e;}});ticks=0;return;
        }
        if(stage==2&&serverDone&&CamaroClient.car()!=null&&++ticks>50){
            stage=3;ticks=0;c.options.setPerspective(Perspective.FIRST_PERSON);CamaroClient.send(CamaroClient.car(),0,1,0,0,0);c.player.setYaw(0);c.player.setPitch(0);c.setScreen(null);return;
        }
        if(stage==3&&++ticks>70){
            Path dir=Path.of("evidence");Files.createDirectories(dir);var car=CamaroClient.car();check(car!=null&&car.flag(1),"client sees running engine");check(MirrorSystem.ready,"three world render passes completed");
            Set<Integer> fingerprints=new HashSet<>();
            for(int i=0;i<3;i++)try(var image=ScreenshotRecorder.takeScreenshot(MirrorSystem.fb[i])){
                image.writeTo(dir.resolve("mirror-"+i+".png"));Set<Integer> colors=new HashSet<>();int hash=1;
                for(int y=0;y<image.getHeight();y+=2)for(int x=0;x<image.getWidth();x+=2){int px=image.getColor(x,y);colors.add(px);hash=31*hash+px;}
                check(colors.size()>8,"world detail in mirror "+i);fingerprints.add(hash);}
            check(fingerprints.size()==3,"three distinct reflection images");
            try(var im=ScreenshotRecorder.takeScreenshot(c.getFramebuffer())){im.writeTo(dir.resolve("cockpit.png"));}
            c.options.setPerspective(Perspective.THIRD_PERSON_BACK);stage=4;ticks=0;
        }
        if(stage==4&&++ticks>20){
            Path dir=Path.of("evidence");try(var im=ScreenshotRecorder.takeScreenshot(c.getFramebuffer())){im.writeTo(dir.resolve("exterior.png"));}
            Files.writeString(dir.resolve("PASS.txt"),"Integrated Fabric 1.20.1 client/server: spawn, seat, engine, cargo transaction, NBT round-trip, mirror framebuffers.\n");stage=5;c.scheduleStop();
        }
    }
}
