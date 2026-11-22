package dev.ghostcamaro;

import java.util.*;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.*;
import net.minecraft.block.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.*;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.hit.*;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.*;
import net.minecraft.world.*;

public final class RouteWorkshop {
    static final String NAMES="old_concrete cracked_concrete cream_plaster ochre_plaster faded_blue_plaster damaged_plaster brick_wall chipped_brick old_window broken_window service_door balcony_rail asphalt worn_asphalt cracked_asphalt lane_white lane_yellow kerb pothole road_sign brick_rubble concrete_chunk rubble exposed_rebar broken_wall debris sandbags concrete_barrier road_barrier wood_crate aid_box medical_case pallet barrel fuel_pump jerry_can generator toolbox field_radio mechanic_bench";
    static final List<Item> catalog=new ArrayList<>();
    static Block pump;static BlockEntityType<Reserve> reserve;
    public static void init(){
        for(String name:NAMES.split(" ")){
            Block block=name.equals("fuel_pump")?new Pump():new Prop(name);
            Registry.register(Registries.BLOCK,GhostCamaro.id(name),block);
            catalog.add(GhostCamaro.item(name,new BlockItem(block,new Item.Settings())));
            if(name.equals("fuel_pump"))pump=block;
        }
        reserve=Registry.register(Registries.BLOCK_ENTITY_TYPE,GhostCamaro.id("pump_reserve"),FabricBlockEntityTypeBuilder.create(Reserve::new,pump).build());
        Registry.register(Registries.ITEM_GROUP,GhostCamaro.id("route_1990s"),FabricItemGroup.builder().displayName(Text.literal("GHOST ROUTE | 1990s")).icon(()->new ItemStack(pump)).entries((ctx,out)->catalog.forEach(out::add)).build());
    }
    static class Prop extends HorizontalFacingBlock {
        final String name;final VoxelShape[] shapes=new VoxelShape[4];
        Prop(String n){super(AbstractBlock.Settings.copy(Blocks.STONE).strength(1.8f).nonOpaque());name=n;setDefaultState(getDefaultState().with(Properties.HORIZONTAL_FACING,Direction.NORTH));
            double[] b=box(n);for(Direction dir:Direction.Type.HORIZONTAL){double x=b[0],z=b[2],X=b[3],Z=b[5];int turns=(dir.getHorizontal()+2)%4;
                for(int i=0;i<turns;i++){double next=16-Z,nextMax=16-z;z=x;Z=X;x=next;X=nextMax;}
                shapes[dir.getHorizontal()]=Block.createCuboidShape(x,b[1],z,X,b[4],Z);}}
        static double[] box(String n){return switch(n){
            case "old_window","broken_window","service_door","balcony_rail","broken_wall"->new double[]{0,0,6,16,16,10};
            case "kerb","pallet","pothole","debris","rubble","concrete_chunk","brick_rubble","exposed_rebar"->new double[]{0,0,1,16,4,15};
            case "sandbags","road_barrier","concrete_barrier"->new double[]{0,0,4,16,11,12};
            case "toolbox","field_radio","medical_case","aid_box"->new double[]{2,0,3,14,8,13};
            case "fuel_pump","jerry_can","barrel","road_sign"->new double[]{3,0,3,13,16,13};
            case "mechanic_bench","wood_crate","generator"->new double[]{1,0,1,15,13,15};
            default->new double[]{0,0,0,16,16,16};};}
        protected void appendProperties(StateManager.Builder<Block,BlockState> b){b.add(Properties.HORIZONTAL_FACING);}
        public BlockState getPlacementState(ItemPlacementContext ctx){return getDefaultState().with(Properties.HORIZONTAL_FACING,ctx.getHorizontalPlayerFacing().getOpposite());}
        public VoxelShape getOutlineShape(BlockState state,BlockView world,BlockPos pos,ShapeContext ctx){return shapes[state.get(Properties.HORIZONTAL_FACING).getHorizontal()];}
        public BlockState rotate(BlockState s,BlockRotation r){return s.with(Properties.HORIZONTAL_FACING,r.rotate(s.get(Properties.HORIZONTAL_FACING)));}
    }
    static class Pump extends Prop implements BlockEntityProvider {
        Pump(){super("fuel_pump");}public BlockEntity createBlockEntity(BlockPos p,BlockState s){return new Reserve(p,s);}
        public ActionResult onUse(BlockState state,World world,BlockPos pos,PlayerEntity player,Hand hand,BlockHitResult hit){
            if(!world.isClient&&world.getBlockEntity(pos) instanceof Reserve r){
                var car=world.getEntitiesByClass(GhostCamaroEntity.class,new Box(pos).expand(5),c->c.serviceReady()).stream().min(Comparator.comparingDouble(c->c.squaredDistanceTo(Vec3d.ofCenter(pos)))).orElse(null);
                if(car!=null&&r.liters>0)car.startPump(player,pos);
                player.sendMessage(Text.translatable(car==null?"ghostcamaro.pump_wait":"ghostcamaro.pump_stock",Math.round(r.liters)),true);}
            return ActionResult.SUCCESS;}
    }
    public static class Reserve extends BlockEntity {
        float liters=120;Reserve(BlockPos pos,BlockState s){super(reserve,pos,s);}
        float take(float requested){float amount=Math.min(requested,liters);liters-=amount;markDirty();return amount;}
        public void readNbt(NbtCompound n){super.readNbt(n);if(n.contains("Liters"))liters=MathHelper.clamp(n.getFloat("Liters"),0,120);}
        protected void writeNbt(NbtCompound n){super.writeNbt(n);n.putFloat("Liters",liters);}
    }
}
