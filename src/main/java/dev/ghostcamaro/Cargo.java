package dev.ghostcamaro;
import net.minecraft.inventory.*;
import net.minecraft.item.*;
import net.minecraft.util.collection.*;
import java.util.function.Predicate;
/** Main-server-thread API. Updates one authoritative inventory; copies never escape as mutable slot references. */
public final class Cargo {
    private Cargo(){}
    public static int count(GhostCamaroEntity car,Predicate<ItemStack> accepted){int total=0;for(int i=0;i<27;i++){ItemStack s=car.cargo.getStack(i);if(!s.isEmpty()&&accepted.test(s.copy()))total+=s.getCount();}return total;}
    public static boolean exchange(GhostCamaroEntity car,Predicate<ItemStack> cost,int amount,ItemStack reward){
        if(car.getWorld().isClient||!car.getWorld().getServer().isOnThread()||amount<0)return false;
        SimpleInventory next=new SimpleInventory(27);
        for(int i=0;i<27;i++)next.setStack(i,car.cargo.getStack(i).copy());
        int left=amount;
        for(int i=0;i<27&&left>0;i++){ItemStack s=next.getStack(i);if(!s.isEmpty()&&cost.test(s.copy()))left-=next.removeStack(i,Math.min(s.getCount(),left)).getCount();}
        if(left!=0||!next.addStack(reward.copy()).isEmpty())return false;
        car.cargo.replace(next);return true;
    }
    public static void listen(GhostCamaroEntity car,InventoryChangedListener listener){car.cargo.addListener(listener);}
    public static void unlisten(GhostCamaroEntity car,InventoryChangedListener listener){car.cargo.removeListener(listener);}
}
