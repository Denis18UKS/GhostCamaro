package dev.ghostcamaro;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.*;

/** Appearance only. Fifteen base positions plus twelve upper positions fit the measured boot. */
public final class CargoDisplay {
    public enum Kind { FOOD, MEDICAL, TOOLS, FUEL, PARCEL }
    public static final int COLUMNS=5, ROWS=3, BASE=COLUMNS*ROWS;
    private CargoDisplay() {}

    public static Kind kind(ItemStack stack) {
        Item item=stack.getItem();
        if(item==GhostCamaro.CAN)return Kind.FUEL;
        if(item instanceof PotionItem || item==Items.GOLDEN_APPLE || item==Items.ENCHANTED_GOLDEN_APPLE || item==Items.HONEY_BOTTLE || item==Items.MILK_BUCKET)return Kind.MEDICAL;
        if(item==GhostCamaro.KIT || item instanceof ToolItem || item==Items.SHEARS || item==Items.FLINT_AND_STEEL || item==Items.FISHING_ROD)return Kind.TOOLS;
        return stack.isFood()?Kind.FOOD:Kind.PARCEL;
    }

    // Five occupancy counts, six bits each. No item IDs, NBT or mutable stacks go to observers.
    public static int summarize(Inventory inventory) {
        int packed=0;
        for(int slot=0;slot<inventory.size();slot++){
            ItemStack stack=inventory.getStack(slot);
            if(!stack.isEmpty())packed+=1<<(6*kind(stack).ordinal());
        }
        return packed;
    }
    public static int count(int packed,Kind k){return (packed >>> (6*k.ordinal()))&63;}
    public static double x(int i){return 1.78+(i%BASE/COLUMNS)*.215;}
    public static double z(int i){return ((i%COLUMNS)-2)*.26;}
    public static double y(int i){return .509+(i/BASE)*.132;}
}
