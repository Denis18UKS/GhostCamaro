package dev.ghostcamaro.mixin;
import dev.ghostcamaro.*;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.*;
import net.minecraft.world.EntityView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.*;
@Mixin(EntityView.class)
interface HullContacts {
    @Inject(method="getEntityCollisions",at=@At("RETURN"),cancellable=true)
    private void ghostcamaro$contacts(Entity mover,Box region,CallbackInfoReturnable<List<VoxelShape>> result){
        var original=result.getReturnValue();if(original.isEmpty())return;
        var view=(EntityView)this;
        var cars=view.getEntitiesByClass(GhostCamaroEntity.class,region.expand(1e-7),c->c!=mover);
        if(cars.isEmpty())return;
        List<VoxelShape> shapes=new ArrayList<>(original);
        for(var car:cars){
            if(!shapes.removeIf(shape->shape.getBoundingBox().equals(car.getBoundingBox())))continue;
            for(Box box:CarHull.pieces(car))if(box.intersects(region.expand(1e-7)))shapes.add(VoxelShapes.cuboid(box));
        }
        result.setReturnValue(shapes);
    }
}
