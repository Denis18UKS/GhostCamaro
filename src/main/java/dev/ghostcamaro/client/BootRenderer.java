package dev.ghostcamaro.client;

import dev.ghostcamaro.*;
import net.minecraft.client.model.*;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;

/** Shared baked geometry, indexed by the five fields in the tracked cargo summary. */
final class BootRenderer {
    private record Parcel(ModelPart mesh, Identifier skin){}
    private static final CargoDisplay.Kind[] KINDS=CargoDisplay.Kind.values();
    private static final Parcel[] MODELS=new Parcel[KINDS.length];
    static {
        for(var kind:KINDS){
            ModelData model=new ModelData();
            ModelPartBuilder b=ModelPartBuilder.create();
            switch(kind){
                case FUEL -> {
                    b.uv(0,0).cuboid(-3,0,-4,6,4,8);
                    b.uv(32,0).cuboid(-2.6f,4,-3.6f,2,1,2);
                    for(int z:new int[]{0,3})b.uv(32,0).cuboid(-1,4,z-1,2,1,1);
                    b.uv(32,0).cuboid(-1,5,-1,2,1,4);
                }
                case TOOLS,MEDICAL -> {
                    b.uv(0,0).cuboid(-4,0,-5,8,4,10);
                    b.uv(0,18).cuboid(-4.1f,4,-5.1f,8.2f,1,10.2f);
                    for(int z:new int[]{-3,2})b.uv(32,0).cuboid(3.93f,2,z,0.3f,2,1);
                }
                default -> b.uv(0,0).cuboid(-4,0,-5,8,5,10);
            }
            model.getRoot().addChild("package",b,ModelTransform.NONE);
            var part=TexturedModelData.of(model,64,32).createModel().getChild("package");
            MODELS[kind.ordinal()]=new Parcel(part,GhostCamaro.id("textures/entity/load_"+kind.name().toLowerCase(java.util.Locale.ROOT)+".png"));
        }
    }
    static void draw(GhostCamaroEntity car,float dt,MatrixStack m,VertexConsumerProvider buffers,int light){
        int packed=car.cargoDisplay();
        if(packed==0 || car.openings[3]<=.005f)return;
        float pitch=MathHelper.lerp(dt,car.lastLoadPitch,car.loadPitch),roll=MathHelper.lerp(dt,car.lastLoadRoll,car.loadRoll);
        int i=0;
        for(var kind:KINDS){
            int count=CargoDisplay.count(packed,kind);
            if(count==0)continue;
            Parcel parcel=MODELS[kind.ordinal()];var out=buffers.getBuffer(RenderLayer.getEntityCutoutNoCull(parcel.skin()));
            for(int j=0;j<count;j++,i++){
                m.push();m.translate(CargoDisplay.x(i),CargoDisplay.y(i),CargoDisplay.z(i));
                m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((i*7%5-2)*1.2f));
                m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(pitch));m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(roll));
                m.scale(.33f,.33f,.33f);
                parcel.mesh().render(m,out,light,OverlayTexture.DEFAULT_UV);
                m.pop();
            }
        }
    }
}
