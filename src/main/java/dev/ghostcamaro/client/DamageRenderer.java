package dev.ghostcamaro.client;
import dev.ghostcamaro.*;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.*;
final class DamageRenderer {
    static void draw(GhostCamaroEntity car,MatrixStack m,VertexConsumerProvider vp,int light){
        var marks=car.marks();
        for(int i=0;i<marks.size();i++){
            var n=marks.getCompound(i);float lateral=n.getFloat("X"),h=n.getFloat("Y"),x=-n.getFloat("Z"),sgn=lateral>=0?1:-1;
            String zone=n.getString("Zone");boolean glass=zone.equals("Windshield")||zone.startsWith("Mirror");
            if(zone.startsWith("Window"))continue;
            float yaw=sgn>0?0:180,pitch=0;
            if(zone.equals("Windshield")){h=MathHelper.clamp(h,.88f,1.20f);x=-.64f+(h-.845f)*1.48f-.008f;yaw=-90;pitch=-55;}
            else if(zone.startsWith("Mirror")){x=-.424f;h=.907f;lateral=sgn*.953f;yaw=90;}
            else if(zone.startsWith("Lamp")){x=-2.37f;h=.65f;lateral=sgn*.73f;yaw=-90;}
            else if(zone.equals("Hood")){h=.85f;pitch=-90;}
            else if(zone.equals("Bumper")){yaw=x<0?-90:90;}
            else if(Math.abs(lateral)>.57){lateral=sgn*(zone.startsWith("Tire")?.912f:.928f);}
            else{pitch=-90;}
            m.push();m.translate(x,h,lateral);m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw));m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
            float size=zone.startsWith("Mirror")?.04f:glass?.105f:.045f;
            var v=vp.getBuffer(RenderLayer.getEntityCutoutNoCull(GhostCamaro.id("textures/entity/"+(glass?"crack":"impact")+".png")));var e=m.peek();
            for(int j=0;j<4;j++){float u=j==0||j==3?0:1,t=j<2?0:1;v.vertex(e.getPositionMatrix(),(u*2-1)*size,(1-t*2)*size,.004f).color(255,255,255,255).texture(u,t).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(e.getNormalMatrix(),0,0,1).next();}
            m.pop();
        }
    }
}
