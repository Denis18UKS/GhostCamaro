package dev.ghostcamaro.client;

import dev.ghostcamaro.*;
import java.util.*;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.nbt.*;
import net.minecraft.util.Identifier;
import org.joml.Vector3f;

/** Attach marks to the original triangles. Surface lookup runs only when the tracked marks change. */
final class DamageRenderer {
    private record Mark(int node,Vector3f center,Vector3f normal,boolean glass,float radius){}
    private record Cache(NbtCompound revision,Map<Integer,List<Mark>> groups){}
    private final Map<GhostCamaroEntity,Cache> vehicles=new WeakHashMap<>();
    private final CarRenderer.Scene scene;
    private final Vector3f[] offsets;
    private static final Identifier CRACK=GhostCamaro.id("textures/entity/crack.png"),IMPACT=GhostCamaro.id("textures/entity/impact.png");
    DamageRenderer(CarRenderer.Scene scene){
        this.scene=scene;offsets=new Vector3f[scene.nodes.length];
        for(int root:scene.roots)offsets(root,new Vector3f());
    }
    private void offsets(int index,Vector3f parent){
        var n=scene.nodes[index];offsets[index]=new Vector3f(parent).add(n.translation[0],n.translation[1],n.translation[2]);
        for(int child:n.children)offsets(child,offsets[index]);
    }
    Map<Integer,List<Mark>> prepare(GhostCamaroEntity car){
        var state=car.damageState();var old=vehicles.get(car);
        if(old!=null&&old.revision==state)return old.groups;
        Map<Integer,List<Mark>> groups=new HashMap<>();var list=car.marks();
        for(int i=0;i<list.size();i++){
            var m=list.getCompound(i);var hit=locate(m);
            if(hit!=null)groups.computeIfAbsent(hit.node,k->new ArrayList<>()).add(hit);
        }
        vehicles.put(car,new Cache(state,groups));return groups;
    }
    private static boolean accepts(String zone,String name){
        if(zone.startsWith("Tire"))return name.startsWith("Tyre")||name.startsWith("Moulded_sidewall")||name.startsWith("Tread_");
        if(zone.startsWith("Mirror"))return name.startsWith("Mirror_glass");
        if(zone.startsWith("Lamp"))return name.startsWith("Headlamp_insert");
        if(zone.startsWith("Windshield"))return name.equals("Curved_windscreen");
        if(zone.equals("Hood"))return name.toLowerCase(Locale.ROOT).contains("hood")||name.startsWith("Bonnet");
        if(zone.equals("Armor"))return name.startsWith("Side_Plate");
        if(zone.equals("Bumper"))return name.startsWith("Blade_wing")||name.toLowerCase(Locale.ROOT).contains("bumper")||name.startsWith("Fascia");
        return name.startsWith("Door_")||name.startsWith("Quarter")||name.startsWith("Front_fender")||name.startsWith("Roof_pressing")||name.startsWith("Decklid")||name.startsWith("Sail_pillar");
    }
    private Mark locate(NbtCompound tag){
        String zone=tag.getString("Zone");if(zone.startsWith("Window"))return null;
        Vector3f target=new Vector3f(-tag.getFloat("Z"),tag.getFloat("Y"),tag.getFloat("X"));
        boolean glass=zone.equals("Windshield")||zone.startsWith("Mirror");
        float best=Float.POSITIVE_INFINITY;Mark result=null;
        Vector3f a=new Vector3f(),b=new Vector3f(),c=new Vector3f(),q=new Vector3f();
        for(int i=0;i<scene.nodes.length;i++){
            var node=scene.nodes[i];if(node.mesh<0||!accepts(zone,node.name))continue;
            Vector3f local=new Vector3f(target).sub(offsets[i]);
            for(var part:scene.meshes[node.mesh])for(int j=0;j<part.count*8;j+=24){
                float[] d=part.data;a.set(d[j],d[j+1],d[j+2]);b.set(d[j+8],d[j+9],d[j+10]);c.set(d[j+16],d[j+17],d[j+18]);
                org.joml.Intersectionf.findClosestPointOnTriangle(a,b,c,local,q);
                float dist=q.distanceSquared(local);if(dist>=best||!Float.isFinite(dist))continue;
                best=dist;Vector3f normal=new Vector3f(d[j+5],d[j+6],d[j+7]).normalize();
                result=new Mark(i,new Vector3f(q),normal,glass,zone.startsWith("Mirror")?.029f:glass?.07f:.026f);
            }
        }
        return result;
    }
    static void drawNode(int i,Map<Integer,List<Mark>> all,MatrixStack m,VertexConsumerProvider vp,int light){
        var marks=all.get(i);if(marks==null)return;
        for(var mark:marks){
            Vector3f normal=mark.normal,up=Math.abs(normal.y)>.92f?new Vector3f(1,0,0):new Vector3f(0,1,0);
            Vector3f tangent=up.cross(normal,new Vector3f()).normalize(),bitangent=normal.cross(tangent,new Vector3f());
            var out=vp.getBuffer(RenderLayer.getEntityCutoutNoCull(mark.glass?CRACK:IMPACT));
            var entry=m.peek();var p=new Vector3f();
            for(int j=0;j<4;j++){
                float u=j==0||j==3?0:1,v=j<2?0:1;
                p.set(mark.center).fma(.0025f,normal).fma((u*2-1)*mark.radius,tangent).fma((1-2*v)*mark.radius,bitangent);
                out.vertex(entry.getPositionMatrix(),p.x,p.y,p.z).color(255,255,255,255).texture(u,v).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry.getNormalMatrix(),normal.x,normal.y,normal.z).next();
            }
        }
    }
    void drawNode(GhostCamaroEntity car,int i,MatrixStack m,VertexConsumerProvider vp,int light){
        var cache=vehicles.get(car);if(cache!=null)drawNode(i,cache.groups,m,vp,light);
    }
}
