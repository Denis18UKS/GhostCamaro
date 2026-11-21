package dev.ghostcamaro.client;

import dev.ghostcamaro.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screen.*;
import net.minecraft.client.network.*;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.item.*;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import java.nio.file.*;
import java.util.*;

final class CargoSmoke {
    static final Path DIR=Path.of(System.getProperty("cargo.evidence","evidence")).toAbsolutePath();
    static final String[] NAMES={"empty","one","mixed","full","closed","removed","unloaded","restored"};
    static final int[] VALUES={0,1,1+(1<<6)+(1<<12)+(1<<18)+(1<<24),6+(6<<6)+(5<<12)+(5<<18)+(5<<24),6+(6<<6)+(5<<12)+(5<<18)+(5<<24),5+(6<<6)+(5<<12)+(5<<18)+(5<<24),0,1+(1<<24)};
    static final Item[] ITEMS={Items.BREAD,Items.POTION,GhostCamaro.KIT,GhostCamaro.CAN,Items.IRON_INGOT};
    int phase,frames;boolean configured;long started=System.nanoTime();Process observer;
    static boolean secondary(){return Boolean.getBoolean("cargo.observer");}
    void launch(MinecraftClient mc)throws Exception{
        Files.createDirectories(DIR);var srv=mc.getServer();srv.setOnlineMode(false);
        VehicleSmoke.check(srv.openToLan(net.minecraft.world.GameMode.CREATIVE,false,25576),"test LAN opened");
        var info=ProcessHandle.current().info();var command=new ArrayList<String>();command.add(info.command().orElseThrow());
        command.add("-Dcargo.observer=true");command.add("-Dcargo.evidence="+DIR);command.add("-Xmx1100m");
        var args=new ArrayList<>(Arrays.asList(info.arguments().orElseThrow()));
        for(String name:List.of("--username","--uuid","--gameDir")){
            int at=args.indexOf(name);if(at>=0){args.remove(at);args.remove(at);}
        }
        Path home=Path.of("observer-client").toAbsolutePath();Files.createDirectories(home);
        args.addAll(List.of("--username","CargoObserver","--gameDir",home.toString()));
        command.addAll(args);observer=new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(DIR.resolve("observer.log").toFile()).start();
    }
    static GhostCamaroEntity visible(MinecraftClient mc){if(mc.world!=null)for(var e:mc.world.getEntities())if(e instanceof GhostCamaroEntity c)return c;return null;}
    boolean tick(MinecraftClient mc,UUID id)throws Exception{
        if((System.nanoTime()-started)>450_000_000_000L)throw new AssertionError("cargo observer timeout at "+phase);
        if(observer!=null&&!observer.isAlive()&&phase<NAMES.length-1)throw new AssertionError("observer exited at "+phase);
        if(secondary()&&!configured){
            if(mc.getOverlay()==null && net.minecraft.client.render.GameRenderer.getRenderTypeSolidProgram()!=null && (mc.currentScreen instanceof TitleScreen||mc.currentScreen instanceof AccessibilityOnboardingScreen)){
                configured=true;mc.options.onboardAccessibility=false;mc.options.getViewDistance().setValue(3);
                var address=new ServerAddress("127.0.0.1",25576);
                ConnectScreen.connect(new TitleScreen(),mc,address,new ServerInfo("Cargo QA","127.0.0.1:25576",true),false);
            }
            return false;
        }
        if(mc.player==null || visible(mc)==null)return false;
        if(phase==NAMES.length)return true;
        String name=NAMES[phase];Path signal=DIR.resolve("phase-"+name);var car=visible(mc);
        if(!secondary()&&!configured){
            if(mc.getServer().getCurrentPlayerCount()<2)return false;
            configured=true;int n=phase;var server=mc.getServer();server.execute(()->{try{
                var v=(GhostCamaroEntity)server.getOverworld().getEntity(id);
                var owner=server.getPlayerManager().getPlayer(mc.player.getUuid());
                if(n==0){
                    for(var p:server.getPlayerManager().getPlayerList()){
                        p.stopRiding();boolean host=p==owner;
                        p.teleport(v.getWorld() instanceof net.minecraft.server.world.ServerWorld world?world:server.getOverworld(),v.getX()+(host?.38:-.40),v.getY(),v.getZ()-3.45,host?9:-9,38);
                    }
                    v.interactAt(owner,new Vec3d(0,.69,-2.04),Hand.MAIN_HAND);owner.closeHandledScreen();v.cargo.clear();
                }
                if(n==1)v.cargo.setStack(0,new ItemStack(ITEMS[0],1));
                if(n==2||n==3)for(int i=0;i<(n==2?5:27);i++)v.cargo.setStack(i,new ItemStack(ITEMS[i%5]));
                if(n==4||n==5){owner.setSneaking(true);v.interactAt(owner,new Vec3d(0,.70,-2.04),Hand.MAIN_HAND);owner.setSneaking(false);owner.closeHandledScreen();}
                if(n==5)v.cargo.removeStack(0);
                if(n==6)v.cargo.clear();
                if(n==7){v.cargo.setStack(12,new ItemStack(Items.BREAD,13));v.cargo.setStack(2,new ItemStack(Items.IRON_INGOT,2));}
                VehicleSmoke.check(v.cargoDisplay()==VALUES[n],"server cargo "+name);Files.createFile(signal);
            }catch(Throwable ex){VehicleSmoke.failure=ex;}});return false;
        }
        if(!Files.exists(signal)||car.cargoDisplay()!=VALUES[phase]||(phase==4?car.openings[3]>.001:car.openings[3]<.999)){frames=0;return false;}
        if(++frames<10)return false;
        String who=secondary()?"observer":"host";
        Path shot=DIR.resolve(who+"-"+name+".png");
        if(!Files.exists(shot)){
            mc.options.setPerspective(Perspective.FIRST_PERSON);
            try(var im=ScreenshotRecorder.takeScreenshot(mc.getFramebuffer())){im.writeTo(shot);}
            VehicleSmoke.check(true,who+" sees "+name+" "+car.cargoDisplay());
        }
        if(!secondary()&&!Files.exists(DIR.resolve("observer-"+name+".png")))return false;
        phase++;frames=0;if(!secondary())configured=false;
        return phase==NAMES.length;
    }
}
