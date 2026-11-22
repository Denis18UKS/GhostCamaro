package dev.ghostcamaro;
import net.minecraft.entity.player.*;
import net.minecraft.inventory.*;
import net.minecraft.item.*;
import net.minecraft.screen.*;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
public final class WorkshopMenu extends GenericContainerScreenHandler {
    private final GhostCamaroEntity car;
    WorkshopMenu(int id,PlayerInventory inv,GhostCamaroEntity car){
        super(ScreenHandlerType.GENERIC_9X1,id,inv,new SimpleInventory(9),1);this.car=car;refresh();
    }
    void refresh(){
        for(int i=0;i<3;i++){ItemStack icon=new ItemStack(car.hasModule(i)?Items.IRON_BLOCK:GhostCamaro.PLATE);
            icon.setCustomName(Text.translatable("ghostcamaro.module."+i).append(Text.literal(car.hasModule(i)?" [−]":" [+]")));getInventory().setStack(i,icon);}
        getInventory().setStack(8,new ItemStack(Items.PAPER).setCustomName(Text.literal(car.buildStats())));
    }
    public boolean canUse(PlayerEntity p){return car.isAlive()&&car.squaredDistanceTo(p)<25&&Math.abs(car.speed())<.012&&!car.flag(1);}
    public void onSlotClick(int slot,int button,SlotActionType type,PlayerEntity player){
        if(type!=SlotActionType.PICKUP||slot<0||slot>2||!canUse(player))return;
        if(car.hasModule(slot)){car.module(slot,false);if(!player.isCreative())player.getInventory().offerOrDrop(new ItemStack(GhostCamaro.PLATE));}
        else{if(!player.isCreative()){int pos=player.getInventory().getSlotWithStack(new ItemStack(GhostCamaro.PLATE));if(pos<0)return;player.getInventory().removeStack(pos,1);}car.module(slot,true);}
        refresh();sendContentUpdates();
    }
    public ItemStack quickMove(PlayerEntity p,int slot){return ItemStack.EMPTY;}
}
