package dev.ghostcamaro.client;
import dev.ghostcamaro.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.*;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.*;
import net.minecraft.client.option.*;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.math.*;
import org.lwjgl.glfw.GLFW;
public class CamaroClient implements ClientModInitializer {
    public static class Options {public int mirrorWidth=256,mirrorFps=12;public float chaseDistance=5.4f,eyeHeight=1.09f;public boolean hud=true;}
    public static final Options config=Settings.load("ghostcamaro-client.json",new Options(),Options.class);
    static final String[] actions={"engine","lights","left","right","hazard","horn","wipers","window_l","window_r","door_l","door_r","hood","trunk","mirror","hood_cam","look_l","look_r","look_c"};
    static final int[] defaults={82,76,90,88,67,72,80,-1,-1,-1,-1,-1,-1,77,86,-1,-1,-1};
    static KeyBinding[] keys=new KeyBinding[actions.length];public static boolean bonnet;public static int glance=-1;public static float glanceBlend;static Perspective saved;static GhostCamaroEntity previous;
    public static MinecraftClient mc(){return MinecraftClient.getInstance();}
    static GhostCamaroEntity car(){return mc().player!=null&&mc().player.getVehicle() instanceof GhostCamaroEntity c?c:null;}
    public void onInitializeClient(){
        EntityRendererRegistry.register(GhostCamaro.CAR,CarRenderer::new);
        for(int i=0;i<keys.length;i++)keys[i]=KeyBindingHelper.registerKeyBinding(new KeyBinding("key.ghostcamaro."+actions[i],InputUtil.Type.KEYSYM,defaults[i],"category.ghostcamaro"));
        ClientTickEvents.END_CLIENT_TICK.register(c->{
            GhostCamaroEntity car=car();if(car!=previous){if(previous!=null&&saved!=null)c.options.setPerspective(saved);saved=car==null?null:c.options.getPerspective();previous=car;bonnet=false;glance=-1;glanceBlend=0;}
            if(car==null)return;int mask=0;
            if(c.currentScreen==null){
                if(c.options.forwardKey.isPressed())mask|=1;if(c.options.backKey.isPressed())mask|=2;if(c.options.leftKey.isPressed())mask|=4;if(c.options.rightKey.isPressed())mask|=8;if(c.options.jumpKey.isPressed())mask|=16;
                for(int i=0;i<keys.length;i++)while(keys[i].wasPressed()){if(i<13&&car.driver(c.player))send(car,mask,i+1,0,0,0);
                    if(i==13&&car.driver(c.player)){int m=MirrorSystem.target(car);if(m>=0)c.setScreen(new Adjust(car,m));else c.player.sendMessage(Text.translatable("message.ghostcamaro.aim_mirror"),true);}
                    if(i==14)bonnet=!bonnet;
                }
            }
            int target=keys[15].isPressed()?0:keys[16].isPressed()?1:keys[17].isPressed()?2:-1;
            if(target>=0)glance=target;glanceBlend=MathHelper.stepTowards(glanceBlend,target>=0?1:0,.2f);
            if(car.driver(c.player))send(car,mask,0,0,0,0);
        });
        HudRenderCallback.EVENT.register((draw,t)->{var car=car();if(car!=null&&config.hud&&!mc().options.hudHidden){
            String text=car.status()+"   "+(car.flag(GhostCamaroEntity.MOTOR)?"ON":"OFF")+"   "+(car.speed()<-.002?"R":car.speed()>.002?"D":"N")+(car.flag(2)?"   LIGHT":"")+(car.flag(16)?"   < >":(car.flag(4)?"   <":"")+(car.flag(8)?"   >":""));
            int y=mc().getWindow().getScaledHeight()-59;draw.fill(6,y-3,14+mc().textRenderer.getWidth(text),y+12,0x99000000);draw.drawTextWithShadow(mc().textRenderer,text,10,y,0xffe7d9);}});
    }
    static void send(GhostCamaroEntity car,int mask,int action,int mirror,float yaw,float pitch){var b=PacketByteBufs.create();b.writeVarInt(car.getId());b.writeByte(mask);b.writeByte(action);b.writeByte(mirror);b.writeFloat(yaw);b.writeFloat(pitch);ClientPlayNetworking.send(GhostCamaro.id("input"),b);}
    static class Adjust extends Screen {
        final GhostCamaroEntity car;final int index;float yaw,pitch;double lastX,lastY;boolean first=true;
        Adjust(GhostCamaroEntity c,int i){super(Text.translatable("key.ghostcamaro.mirror"));car=c;index=i;yaw=c.mirror(i,0);pitch=c.mirror(i,1);}
        public boolean shouldPause(){return false;}
        public void mouseMoved(double x,double y){if(!first){yaw=MathHelper.clamp(yaw+(float)(x-lastX)*.16f,-28,28);pitch=MathHelper.clamp(pitch+(float)(y-lastY)*.16f,-18,18);}lastX=x;lastY=y;first=false;}
        public void tick(){if(car!=CamaroClient.car()){close();return;}send(car,0,14,index,yaw,pitch);}
        public boolean mouseClicked(double x,double y,int button){if(button==1){yaw=0;pitch=0;}send(car,0,14,index,yaw,pitch);close();return true;}
        public void render(DrawContext d,int x,int y,float t){d.drawCenteredTextWithShadow(textRenderer,Text.translatable("message.ghostcamaro.adjust"),width/2,12,0xffffff);}
    }
}
