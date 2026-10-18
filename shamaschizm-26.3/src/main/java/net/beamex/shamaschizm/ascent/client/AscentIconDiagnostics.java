package net.beamex.shamaschizm.ascent.client;
import com.mojang.logging.LogUtils;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
/** Temporary read-only diagnostics: each distinct trimmed item is logged once,
 * up to twelve items per session. Does not change the inventory or item model. */
@EventBusSubscriber(modid="shamaschizm",value=Dist.CLIENT)
public final class AscentIconDiagnostics {
 private static final Set<String> SEEN=new HashSet<>();
 private static int ticks;
 @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
  var mc=Minecraft.getInstance();
  if(mc.player==null || ++ticks%40!=0 || SEEN.size()>=12)return;
  for(var slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET,EquipmentSlot.MAINHAND,EquipmentSlot.OFFHAND}) {
   var stack=mc.player.getItemBySlot(slot);var trim=stack.get(DataComponents.TRIM);
   if(stack.isEmpty() || trim==null || SEEN.size()>=12)continue;
   var model=stack.get(DataComponents.ITEM_MODEL);
   var key=BuiltInRegistries.ITEM.getKey(stack.getItem())+" model="+model+" material="+trim.material().unwrapKey()+" pattern="+trim.pattern().value().assetId();
   if(!SEEN.add(key))continue;
   LogUtils.getLogger().info("[Ascent icon diagnostic v3] {} slot={} components={}",key,slot,stack.getComponents());
   if(model==null)continue;
   var definition=model.withPath("items/"+model.getPath()+".json");
   var resource=mc.getResourceManager().getResource(definition);
   if(resource.isEmpty())LogUtils.getLogger().warn("[Ascent icon diagnostic v3] Missing item definition: {}",definition);
   else try(var stream=resource.get().open()) {
    LogUtils.getLogger().info("[Ascent icon diagnostic v3] Active definition {}: {}",definition,new String(stream.readAllBytes(),StandardCharsets.UTF_8));
   }catch(Exception e){LogUtils.getLogger().warn("[Ascent icon diagnostic v3] Could not read {}",definition,e);}
  }
 }
}
