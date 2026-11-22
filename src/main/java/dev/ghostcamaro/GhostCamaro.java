package dev.ghostcamaro;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.registry.*;
import net.minecraft.server.command.*;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
public class GhostCamaro implements ModInitializer {
    public static Identifier id(String n){return new Identifier("ghostcamaro",n);}
    public static final Settings CONFIG=Settings.load("ghostcamaro.json",new Settings(),Settings.class);
    public static final EntityType<GhostCamaroEntity> CAR=Registry.register(Registries.ENTITY_TYPE,id("ghost_camaro"),
        EntityType.Builder.<GhostCamaroEntity>create(GhostCamaroEntity::new,SpawnGroup.MISC).setDimensions(1.92f,1.30f).maxTrackingRange(12).trackingTickInterval(1).build("ghost_camaro"));
    public static final Item CAR_ITEM=item("ghost_camaro",new Item(new Item.Settings().maxCount(1)){
        @Override public ActionResult useOnBlock(ItemUsageContext c){
            if(c.getWorld().isClient)return ActionResult.SUCCESS;
            GhostCamaroEntity e=new GhostCamaroEntity(CAR,c.getWorld());Vec3d p=c.getHitPos();
            e.refreshPositionAndAngles(p.x,p.y+.02,p.z,c.getPlayerYaw(),0);
            if(!CarHull.clear(e)||!c.getWorld().spawnEntity(e))return ActionResult.FAIL;
            if(c.getPlayer()!=null&&!c.getPlayer().getAbilities().creativeMode)c.getStack().decrement(1);return ActionResult.CONSUME;
        }});
    public static final Item CAN=item("fuel_can",new Item(new Item.Settings().maxCount(1))), KIT=item("repair_kit",new Item(new Item.Settings().maxCount(16)));
    public static final Item PLATE=item("armor_panel",new Item(new Item.Settings())),WRENCH=item("workshop_key",new Item(new Item.Settings().maxCount(1)));
    public static final Item PISTOL_AMMO=item("pistol_rounds",new Item(new Item.Settings())),RIFLE_AMMO=item("rifle_rounds",new Item(new Item.Settings()));
    public static final Item PISTOL=item("range_pistol",new RangeItem(false)),RIFLE=item("range_rifle",new RangeItem(true));
    static Item item(String n,Item i){return Registry.register(Registries.ITEM,id(n),i);}
    public static final SoundEvent ENGINE=sound("engine"), START=sound("start"), HORN=sound("horn"), CLICK=sound("click"), BURST=sound("burst");
    static SoundEvent sound(String n){return Registry.register(Registries.SOUND_EVENT,id(n),SoundEvent.of(id(n)));}
    public void onInitialize(){
        RouteWorkshop.init();
        Registry.register(Registries.ITEM_GROUP,id("garage"),FabricItemGroup.builder().displayName(Text.translatable("itemGroup.ghostcamaro")).icon(()->new ItemStack(CAR_ITEM)).entries((c,e)->{e.add(CAR_ITEM);e.add(CAN);e.add(KIT);for(var i:new Item[]{PLATE,WRENCH,PISTOL,RIFLE,PISTOL_AMMO,RIFLE_AMMO})e.add(i);}).build());
        ServerPlayNetworking.registerGlobalReceiver(id("input"),(server,p,h,buf,response)->{int eid=buf.readVarInt(),mask=buf.readUnsignedByte(),act=buf.readUnsignedByte(); int mirror=buf.readUnsignedByte();float yaw=buf.readFloat(),pitch=buf.readFloat();server.execute(()->{
            if(p.getVehicle() instanceof GhostCamaroEntity car && car.getId()==eid&&car.driver(p))car.input(p,mask,act,mirror,yaw,pitch);
        });});
        CommandRegistrationCallback.EVENT.register((d,r,e)->{
            var cmd=CommandManager.literal("ghostcamaro");
            for(String op:new String[]{"spawn","repair","refuel","status"})cmd.then(CommandManager.literal(op).requires(s->op.equals("status")||s.hasPermissionLevel(2)).executes(ctx->{
                var s=ctx.getSource();var player=s.getPlayerOrThrow();var world=s.getWorld();
                if(op.equals("spawn")){var car=new GhostCamaroEntity(CAR,world);var v=player.getPos().add(Vec3d.fromPolar(0,player.getYaw()).multiply(4));car.refreshPositionAndAngles(v.x,v.y,v.z,player.getYaw(),0);if(!CarHull.clear(car))return 0;return world.spawnEntity(car)?1:0;}
                GhostCamaroEntity car=player.getVehicle() instanceof GhostCamaroEntity c?c:world.getEntitiesByClass(GhostCamaroEntity.class,player.getBoundingBox().expand(8),v->true).stream().min(java.util.Comparator.comparingDouble(v->v.squaredDistanceTo(player))).orElse(null);
                if(car==null){s.sendError(Text.translatable("message.ghostcamaro.no_car"));return 0;}
                if(op.equals("repair")) car.repair();if(op.equals("refuel"))car.refuel();s.sendFeedback(()->Text.literal(car.status()),false);return 1;
            }));d.register(cmd);
        });
    }
}
