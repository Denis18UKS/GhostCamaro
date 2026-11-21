package dev.ghostcamaro.client;
import dev.ghostcamaro.*;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.*;
import net.minecraft.client.gl.*;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import org.joml.Matrix4f;import org.joml.Vector3f;
final class MirrorSystem {
    static boolean rendering,ready;static long next;static final Framebuffer[] fb=new Framebuffer[3];static final Identifier[] ids={GhostCamaro.id("rearview/left"),GhostCamaro.id("rearview/right"),GhostCamaro.id("rearview/center")};
    static Vec3d center(GhostCamaroEntity car,int i,float tick){return car.getLerpedPos(tick).add(car.offset(i==0?.953:i==1?-.953:0,i==2?1.128:.908,i==2?.103:.431));}
    static int target(GhostCamaroEntity car){var p=CamaroClient.mc().player;Vec3d eye=car.getPos().add(car.offset(.398,CamaroClient.config.eyeHeight,-.18));double best=.88;int selected=-1;for(int i=0;i<3;i++){double dot=center(car,i,1).subtract(eye).normalize().dotProduct(p.getRotationVec(1));if(dot>best){selected=i;best=dot;}}return selected;}
    static class RearCamera extends Camera {void pose(Vec3d where,Vec3d dir){setPos(where);setRotation((float)Math.toDegrees(Math.atan2(-dir.x,dir.z)),(float)-Math.toDegrees(Math.asin(dir.y)));}}
    static void update(float delta){
        var mc=CamaroClient.mc();var car=CamaroClient.car();int width=MathHelper.clamp(CamaroClient.config.mirrorWidth,0,512);
        if(rendering||car==null||mc.world==null||!mc.options.getPerspective().isFirstPerson()||CamaroClient.bonnet||width<64){ready=false;return;}
        long now=System.nanoTime();if(now<next&&ready)return;next=now+1000000000L/MathHelper.clamp(CamaroClient.config.mirrorFps,1,30);
        var main=mc.getFramebuffer();var prev=new Matrix4f(RenderSystem.getProjectionMatrix());var sorter=RenderSystem.getVertexSorting();rendering=true;
        try{
            for(int i=0;i<3;i++){
                int h=width/2;if(fb[i]==null||fb[i].textureWidth!=width){
                    if(fb[i]!=null)fb[i].delete();fb[i]=new SimpleFramebuffer(width,h,true,MinecraftClient.IS_SYSTEM_MAC);
                    final int ix=i;if(!ready)mc.getTextureManager().registerTexture(ids[i],new AbstractTexture(){public void load(ResourceManager rm){} public int getGlId(){return fb[ix].getColorAttachment();}public void clearGlId(){}});
                    fb[i].setTexFilter(9729);}
                Vec3d mid=center(car,i,delta);Vec3d eye=car.getLerpedPos(delta).add(car.offset(car.driver(mc.player)?.398:-.398,CamaroClient.config.eyeHeight,-.18));
                Vec3d incident=mid.subtract(eye).normalize();Vec3d design=car.offset(i==0?.28:i==1?-.28:0,-.035,-1).normalize();
                Vec3d driverEye=car.getLerpedPos(delta).add(car.offset(.398,1.09,-.18));Vec3d normal=mid.subtract(driverEye).normalize().subtract(design).normalize()
                    .rotateY(-car.mirror(i,0)*MathHelper.RADIANS_PER_DEGREE);
                Vec3d right=car.offset(1,0,0);Vector3f nn=normal.toVector3f().rotateAxis(car.mirror(i,1)*MathHelper.RADIANS_PER_DEGREE,(float)right.x,0,(float)right.z);normal=new Vec3d(nn);
                Vec3d ray=incident.subtract(normal.multiply(2*incident.dotProduct(normal))).normalize();
                RearCamera camera=new RearCamera();camera.update(mc.world,mc.player,true,false,delta);camera.pose(mid.add(ray.multiply(.035)),ray);
                Matrix4f projection=new Matrix4f().setPerspective((float)Math.toRadians(i==2?40:52),2,.045f,Math.min(144,mc.options.getViewDistance().getValue()*16));
                MatrixStack matrices=new MatrixStack();matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw()+180));
                ((BufferAccess)mc).ghostcamaro$buffer(fb[i]);fb[i].setClearColor(.44f,.58f,.72f,1);fb[i].clear(MinecraftClient.IS_SYSTEM_MAC);fb[i].beginWrite(true);
                mc.gameRenderer.loadProjectionMatrix(projection);mc.worldRenderer.setupFrustum(matrices,camera.getPos(),projection);
                mc.worldRenderer.render(matrices,delta,System.nanoTime(),false,camera,mc.gameRenderer,mc.gameRenderer.getLightmapTextureManager(),projection);
            }ready=true;
        }finally{((BufferAccess)mc).ghostcamaro$buffer(main);main.beginWrite(true);RenderSystem.setProjectionMatrix(prev,sorter);rendering=false;}
    }
}
