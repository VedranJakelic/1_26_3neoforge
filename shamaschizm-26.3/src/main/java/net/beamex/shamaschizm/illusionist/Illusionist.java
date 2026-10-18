package net.beamex.shamaschizm.illusionist;

import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.network.syncher.*;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Persistent single-use trader. Movement and combat AI remain disabled. */
public final class Illusionist extends PathfinderMob implements net.minecraft.world.item.trading.Merchant {
 private static final EntityDataAccessor<Integer> LOOK_TARGET=SynchedEntityData.defineId(Illusionist.class,EntityDataSerializers.INT);
 private static final EntityDataAccessor<Boolean> TALKING=SynchedEntityData.defineId(Illusionist.class,EntityDataSerializers.BOOLEAN);
 private float headYaw,oldHeadYaw,headPitch,oldHeadPitch,talkBlend,oldTalkBlend,talkClock,oldTalkClock;
 private boolean wasTalking,manualTalking,hasSeenPlayer;
 private Player tradingPlayer;
 private final net.minecraft.world.item.trading.MerchantOffers tradeOffers=new net.minecraft.world.item.trading.MerchantOffers();
 private boolean tradeCompleted,departurePending;
 private int departureRemaining;
 private static final EntityDataAccessor<Long> DEPARTURE_START=SynchedEntityData.defineId(Illusionist.class,EntityDataSerializers.LONG);
 // choice.ogg: 13.743061 seconds, rounded up to the next 20-Hz server tick.
 public static final int CHOICE_DURATION_TICKS=275;
 private static final EntityDataAccessor<Long> FIRST_SPIN_END=SynchedEntityData.defineId(Illusionist.class,EntityDataSerializers.LONG);
 public static final int FIRST_SPIN_TICKS=48;
 private int greetingTicks,greetingCooldown;
 private final java.util.Set<java.util.UUID> greetedNearby=new java.util.HashSet<>();
 public static final double GREETING_RADIUS=4, GREETING_RESET_RADIUS=8;
 public static final int GREETING_DURATION_TICKS=265, GREETING_COOLDOWN_TICKS=600;
 @Override protected void defineSynchedData(SynchedEntityData.Builder builder){super.defineSynchedData(builder);builder.define(LOOK_TARGET,-1);builder.define(TALKING,false);builder.define(FIRST_SPIN_END,-1L);builder.define(DEPARTURE_START,-1L);}
 public Illusionist(EntityType<? extends Illusionist> type, Level level) {
  super(type,level);setNoAi(true);setNoGravity(true);setSilent(true);setPersistenceRequired();
 }
 public static AttributeSupplier.Builder attributes(){return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,20).add(Attributes.MOVEMENT_SPEED,0);}
 @Override public void tick(){
  super.tick();
  if(!level().isClientSide()){
   if(tradeCompleted){tickDeparture();return;}
   if(greetingTicks>0)greetingTicks--;
   if(greetingCooldown>0)greetingCooldown--;
   if(tickCount%5==0)checkGreeting();
   entityData.set(TALKING,manualTalking||greetingTicks>0);
   Entity current=level().getEntity(entityData.get(LOOK_TARGET));
   boolean valid=current instanceof Player p&&p.isAlive()&&!p.isSpectator()&&distanceToSqr(p)<=256;
   if(!valid||(!entityData.get(TALKING)&&tickCount%10==0)){
    Player nearest=level().getNearestPlayer(getX(),getY(),getZ(),16,p->p.isAlive()&&!p.isSpectator());
    entityData.set(LOOK_TARGET,nearest==null?-1:nearest.getId());
    if(nearest==null){manualTalking=false;greetingTicks=0;entityData.set(TALKING,false);}
   }
   Entity firstViewer=level().getEntity(entityData.get(LOOK_TARGET));
   if(!hasSeenPlayer&&firstViewer instanceof Player&&hasLineOfSight(firstViewer)){
    hasSeenPlayer=true;entityData.set(FIRST_SPIN_END,level().getGameTime()+FIRST_SPIN_TICKS);
   }
  }else{
   oldHeadYaw=headYaw;oldHeadPitch=headPitch;
   Entity target=level().getEntity(entityData.get(LOOK_TARGET));float yaw=0,pitch=0;
   if(target instanceof Player){
    Vec3 d=target.getEyePosition().subtract(getX(),getY()+1.96,getZ());
    float worldYaw=(float)Math.toDegrees(Math.atan2(-d.x,d.z));
    yaw=getYRot()-worldYaw;
    pitch=Mth.clamp((float)Math.toDegrees(Math.atan2(d.y,Math.sqrt(d.x*d.x+d.z*d.z))),-45,45);
   }
   // Shortest angular route across +/-180, with no humanoid neck/body clamp.
   headYaw+=Mth.clamp(Mth.wrapDegrees(yaw-headYaw)*.18F,-18,18);
   headPitch+=(pitch-headPitch)*.18F;
   oldTalkBlend=talkBlend;oldTalkClock=talkClock;
   boolean talking=entityData.get(TALKING);
   if(talking&&!wasTalking&&talkBlend<.001F){talkClock=0;oldTalkClock=0;}
   talkBlend=Mth.approach(talkBlend,talking?1:0,.075F);
   if(talkBlend>0)talkClock+=.05F;
   wasTalking=talking;
  }
 }
 private void checkGreeting(){
  // Hysteresis prevents repeated greetings from moving back and forth at five blocks.
  var nearby=level().getEntitiesOfClass(Player.class,getBoundingBox().inflate(GREETING_RESET_RADIUS),p->p.isAlive()&&!p.isSpectator()&&distanceToSqr(p)<=GREETING_RESET_RADIUS*GREETING_RESET_RADIUS);
  var present=new java.util.HashSet<java.util.UUID>();for(Player p:nearby)present.add(p.getUUID());
  greetedNearby.retainAll(present);
  nearby.sort(java.util.Comparator.comparingDouble(this::distanceToSqr));
  for(Player player:nearby){
   if(distanceToSqr(player)>GREETING_RADIUS*GREETING_RADIUS||!greetedNearby.add(player.getUUID()))continue;
   if(greetingCooldown>0)continue;
   greetingCooldown=GREETING_COOLDOWN_TICKS;greetingTicks=GREETING_DURATION_TICKS;
   entityData.set(LOOK_TARGET,player.getId());
   // Direct world playback intentionally bypasses the placeholder entity's silent flag.
   level().playSound(null,getX(),getY()+1.8,getZ(),Illusionists.GREETING,net.minecraft.sounds.SoundSource.NEUTRAL,.85F,1F);
  }
 }
 /** One complete smooth roll about the local forward axis through the eye. */
 public float firstSpin(float partial){
  long end=entityData.get(FIRST_SPIN_END);if(end<0)return 0;
  float t=(float)((level().getGameTime()-end+FIRST_SPIN_TICKS)+partial)/FIRST_SPIN_TICKS;
  if(t<=0||t>=1)return 0;
  return 360*t*t*t*(t*(t*6-15)+10);
 }
 @Override protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput out){super.addAdditionalSaveData(out);out.putBoolean("IllusionistHasSeenPlayer",hasSeenPlayer);out.putBoolean("IllusionistTraded",tradeCompleted);}
 @Override protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput in){super.readAdditionalSaveData(in);hasSeenPlayer=in.getBooleanOr("IllusionistHasSeenPlayer",false);entityData.set(FIRST_SPIN_END,-1L);tradeCompleted=in.getBooleanOr("IllusionistTraded",false);departurePending=tradeCompleted;tradeOffers.clear();tradingPlayer=null;entityData.set(DEPARTURE_START,-1L);}
 public float headYaw(float partial){return Mth.lerp(partial,oldHeadYaw,headYaw);}
 public float headPitch(float partial){return Mth.lerp(partial,oldHeadPitch,headPitch);}
 public float talkBlend(float partial){float t=Mth.lerp(partial,oldTalkBlend,talkBlend);return t*t*(3-2*t);}
 public float talkClock(float partial){return Mth.lerp(partial,oldTalkClock,talkClock);}
 @Override protected InteractionResult mobInteract(Player player,InteractionHand hand){
  if(!level().isClientSide()&&player instanceof net.minecraft.server.level.ServerPlayer serverPlayer){
   if(tradeCompleted||!isAlive()||tradingPlayer!=null)return InteractionResult.SUCCESS;
   setTradingPlayer(player);
   var opened=serverPlayer.openMenu(new net.minecraft.world.SimpleMenuProvider(
    (id,inventory,who)->new IllusionistTradeMenu(id,inventory,this),getDisplayName()));
   if(opened.isPresent() && serverPlayer.containerMenu instanceof IllusionistTradeMenu menu)menu.sendOffersWithSoulTooltips();
   if(!(serverPlayer.containerMenu instanceof net.minecraft.world.inventory.MerchantMenu))setTradingPlayer(null);
  }
  return InteractionResult.SUCCESS;
 }
 @Override public void setTradingPlayer(Player player){tradingPlayer=player;}
 @Override public Player getTradingPlayer(){return tradingPlayer;}
 @Override public net.minecraft.world.item.trading.MerchantOffers getOffers(){
  if(tradeOffers.isEmpty()){
   addOffer(net.beamex.shamaschizm.slab.SlabRegistration.SLAB,1,new net.minecraft.world.item.ItemStack(net.beamex.shamaschizm.event.ModItemBootstrap.ANCIENT_BLUEPRINT));
   addOffer(net.beamex.shamaschizm.slab.SlabRegistration.SLAB,1,new net.minecraft.world.item.ItemStack(net.beamex.shamaschizm.event.ModItemBootstrap.SOUL_ICON));
   addOffer(net.beamex.shamaschizm.event.ModItemBootstrap.SOUL_ICON,1,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_BLOCK,9));
   addOffer(net.beamex.shamaschizm.event.ModItemBootstrap.SOUL_ICON,1,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ANCIENT_DEBRIS,16));
   addOffer(net.beamex.shamaschizm.event.ModItemBootstrap.SOUL_ICON,2,enchanted(net.minecraft.world.item.Items.ENCHANTED_BOOK,net.minecraft.world.item.enchantment.Enchantments.FIRE_ASPECT,10,"Embers from the Day of Judgement"));
   addOffer(net.beamex.shamaschizm.event.ModItemBootstrap.SOUL_ICON,1,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GOLDEN_APPLE,64));
   addOffer(net.beamex.shamaschizm.event.ModItemBootstrap.HIGH_SOUL_ICON,1,enchanted(net.minecraft.world.item.Items.ENCHANTED_BOOK,net.minecraft.world.item.enchantment.Enchantments.SWEEPING_EDGE,6,"Book of eclipses"));
   addOffer(net.beamex.shamaschizm.event.ModItemBootstrap.HIGH_SOUL_ICON,1,enchanted(net.minecraft.world.item.Items.ENCHANTED_BOOK,net.minecraft.world.item.enchantment.Enchantments.FORTUNE,6,"Book of moonlight"));
   addOffer(net.beamex.shamaschizm.event.ModItemBootstrap.HIGH_SOUL_ICON,2,enchanted(net.minecraft.world.item.Items.NETHERITE_SWORD,net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENCHANTMENT,net.minecraft.resources.Identifier.fromNamespaceAndPath("shamaschizm","withering")),1,"Sword of Dark Rites"));
   addOffer(net.minecraft.world.item.Items.NETHER_STAR,1,new net.minecraft.world.item.ItemStack(net.beamex.shamaschizm.event.ModItemBootstrap.SOUL_ICON));
   addOffer(net.minecraft.world.item.Items.GHAST_TEAR,7,new net.minecraft.world.item.ItemStack(net.beamex.shamaschizm.event.ModItemBootstrap.SOUL_ICON));
   if(tradeCompleted)for(var offer:tradeOffers)offer.setToOutOfStock();
  }
  return tradeOffers;
 }
 private void addOffer(net.minecraft.world.level.ItemLike cost,int count,net.minecraft.world.item.ItemStack result){
  tradeOffers.add(new net.minecraft.world.item.trading.MerchantOffer(new net.minecraft.world.item.trading.ItemCost(cost,count),result,1,0,0F));
 }
 private net.minecraft.world.item.ItemStack enchanted(net.minecraft.world.item.Item item,net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> key,int level,String name){
  var stack=new net.minecraft.world.item.ItemStack(item);
  var enchantment=level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(key);
  var builder=new net.minecraft.world.item.enchantment.ItemEnchantments.Mutable(net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);
  builder.set(enchantment,level);
  stack.set(item==net.minecraft.world.item.Items.ENCHANTED_BOOK?net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS:net.minecraft.core.component.DataComponents.ENCHANTMENTS,builder.toImmutable());
  stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal(name));
  return stack;
 }
 @Override public void overrideOffers(net.minecraft.world.item.trading.MerchantOffers offers){}
 @Override public void notifyTrade(net.minecraft.world.item.trading.MerchantOffer offer){
  if(level().isClientSide()||tradeCompleted)return;
  // Lock immediately, before vanilla refreshes result slots or repeats a shift-click.
  tradeCompleted=true;for(var entry:tradeOffers)entry.setToOutOfStock();departurePending=true;
  manualTalking=false;greetingTicks=0;entityData.set(TALKING,false);
  entityData.set(FIRST_SPIN_END,-1L);
 }
 @Override public void notifyTradeUpdated(net.minecraft.world.item.ItemStack stack){}
 @Override public int getVillagerXp(){return 0;}
 @Override public void overrideXp(int xp){}
 @Override public boolean showProgressBar(){return false;}
 @Override public boolean canRestock(){return false;}
 @Override public net.minecraft.sounds.SoundEvent getNotifyTradeSound(){return Illusionists.CHOICE;}
 @Override public boolean isClientSide(){return level().isClientSide();}
 @Override public boolean stillValid(Player player){return isAlive()&&!tradeCompleted&&tradingPlayer==player&&distanceToSqr(player)<=64;}
 private void tickDeparture(){
  var server=(net.minecraft.server.level.ServerLevel)level();
  if(departurePending){
   // Run after the completed click so vanilla can settle the result and return unused inputs.
   if(tradingPlayer instanceof net.minecraft.server.level.ServerPlayer player)player.closeContainer();
   tradingPlayer=null;departurePending=false;
   departureRemaining=CHOICE_DURATION_TICKS;
   entityData.set(DEPARTURE_START,level().getGameTime());
   // Prevent the greeting voice from continuing over the choice response.
   for(var listener:server.players())if(distanceToSqr(listener)<1024)
    listener.connection.send(new net.minecraft.network.protocol.game.ClientboundStopSoundPacket(Illusionists.GREETING.location(),net.minecraft.sounds.SoundSource.NEUTRAL));
   level().playSound(null,getX(),getY()+1.8,getZ(),Illusionists.CHOICE,net.minecraft.sounds.SoundSource.NEUTRAL,.85F,1F);
   return;
  }
  if(--departureRemaining<=0){
   server.sendParticles(net.minecraft.core.particles.ParticleTypes.WHITE_SMOKE,getX(),getY()+1.2,getZ(),90,.55,.85,.55,.045);
   server.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,getX(),getY()+1.2,getZ(),30,.45,.7,.45,.025);
   discard();
  }
 }
 public float choiceRoll(float partial){
  long start=entityData.get(DEPARTURE_START);if(start<0)return 0;
  float seconds=(level().getGameTime()-start+partial)/20F;
  // Turn over, pause upside down, then return upright; fit inside shorter recordings too.
  float duration=Math.min(2.4F,Math.max(.1F,CHOICE_DURATION_TICKS/20F*.8F));
  float t=Math.clamp(seconds/duration,0,1);
  float u=t<.4F?t/.4F:t<.6F?1:(1-t)/.4F;
  return 180*u*u*(3-2*u);
 }
 @Override protected void registerGoals(){}
 @Override public void travel(Vec3 input){setDeltaMovement(Vec3.ZERO);}
 @Override public boolean isPushable(){return false;}
 @Override public void push(double x,double y,double z){}
 @Override public void checkDespawn(){}
 @Override public boolean removeWhenFarAway(double distance){return false;}
}
