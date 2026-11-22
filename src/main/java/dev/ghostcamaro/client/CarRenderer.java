package dev.ghostcamaro.client;
import com.google.gson.*;
import dev.ghostcamaro.*;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import org.joml.Quaternionf;
import java.io.*;
import java.util.*;
final class CarRenderer extends EntityRenderer<GhostCamaroEntity>{
    static final Identifier[] tex=new Identifier[4];
    static {for(int i=0;i<4;i++)tex[i]=GhostCamaro.id("textures/entity/atlas"+i+".png");}
    static final class Node {String name;int mesh=-1;int[] children={};float[] translation={0,0,0},rotation={0,0,0,1},scale={1,1,1};}
    static final class Primitive {int texture,count;boolean alpha,glow;float[] data;}
    static final class Track {int node;String kind;float[] times;float[][] values;boolean step;}
    static final class Scene {int[] roots;Node[] nodes;Primitive[][] meshes;Map<String,Track[]> clips;}
    final Scene scene;final Map<String,Integer> nodeIndex=new HashMap<>();final float[][] tr,scale;final Quaternionf[] rotation;
    CarRenderer(EntityRendererFactory.Context ctx){super(ctx);shadowRadius=1.6f;
        var rm=CamaroClient.mc().getResourceManager();
        try(var reader=rm.getResourceOrThrow(GhostCamaro.id("vehicle/scene.json")).getReader();var in=new DataInputStream(new BufferedInputStream(rm.getResourceOrThrow(GhostCamaro.id("vehicle/geometry.bin")).getInputStream()))){
            scene=new Gson().fromJson(reader,Scene.class);for(var batch:scene.meshes)for(var p:batch){p.data=new float[p.count*8];for(int k=0;k<p.data.length;k++)p.data[k]=in.readFloat();}
        }catch(IOException ex){throw new UncheckedIOException(ex);}
        tr=new float[scene.nodes.length][3];scale=new float[scene.nodes.length][3];rotation=new Quaternionf[tr.length];
        for(int i=0;i<tr.length;i++){nodeIndex.put(scene.nodes[i].name,i);rotation[i]=new Quaternionf();}
    }
    public Identifier getTexture(GhostCamaroEntity car){return tex[0];}
    void play(String name,float time,boolean skipDrive){
        for(var t:scene.clips.get(name)){
            int i=t.node;Node n=scene.nodes[i];if(skipDrive&&(n.name.startsWith("Knuckle_")||n.name.startsWith("Wheel_")||n.name.equals("Vehicle_Root")))continue;
            int b=Arrays.binarySearch(t.times,time);if(b<0)b=-b-1;b=Math.min(b,t.times.length-1);int a=Math.max(0,b-1);
            float f=a==b?0:MathHelper.clamp((time-t.times[a])/(t.times[b]-t.times[a]),0,1);if(t.step&&f<1)f=0;
            float[] v=t.values[a],w=t.values[b];
            if(t.kind.equals("rotation"))rotation[i].set(v[0],v[1],v[2],v[3]).slerp(new Quaternionf(w[0],w[1],w[2],w[3]),f);
            else {float[] dst=t.kind.equals("scale")?scale[i]:tr[i];for(int k=0;k<3;k++)dst[k]=MathHelper.lerp(f,v[k],w[k]);}
        }
    }
    void angle(String name,float x,float y,float z,float angle){rotation[nodeIndex.get(name)].rotationAxis(angle,x,y,z);}
    public void render(GhostCamaroEntity c,float yaw,float dt,MatrixStack m,VertexConsumerProvider v,int light){
        if(MirrorSystem.rendering&&c==CamaroClient.car())return;
        for(int i=0;i<tr.length;i++){Node n=scene.nodes[i];System.arraycopy(n.translation,0,tr[i],0,3);System.arraycopy(n.scale,0,scale[i],0,3);rotation[i].set(n.rotation[0],n.rotation[1],n.rotation[2],n.rotation[3]);}
        float time=(c.age+dt)/20f;
        String[] op={"Door_L_Open","Door_R_Open","Hood_Open","Trunk_Open","Window_L_Down","Window_R_Down"};
        for(int i=0;i<6;i++)if(c.openings[i]>0)play(op[i],c.openings[i]*(i<4?.85f:1.2f),false);
        if(c.openings[6]>0)play("Glovebox_Open",.85f*c.openings[6],false);
        float turn=c.steering();if(turn!=0)play("Steering_Left_Right",turn>0?.7f*turn:1.4f-.7f*turn,false);
        for(String n:new String[]{"Wheel_FL","Wheel_FR","Wheel_RL","Wheel_RR"})angle(n,0,0,1,c.roll());
        if(c.flag(2))play("Headlights_On",.36f,false);
        if(c.flag(16))play("Hazards",time%1.44f,false);else{if(c.flag(4))play("Indicator_Left",time%1.44f,false);if(c.flag(8))play("Indicator_Right",time%1.44f,false);}
        if(c.flag(16384))play("Throttle_Cycle",.5f,false);
        if(c.age-c.shiftAge<26)play("Gear_Shift",(c.age-c.shiftAge+dt)/20f,false);
        if(c.flag(32)&&c.flag(8192))play("Brake_Emergency",.40f,true);else if(c.flag(32)&&Math.abs(c.speed())>.02)play("Brake_Normal",.44f,true);
        if(c.flag(32))play("Brake_Cycle",.5f,false);if(c.flag(8192))play("Handbrake_Engage",.45f,false);
        if(c.flag(4096))play("Wipers",time%1.4f,false);
        angle("Speed_needle",1,0,0,-Math.abs(c.speed())*1.95f);angle("RPM_needle",1,0,0,-c.rpm()*.00047f);
        if(c.failure()>60){play("Critical_Failure_Burst",Math.min(6,(c.failure()-60+dt)/20f),false);}else if(c.failure()>0){play("Smoke_Critical_Loop",(c.failure()+dt)/20f%3,false);}else if(c.health()<45){play("Smoke_Minor_Loop",time%3,false);if(c.flag(1))play("Breakdown_Stall",time%2,false);}
        if(c.capPhase>0)play("Fuel_Cap_Open",c.capPhase*.55f,false);
        if(c.flag(131072))play("Refueling",time%.8f,false);
        for(String w:new String[]{"FL","FR","RL","RR"})if(c.damaged("Tire_"+w))play("Tire_Damage_"+w,.48f,false);
        for(String side:new String[]{"L","R"}){
            if(c.damaged("Mirror_"+side))play("Mirror_Damage_"+side,.3f,false);
            if(c.damaged("Window_"+side))play("Window_Shatter_"+side,.2f,false);
            if(c.damaged("Lamp_"+side))play("Headlight_Break_"+side,.2f,false);
        }
        if(c.damaged("Windshield"))play("Windshield_Crack",.12f,false);
        if(c.damaged("Hood")&&c.openings[2]==0&&c.failure()==0)play("Hood_Damage",.45f,false);
        if(c.damaged("Bumper"))play("Bumper_Damage",.45f,false);
        if(c.serviceTime()<.55f){String clip=switch(c.serviceType()){case 1->"Armor_Install";case 2->"Armor_Remove";case 3->"Vehicle_Repair";case 4->"Bullet_Impact_Armor";case 5->"Bullet_Impact_Body";default->null;};if(clip!=null)play(clip,c.serviceTime(),false);}
        m.push();m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90-yaw));for(int root:scene.roots)draw(c,root,m,v,light);DamageRenderer.draw(c,m,v,light);BootRenderer.draw(c,dt,m,v,light);hands(c,m,v,light);m.pop();super.render(c,yaw,dt,m,v,light);
    }
    private void hands(GhostCamaroEntity car,MatrixStack m,VertexConsumerProvider buffers,int light){
        var client=CamaroClient.mc();var player=client.player;
        if(MirrorSystem.rendering || CamaroClient.bonnet || player==null || player.getVehicle()!=car || !car.driver(player) || !client.options.getPerspective().isFirstPerson())return;
        if(!(client.getEntityRenderDispatcher().getRenderer(player) instanceof PlayerEntityRenderer renderer))return;
        for(int side=-1;side<=1;side+=2){
            m.push();m.translate(.008,.875,.398);
            m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(car.steering()*40));
            m.translate(.218,.009,side*.157);
            m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(96));
            m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90));
            m.scale(.31f,.31f,.31f);
            m.translate(-side*5f/16,-2f/16,0);
            if(side>0)renderer.renderLeftArm(m,buffers,light,player);else renderer.renderRightArm(m,buffers,light,player);
            m.pop();
        }
    }
    void draw(GhostCamaroEntity car,int i,MatrixStack m,VertexConsumerProvider vp,int light){
        Node n=scene.nodes[i];if(n.name.equals("Cargo")||n.name.equals("Windshield_Fracture")&&!car.damaged("Windshield"))return;
        if((n.name.startsWith("Side_Plate")||n.name.equals("Armor"))&&!car.hasModule(0))return;
        if((n.name.equals("Engine_Guard")||n.name.equals("Rear_window_shield"))&&!car.hasModule(1))return;
        if((n.name.equals("Rear_Guard")||n.name.equals("Armored_Plough")||n.name.equals("Bullbar"))&&!car.hasModule(2))return;
        if((n.name.startsWith("Head_")||n.name.startsWith("BeamPreview_"))&&car.damaged("Lamp_"+n.name.charAt(n.name.length()-1)))return; m.push();m.translate(tr[i][0],tr[i][1],tr[i][2]);m.multiply(rotation[i]);
        if(scale[i][0]!=1||scale[i][1]!=1||scale[i][2]!=1)m.scale(scale[i][0],scale[i][1],scale[i][2]);
        int mirror=n.name.equals("Mirror_glass")?0:n.name.equals("Mirror_glass.001")?1:n.name.equals("Mirror_silver")?2:-1;
        if(mirror>=0){float x=mirror==2?-.103f:-.431f,y=mirror==2?1.128f:.908f,z=mirror==0?.953f:mirror==1?-.953f:0;
            m.translate(x,y,z);m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(car.mirror(mirror,0)));m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(car.mirror(mirror,1)));m.translate(-x,-y,-z);}
        if(n.mesh>=0)for(var p:scene.meshes[n.mesh]){
            boolean reflect=mirror>=0&&!(mirror<2&&car.damaged("Mirror_"+(mirror==0?"L":"R")))&&MirrorSystem.ready&&!MirrorSystem.rendering&&car==CamaroClient.car()&&CamaroClient.mc().options.getPerspective().isFirstPerson();
            var normal=vp.getBuffer(p.alpha?RenderLayer.getEntityTranslucent(tex[p.texture]):RenderLayer.getEntityCutoutNoCull(tex[p.texture]));
            var entry=m.peek();float[] d=p.data;
            for(int k=0;k<p.count;k+=3){
                boolean face=reflect&&d[k*8+5]>.9f;
                var dest=face?vp.getBuffer(RenderLayer.getEntitySolid(MirrorSystem.ids[mirror])):normal;
                for(int j=0;j<4;j++){int a=(k+Math.min(j,2))*8;float u=d[a+3],v=d[a+4];
                    if(face){float cz=mirror==0?.953f:mirror==1?-.953f:0;float cy=mirror==2?1.128f:.908f;
                        u=.5f+(d[a+2]-cz)/(mirror==2?.20f:.146f);v=.5f+(d[a+1]-cy)/(mirror==2?.046f:.087f);}
                    dest.vertex(entry.getPositionMatrix(),d[a],d[a+1],d[a+2]).color(255,255,255,255).texture(u,v).overlay(OverlayTexture.DEFAULT_UV).light(face||p.glow?LightmapTextureManager.MAX_LIGHT_COORDINATE:light).normal(entry.getNormalMatrix(),d[a+5],d[a+6],d[a+7]).next();}
                if(face)normal=vp.getBuffer(RenderLayer.getEntityCutoutNoCull(tex[p.texture]));
            }
        }
        for(int child:n.children)draw(car,child,m,vp,light);m.pop();
    }
}
