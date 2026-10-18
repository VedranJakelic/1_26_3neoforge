package net.beamex.shamaschizm.saints;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.beamex.shamaschizm.entity.SkeletonVariantRegistration;
import net.beamex.shamaschizm.entity.custom.CoffinEntity;
import net.beamex.shamaschizm.entity.custom.SkeletonVariantEntity;
import net.beamex.shamaschizm.registry.ModDataComponents;
import net.beamex.shamaschizm.plantedsword.PlantedSwordEntity;
import net.beamex.shamaschizm.plantedsword.PlantedSwords;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.goal.Goal;
import net.beamex.shamaschizm.mixin.SaintMobGoalsAccessor;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.equine.SkeletonHorse;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/** Server-authoritative encounter. Missing/unloaded members never count as dead. */
public final class CatacombEncounterEntity extends Entity {
    private static final Direction[] DIRECTIONS={Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST};
    private final UUID[] coffins=new UUID[4], riders=new UUID[4], horses=new UUID[4];
    private static final Identifier RUSH = Identifier.parse("shamaschizm:saints_rush");
    private static final Identifier VITALITY = Identifier.parse("shamaschizm:saints_vitality");
    private final int[] lastMeleeCycle={-1,-1,-1,-1};
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> HUD_ACTIVE=SynchedEntityData.defineId(CatacombEncounterEntity.class,net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);
    private static final List<net.minecraft.network.syncher.EntityDataAccessor<Float>> HUD_HEALTH=List.of(
            SynchedEntityData.defineId(CatacombEncounterEntity.class,net.minecraft.network.syncher.EntityDataSerializers.FLOAT),
            SynchedEntityData.defineId(CatacombEncounterEntity.class,net.minecraft.network.syncher.EntityDataSerializers.FLOAT),
            SynchedEntityData.defineId(CatacombEncounterEntity.class,net.minecraft.network.syncher.EntityDataSerializers.FLOAT),
            SynchedEntityData.defineId(CatacombEncounterEntity.class,net.minecraft.network.syncher.EntityDataSerializers.FLOAT));
    // 0 spear combat, 1 wind-up, 2 airborne, 3 landing recovery, 4 spear recovery, 5 remount.
    private int maceMode,maceTicks,maceSlams,spearThrowCooldown;
    private UUID thrownSpear;
    private boolean permanentMace;
    private UUID spear;
    private Vec3 spearChargeEnd;
    private final int[] formationSlots={0,1,2,3};
    private double formationAngle;
    private boolean formationAssigned;
    // 0 sealed, 1 opening/ghost approach, 2 spear recovery, 3 battle, 4 rewarded.
    private int phase, phaseTicks, deadMask;
    private boolean initialized;
    private UUID finalBoss,lastSaint;
    private Vec3 finalGround;
    private final java.util.Set<UUID> freedHorses=new java.util.HashSet<>();

    public CatacombEncounterEntity(EntityType<?> type, Level level) {
        super(type,level);this.noPhysics=true;setNoGravity(true);setPermanentlyInvulnerable(true);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder data) {
        data.define(HUD_ACTIVE,false);for(var key:HUD_HEALTH)data.define(key,1.0F);
    }
    public boolean bossHudActive(){return entityData.get(HUD_ACTIVE);}
    public float bossHealth(int role){return Math.max(0,Math.min(1,entityData.get(HUD_HEALTH.get(role))));}
    private void syncBossHud(ServerLevel level){
        entityData.set(HUD_ACTIVE,phase>=1 && phase<=3 && deadMask!=15);
        for(int i=0;i<4;i++){
            if((deadMask&(1<<i))!=0)entityData.set(HUD_HEALTH.get(i),0.0F);
            else if(riders[i]!=null && level.getEntity(riders[i]) instanceof AbstractSkeleton rider)
                entityData.set(HUD_HEALTH.get(i),rider.getHealth()/rider.getMaxHealth());
        }
    }
    @Override public boolean hurtServer(ServerLevel level, DamageSource source,float damage){return false;}

    public static boolean createEncounter(ServerLevel level,Vec3 centre,Player player) {
        CatacombEncounterEntity encounter=CatacombSaints.ENCOUNTER.create(level,EntitySpawnReason.SPAWN_ITEM_USE);
        if(encounter==null)return false;
        encounter.snapTo(centre.x,centre.y,centre.z,0,0);
        return encounter.initializeArena(level,player,true);
    }
    private boolean initializeArena(ServerLevel level,Player player,boolean addController) {
        CatacombEncounterEntity encounter=this;
        Vec3 centre=position();
        List<Entity> staged=new ArrayList<>();
        if(addController)staged.add(encounter);
        for(int i=0;i<4;i++) {
            CoffinEntity coffin=CatacombSaints.COFFIN2.create(level,EntitySpawnReason.EVENT);
            if(coffin==null)return false;
            Vec3 offset=Vec3.atLowerCornerOf(DIRECTIONS[i].getUnitVec3i()).scale(7);
            coffin.snapTo(centre.x+offset.x,centre.y,centre.z+offset.z,0,0);
            coffin.setCoffinFacing(DIRECTIONS[i].getOpposite());
            if(!coffin.canInstallCollisionBlocks() || !level.noCollision(coffin)) {
                if(player!=null)player.sendOverlayMessage(Component.literal("The four coffin positions need clear space, seven blocks from the centre."));
                return false;
            }
            coffin.markSaintsEncounter(encounter.getUUID());
            encounter.coffins[i]=coffin.getUUID();staged.add(coffin);
        }
        PlantedSwordEntity display=PlantedSwords.TYPE.create(level,EntitySpawnReason.EVENT);
        if(display==null)return false;
        display.snapTo(centre.x,centre.y,centre.z,player==null?getYRot():player.getYRot(),0);
        display.configure(spearStack(level),0,0);display.bindSaintsEncounter(encounter.getUUID());
        encounter.spear=display.getUUID();staged.add(display);
        for(Entity entity:staged) {
            if(!level.addFreshEntity(entity)) {
                for(Entity rollback:staged)rollback.discard();
                return false;
            }
        }
        encounter.initialized=true;
        for(Entity entity:staged)if(entity instanceof CoffinEntity coffin)coffin.installCollisionBlocks();
        return true;
    }
    /** Structure templates replace entity UUIDs without rewriting custom UUID references. */
    private boolean relinkSealedArena(ServerLevel level,PlantedSwordEntity requested) {
        if(phase!=0)return false; // Never reconstruct a running or completed fight.
        PlantedSwordEntity display=requested;
        if(display==null) {
            var local=level.getEntitiesOfClass(PlantedSwordEntity.class,getBoundingBox().inflate(2),
                    p->p.saintsEncounterId()!=null && isSaintSpear(p.sword())
                            && p.distanceToSqr(this)<4);
            if(local.size()!=1)return false;
            display=local.get(0);
        }
        if(display.distanceToSqr(this)>=4 || display.saintsEncounterId()==null)return false;
        UUID oldOwner=display.saintsEncounterId();
        List<CoffinEntity> found=new ArrayList<>();
        for(Direction direction:DIRECTIONS) {
            Vec3 expected=display.position().add(Vec3.atLowerCornerOf(direction.getUnitVec3i()).scale(7));
            var candidates=level.getEntitiesOfClass(CoffinEntity.class,
                    new net.minecraft.world.phys.AABB(expected,expected).inflate(1.75),
                    c->c.getType()==CatacombSaints.COFFIN2 && !c.isOpen() && !c.isRemoved()
                            && c.position().distanceToSqr(expected)<3.0625
                            && (oldOwner.equals(CatacombSaints.encounterId(c)) || getUUID().equals(CatacombSaints.encounterId(c))));
            if(candidates.size()!=1)return false;
            found.add(candidates.get(0));
        }
        // Commit only after all four local coffins are present. The old source
        // arena may still exist elsewhere; never follow its UUIDs remotely.
        for(int i=0;i<4;i++){coffins[i]=found.get(i).getUUID();found.get(i).markSaintsEncounter(getUUID());}
        spear=display.getUUID();display.bindSaintsEncounter(getUUID());sharpenSpear(level,display.sword());initialized=true;
        return true;
    }
    public static CatacombEncounterEntity resolveChallenge(ServerLevel level,PlantedSwordEntity display) {
        var local=level.getEntitiesOfClass(CatacombEncounterEntity.class,display.getBoundingBox().inflate(2),
                e->!e.isRemoved() && e.distanceToSqr(display)<4);
        if(local.size()>1)return null; // Ambiguous overlapping arenas must not steal each other's members.
        if(local.size()==1) {
            CatacombEncounterEntity encounter=local.get(0);
            if(encounter.phase!=0)return display.getUUID().equals(encounter.spear)?encounter:null;
            return encounter.relinkSealedArena(level,display)?encounter:null;
        }
        // Some templates omit the invisible controller. Recover it only from a
        // complete, unopened, explicitly marked four-coffin arena around this spear.
        CatacombEncounterEntity recovered=CatacombSaints.ENCOUNTER.create(level,EntitySpawnReason.EVENT);
        if(recovered==null)return null;
        recovered.snapTo(display.getX(),display.getY(),display.getZ(),display.getYRot(),0);
        if(!level.addFreshEntity(recovered))return null;
        if(!recovered.relinkSealedArena(level,display)){recovered.discard();return null;}
        return recovered;
    }
    public void activate(Player player) {
        if(!(level() instanceof ServerLevel level)||phase!=0)return;
        if(!relinkSealedArena(level,null)) {
            player.sendOverlayMessage(Component.literal("The saints' arena is incomplete or not fully loaded. All four sealed coffins must be nearby."));
            return;
        }
        List<AbstractSkeleton> staged=new ArrayList<>();
        for(int i=0;i<4;i++) {
            if(!(level.getEntity(coffins[i]) instanceof CoffinEntity coffin))return;
            AbstractSkeleton rider=switch(i) {
                case 0 -> EntityTypes.STRAY.create(level,EntitySpawnReason.EVENT);
                case 1 -> EntityTypes.BOGGED.create(level,EntitySpawnReason.EVENT);
                case 2 -> EntityTypes.PARCHED.create(level,EntitySpawnReason.EVENT);
                default -> SkeletonVariantRegistration.TYPE.create(level,EntitySpawnReason.EVENT);
            };
            if(rider==null)return;
            Vec3 foot=coffin.position().add(Vec3.atLowerCornerOf(coffin.getCoffinFacing().getOpposite().getUnitVec3i()).scale(1.30)).add(0,.18,0);
            rider.snapTo(foot.x,foot.y,foot.z,coffin.getYRot(),0);
            rider.setPersistenceRequired();rider.getGoalSelector().removeAllGoals(g->true);rider.setNoAi(true);rider.setNoGravity(true);rider.setPermanentlyInvulnerable(true);
            sleepFacing(rider,coffin);rider.getPersistentData().putString(CatacombSaints.ENCOUNTER_TAG,getUUID().toString());
            rider.getPersistentData().putInt(CatacombSaints.ROLE_TAG,i);
            rider.getPersistentData().putBoolean(SkeletonVariantEntity.EQUIPPED_TAG,true);
            if(rider instanceof SkeletonVariantEntity variant)variant.waitInsideCoffin(coffin.getCoffinFacing());
            equip(level,rider,i);
            upgradeSaint(level,rider,i);
            staged.add(rider);
        }
        for(AbstractSkeleton rider:staged)if(!level.addFreshEntity(rider)) {
            for(AbstractSkeleton rollback:staged)rollback.discard();return;
        }
        for(int i=0;i<4;i++) {
            riders[i]=staged.get(i).getUUID();
            ((CoffinEntity)level.getEntity(coffins[i])).openForSaints();
        }
        net.beamex.shamaschizm.experimental.BossGardenLocks.get(level).bind(blockPosition(),getUUID());
        phase=1;phaseTicks=0;
        net.beamex.shamaschizm.saints.audio.ArenaAudio.send(level,getUUID(),position(),true,true);
        player.sendOverlayMessage(Component.literal("The Catacomb Saints answer the challenge."));
    }
    private static ItemStack spearStack(ServerLevel level) {
        ItemStack stack=new ItemStack(net.beamex.shamaschizm.saints.vindication.VindicationRegistration.SPEAR);
        stack.set(DataComponents.CUSTOM_NAME,Component.literal("Witness of Sin"));
        stack.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LUNGE),5);
        stack.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS),3);
        return stack;
    }
    private static void upgradeSaint(ServerLevel level, AbstractSkeleton rider, int role) {
        var health=rider.getAttribute(Attributes.MAX_HEALTH);
        if(health!=null && !health.hasModifier(VITALITY)) {
            float fraction=rider.getHealth()/rider.getMaxHealth();
            health.addPermanentModifier(new AttributeModifier(VITALITY,1.0,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            rider.setHealth(rider.getMaxHealth()*fraction);
        }
        if(role==2) {
            // Kinetic spear damage uses base mob damage, not the held item's attack attribute.
            rider.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(8.0);
            if(isSaintSpear(rider.getMainHandItem())) {
                if(rider.getMainHandItem().is(Items.NETHERITE_SPEAR))rider.setItemSlot(EquipmentSlot.MAINHAND,modernSpear(rider.getMainHandItem()));
                rider.getMainHandItem().set(DataComponents.KINETIC_WEAPON,Items.NETHERITE_SPEAR.components().get(DataComponents.KINETIC_WEAPON));
                sharpenSpear(level,rider.getMainHandItem());
            }
        }
        if(role==1 && rider.getOffhandItem().is(CatacombSaints.LID)) {
            var blocks=rider.getOffhandItem().get(DataComponents.BLOCKS_ATTACKS);
            if(blocks!=null && blocks.blockDelaySeconds()>0)rider.getOffhandItem().set(DataComponents.BLOCKS_ATTACKS,
                    new net.minecraft.world.item.component.BlocksAttacks(0.0F,blocks.disableCooldownScale(),
                            blocks.damageReductions(),blocks.itemDamage(),blocks.bypassedBy(),blocks.blockSound(),blocks.disableSound()));
        }
    }
    private static void sharpenSpear(ServerLevel level, ItemStack stack) {
        var sharpness=level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS);
        if(net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(sharpness,stack)<3)stack.enchant(sharpness,3);
    }
    private static void trim(ServerLevel level,ItemStack stack,String material) {
        var materials=level.registryAccess().lookupOrThrow(Registries.TRIM_MATERIAL);
        var patterns=level.registryAccess().lookupOrThrow(Registries.TRIM_PATTERN);
        stack.set(DataComponents.TRIM,new ArmorTrim(materials.getOrThrow(ResourceKey.create(Registries.TRIM_MATERIAL,Identifier.parse("minecraft:"+material))),
                patterns.getOrThrow(ResourceKey.create(Registries.TRIM_PATTERN,Identifier.parse("minecraft:spire")))));
    }
    private static void equip(ServerLevel level,AbstractSkeleton rider,int role) {
        var ench=level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        EquipmentSlot[] slots={EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
        ItemStack[] armor={new ItemStack(Items.NETHERITE_CHESTPLATE),new ItemStack(Items.NETHERITE_LEGGINGS),new ItemStack(Items.NETHERITE_BOOTS)};
        for(int i=0;i<3;i++) {
            armor[i].set(ModDataComponents.RAISED_ENCHANT_CAP,true);trim(level,armor[i],"gold");
            armor[i].enchant(ench.getOrThrow(switch(level.getRandom().nextInt(4)) {
                case 0 -> Enchantments.PROTECTION;case 1 -> Enchantments.FIRE_PROTECTION;
                case 2 -> Enchantments.BLAST_PROTECTION;default -> Enchantments.PROJECTILE_PROTECTION;
            }),1+level.getRandom().nextInt(2));
            if(level.getRandom().nextBoolean())armor[i].enchant(ench.getOrThrow(Enchantments.UNBREAKING),1+level.getRandom().nextInt(2));
            rider.setItemSlot(slots[i],armor[i]);
        }
        ItemStack helmet=new ItemStack(Items.GOLDEN_HELMET);trim(level,helmet,"redstone");
        helmet.set(DataComponents.CUSTOM_NAME,Component.literal("Pride of the Catacomb Saints"));
        helmet.enchant(ench.getOrThrow(Enchantments.FIRE_PROTECTION),8);helmet.enchant(ench.getOrThrow(Enchantments.UNBREAKING),5);
        rider.setItemSlot(EquipmentSlot.HEAD,helmet);
        ItemStack weapon=switch(role){case 0->new ItemStack(Items.BOW);case 1->new ItemStack(Items.IRON_SWORD);case 3->new ItemStack(Items.GOLDEN_AXE);default->ItemStack.EMPTY;};
        if(role==0){weapon.enchant(ench.getOrThrow(Enchantments.FLAME),1);weapon.enchant(ench.getOrThrow(Enchantments.PUNCH),5);}
        if(role==3)weapon.enchant(ench.getOrThrow(Enchantments.UNBREAKING),9);
        rider.setItemSlot(EquipmentSlot.MAINHAND,weapon);
        // The lid is transferred visibly after opening, not duplicated on the sleeping rider.
        rider.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);
        rider.setDropChance(EquipmentSlot.MAINHAND,role==2?2.0F:.085F);
        rider.setDropChance(EquipmentSlot.OFFHAND,role==1?2.0F:0.0F);
    }
    @Override public void tick() {
        super.tick();
        if(!(level() instanceof ServerLevel level))return;
        syncBossHud(level);
        if(tickCount%20==0 && phase!=0 && phase!=4)net.beamex.shamaschizm.saints.audio.ArenaAudio.send(level,getUUID(),position(),true,false);
        if(phase==0 && tickCount%20==0) {
            boolean repaired=relinkSealedArena(level,null);
            if(!repaired && !initialized) {
                // A bare /summon or dispenser spawn needs the same arena as the egg.
                // Wait for copied pieces/chunks instead of duplicating a partial template.
                boolean pieces=!level.getEntitiesOfClass(CoffinEntity.class,getBoundingBox().inflate(10),CoffinEntity::isSaintsCoffin).isEmpty()
                        || !level.getEntitiesOfClass(PlantedSwordEntity.class,getBoundingBox().inflate(2),p->p.saintsEncounterId()!=null).isEmpty();
                boolean loaded=true;
                for(int x=(blockPosition().getX()-10)>>4;x<=(blockPosition().getX()+10)>>4;x++)
                    for(int z=(blockPosition().getZ()-10)>>4;z<=(blockPosition().getZ()+10)>>4;z++)
                        if(level.getChunkSource().getChunkNow(x,z)==null)loaded=false;
                if(!pieces && loaded)initializeArena(level,null,false);
            }
        }
        if(!initialized || phase==0)return;
        if(phase<4) {
            for(UUID id:horses)if(id!=null && level.getEntity(id) instanceof SkeletonHorse horse)prepareCombatHorse(horse);
            for(int i=0;i<4;i++)if(riders[i]!=null && level.getEntity(riders[i]) instanceof AbstractSkeleton rider && rider.isAlive())upgradeSaint(level,rider,i);
            if(spear!=null && level.getEntity(spear) instanceof PlantedSwordEntity display)sharpenSpear(level,display.sword());
        }
        if(phase==5){transformLastSaint(level);return;}
        if(phase==6){if(SaintsDeathData.get(level).vindicationDefeated(getUUID()))vindicationDefeated();return;}
        if(phase==4){
            for(UUID id:horses)if(id!=null && !freedHorses.contains(id) && level.getEntity(id) instanceof SkeletonHorse horse){freeHorse(horse);freedHorses.add(id);}
            return;
        }
        int reported=SaintsDeathData.get(level).mask(getUUID());
        for(int i=0;i<4;i++)if((reported&(1<<i))!=0 && (deadMask&(1<<i))==0 && riders[i]!=null)
            recordDeath(riders[i]);
        phaseTicks++;
        if(phase==1){approach(level);return;}
        if((deadMask&4)!=0 && spear!=null)dropUnclaimedSpear(level,position());
        if(Integer.bitCount(deadMask)>=3){beginVindication(level);return;}
        if(phase==2){recoverSpear(level);return;}
        battle(level);
    }
    private void approach(ServerLevel level) {
        for(int i=0;i<4;i++) {
            if(level.getEntity(riders[i]) instanceof AbstractSkeleton rider && !rider.isPassenger()
                    && level.getEntity(coffins[i]) instanceof CoffinEntity coffin) {
                Vec3 foot=coffin.position().add(Vec3.atLowerCornerOf(coffin.getCoffinFacing().getOpposite().getUnitVec3i()).scale(1.30)).add(0,.18,0);
                rider.setPos(foot.x,foot.y,foot.z);rider.setDeltaMovement(Vec3.ZERO);sleepFacing(rider,coffin);
            }
        }
        if(phaseTicks<80)return;
        boolean allMounted=true;
        for(int i=0;i<4;i++) {
            if(!(level.getEntity(riders[i]) instanceof AbstractSkeleton rider)
                    || !(level.getEntity(coffins[i]) instanceof CoffinEntity coffin)){allMounted=false;continue;}
            SkeletonHorse horse=horses[i]==null?null:(level.getEntity(horses[i]) instanceof SkeletonHorse h?h:null);
            if(horse==null && horses[i]==null) {
                horse=EntityTypes.SKELETON_HORSE.create(level,EntitySpawnReason.EVENT);
                if(horse==null){allMounted=false;continue;}
                Vec3 p=position().add(Vec3.atLowerCornerOf(DIRECTIONS[i].getUnitVec3i()).scale(30));
                horse.snapTo(p.x,p.y,p.z,0,0);horse.setPersistenceRequired();horse.setNoAi(true);
                prepareCombatHorse(horse);
                horse.noPhysics=true;horse.setNoGravity(true);horse.setPermanentlyInvulnerable(true);
                horse.setItemSlot(EquipmentSlot.BODY,new ItemStack(Items.NETHERITE_HORSE_ARMOR));
                horse.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(.24D);
                if(!level.addFreshEntity(horse)){allMounted=false;continue;}horses[i]=horse.getUUID();
            }
            if(horse==null){allMounted=false;continue;}
            if(rider.getVehicle()==horse)continue;
            allMounted=false;
            Vec3 target=coffin.position().add(Vec3.atLowerCornerOf(coffin.getCoffinFacing().getUnitVec3i()).scale(1.4));
            Vec3 delta=target.subtract(horse.position());
            horse.noPhysics=true;horse.setNoGravity(true);horse.setDeltaMovement(Vec3.ZERO);
            if(delta.length()>.3) {
                Vec3 p=horse.position().add(delta.normalize().scale(.3));horse.setPos(p.x,p.y,p.z);face(horse,target);
                if(phaseTicks%4==0)level.sendParticles(ParticleTypes.SMOKE,horse.getX(),horse.getY()+.9,horse.getZ(),5,.5,.5,.5,.015);
            } else {
                coffin.releaseSaintsCollision();
                if(i==1){coffin.removeSaintsLid();rider.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(CatacombSaints.LID));}
                rider.setPose(Pose.STANDING);rider.clearSleepingPos();rider.setNoGravity(false);
                rider.startRiding(horse,true,false);
            }
        }
        if(allMounted) {
            for(int i=0;i<4;i++) {
                if(level.getEntity(riders[i]) instanceof Mob rider){rider.setPermanentlyInvulnerable(false);rider.setNoAi(false);rider.getGoalSelector().removeAllGoals(g->true);}
                if(level.getEntity(horses[i]) instanceof SkeletonHorse horse) {
                    horse.noPhysics=false;horse.setNoGravity(false);horse.setPermanentlyInvulnerable(false);horse.setNoAi(false);
                    horse.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(.24D);
                }
            }
            phase=2;phaseTicks=0;
        }
    }
    private void recoverSpear(ServerLevel level) {
        if((deadMask&4)!=0){phase=3;phaseTicks=0;return;}
        if(!(level.getEntity(riders[2]) instanceof AbstractSkeleton rider))return;
        Entity display=spear==null?null:level.getEntity(spear);
        if(display==null)return; // never replace a missing/unloaded spear with a duplicate
        move(rider,display.position(),1.05);
        if(rider.distanceToSqr(display)<6.25) {
            rider.setItemSlot(EquipmentSlot.MAINHAND,((PlantedSwordEntity)display).sword().copy());
            rider.setItemSlot(EquipmentSlot.MAINHAND,modernSpear(rider.getMainHandItem()));rider.setDropChance(EquipmentSlot.MAINHAND,2.0F);display.discard();spear=null;
            phase=3;phaseTicks=0;
        }
    }
    private void battle(ServerLevel level) {
        List<ServerPlayer> players=level.players().stream().filter(p->p.isAlive()&&!p.isCreative()&&!p.isSpectator()
                &&p.distanceToSqr(this)<64*64).toList();
        if(players.isEmpty()){
            for(UUID id:riders)if(id!=null && level.getEntity(id) instanceof AbstractSkeleton rider){
                setRush(rider,false);rider.stopUsingItem();rider.getNavigation().stop();
            }
            return;
        }
        Vec3 centre=Vec3.ZERO;for(Player p:players)centre=centre.add(p.position());centre=centre.scale(1.0/players.size());
        int cycle=phaseTicks%180;
        if(!formationAssigned || cycle==0)assignFormation(level,centre);
        // One shared phase keeps the four destinations exactly a quarter turn apart.
        formationAngle=(formationAngle+.010)%(Math.PI*2);
        for(int i=0;i<4;i++) {
            if((deadMask&(1<<i))!=0||!(level.getEntity(riders[i]) instanceof AbstractSkeleton rider)||!rider.isAlive())continue;
            ServerPlayer target=players.stream().min(java.util.Comparator.comparingDouble(p->p.distanceToSqr(rider))).orElseThrow();
            if(i==3)target=players.stream().filter(Player::isBlocking).min(java.util.Comparator.comparingDouble(p->p.distanceToSqr(rider))).orElse(target);
            for(EquipmentSlot slot:new EquipmentSlot[]{EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET})
                if(rider.getItemBySlot(slot).has(ModDataComponents.ANCIENT_APPEARANCE))rider.getItemBySlot(slot).remove(ModDataComponents.ANCIENT_APPEARANCE);
            rider.setTarget(target);rider.setNoAi(false);face(rider,target.getEyePosition());
            if(i==0)bowChargeEffects(level,rider,cycle);
            if(i==2 && tickDismountedSpear(level,rider,target))continue;
            rider.getGoalSelector().removeAllGoals(g->true);
            Entity carrier=rider.getVehicle()==null?rider:rider.getVehicle();
            double angle=formationAngle+formationSlots[i]*Math.PI/2;
            Vec3 formationPoint=centre.add(Math.cos(angle)*7,0,Math.sin(angle)*7);
            Vec3 destination=formationPoint;
            boolean forming=true;
            boolean rushing=i!=3 && cycle>=75 && cycle<=170;
            boolean protectsMace=i==1 && maceMode==1 && (deadMask&4)==0
                    && riders[2]!=null && level.getEntity(riders[2]) instanceof AbstractSkeleton;
            boolean swordApproach=i==1 && !protectsMace && cycle>=75 && cycle<=170 && lastMeleeCycle[1]!=phaseTicks/180;
            boolean axeApproach=i==3 && (target.isBlocking() || phaseTicks%70>=10) && lastMeleeCycle[3]!=phaseTicks/70;
            if(swordApproach){destination=target.position();forming=false;}
            if(axeApproach){
                destination=target.position();forming=false;
            }
            if(i==1) {
                Vec3 intercept=intercept(level,players,rider);
                boolean guarding=CoffinLidProjectiles.guardUntil(rider)>level.getGameTime();
                if(intercept!=null && !swordApproach){rushing=true;forming=false;destination=intercept;guarding=true;}
                if(protectsMace && level.getEntity(riders[2]) instanceof AbstractSkeleton maceSaint){
                    Vec3 toward=target.position().subtract(maceSaint.position()).multiply(1,0,1).normalize();
                    destination=maceSaint.position().add(toward.scale(1.8));
                    rushing=true;forming=false;guarding=true;face(rider,target.getEyePosition());
                    CoffinLidProjectiles.holdGuard(rider,level.getGameTime()+20);
                }
                if(guarding){if(!rider.isUsingItem())rider.startUsingItem(InteractionHand.OFF_HAND);}
                else rider.stopUsingItem();
            }
            if(i==0 && cycle>=105 && cycle<130 && !rider.isUsingItem())rider.startUsingItem(InteractionHand.MAIN_HAND);
            if(i==2) {
                var kinetic=rider.getMainHandItem().get(DataComponents.KINETIC_WEAPON);
                var range=rider.getAttackRangeWith(rider.getMainHandItem());
                boolean charging=cycle>=130 && cycle<=170;
                rushing=charging;
                if(cycle>=95 && cycle<130) {
                    Vec3 away=carrier.position().subtract(target.position()).multiply(1,0,1).normalize();
                    if(away.lengthSqr()<.01)away=new Vec3(Math.cos(angle),0,Math.sin(angle));
                    destination=target.position().add(away.scale(range.effectiveMaxRange(rider)+2.0));forming=false;
                }
                if(!charging)spearChargeEnd=null;
                if(charging) {
                    if(spearChargeEnd==null || carrier.distanceToSqr(target)>range.effectiveMaxRange(rider)*range.effectiveMaxRange(rider)) {
                        Vec3 forward=target.position().subtract(carrier.position()).multiply(1,0,1).normalize();
                        if(forward.lengthSqr()<.01)forward=rider.getLookAngle().multiply(1,0,1).normalize();
                        spearChargeEnd=target.position().add(forward.scale(4));
                    }
                    destination=spearChargeEnd;forming=false;
                }
                // Prime the real kinetic weapon so its warm-up ends on the shared
                // attack cue. Vanilla handles swept hits, reach and charge damage.
                int readyAt=kinetic==null?130:Math.max(95,130-kinetic.delayTicks());
                if(kinetic!=null && cycle>=readyAt && cycle<=170) {
                    if(!rider.isUsingItem()){
                        rider.startUsingItem(InteractionHand.MAIN_HAND);kinetic.makeSound(rider);
                    }
                    rider.setAggressive(true);
                    rider.getLookControl().setLookAt(target,30.0F,30.0F);
                    Vec3 aim=target.getEyePosition().subtract(rider.getEyePosition());
                    rider.setXRot((float)(-Math.atan2(aim.y,aim.horizontalDistance())*180/Math.PI));
                } else {rider.stopUsingItem();rider.setAggressive(false);rider.setXRot(0);}
            }
            boolean catchingUp=forming && carrier.position().subtract(formationPoint).multiply(1,0,1).lengthSqr()>6.25;
            setRush(rider,rushing || catchingUp);
            if(phaseTicks%5==0)move(rider,destination,i==2 && rushing?1.45:(i==3?1.15:1.05));
            if(i==0 && cycle==130){rider.stopUsingItem();shoot(level,rider,target);}
            boolean meleeReady=(swordApproach && cycle>=130) || axeApproach;
            // Mounted feet are high above the target: use horizontal approach distance
            // with a separate vertical bound, and retain the attack until we arrive.
            if(meleeReady && rider.position().subtract(target.position()).multiply(1,0,1).lengthSqr()<9.0
                    && Math.abs(rider.getY()-target.getY())<3.0 && rider.hasLineOfSight(target)) {
                rider.stopUsingItem();rider.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT);rider.doHurtTarget(level,target);
                lastMeleeCycle[i]=i==3?phaseTicks/70:phaseTicks/180;
                if(i==1){CoffinLidProjectiles.holdGuard(rider,level.getGameTime()+20);rider.startUsingItem(InteractionHand.OFF_HAND);}
            }
        }
    }
    private static void bowChargeEffects(ServerLevel level,AbstractSkeleton rider,int cycle){
        if(cycle>=105 && cycle<=125 && cycle%3==0){
            Vec3 centre=rider.getEyePosition().add(0,-.35,0);
            for(int n=0;n<3;n++){
                double angle=level.getRandom().nextDouble()*Math.PI*2;
                Vec3 offset=new Vec3(Math.cos(angle)*1.15,(level.getRandom().nextDouble()-.5)*1.2,Math.sin(angle)*1.15);
                Vec3 start=centre.add(offset),velocity=offset.scale(-.16);
                level.sendParticles(ParticleTypes.FLAME,start.x,start.y,start.z,0,velocity.x,velocity.y,velocity.z,1);
            }
        }
        if(cycle==125)rider.addEffect(new MobEffectInstance(MobEffects.GLOWING,12,0,false,false));
    }
    private boolean tickDismountedSpear(ServerLevel level,AbstractSkeleton rider,Player target){
        if(!rider.getUUID().equals(riders[2]))return false;
        if(rider.isPassenger()){
            // A remount (including a command/reload) cancels any pending jump;
            // never call stopRiding or eject the rider to enter the mace phase.
            rider.getPersistentData().putBoolean("SaintsMaceAirborne",false);
            if(rider.getMainHandItem().is(Items.MACE)){
                maceMode=1;maceTicks=0;rider.stopUsingItem();return true;
            }
            maceMode=0;maceTicks=0;return false;
        }
        permanentMace |= SaintsDeathData.get(level).spearClaimed(getUUID());
        if(spearThrowCooldown>0)spearThrowCooldown--;
        if(maceMode==0){
            if(rider.isPassenger() || spearThrowCooldown>0)return false;
            if(!isSaintSpear(rider.getMainHandItem()))return false;
            SaintThrownSpear projectile=CatacombSaints.THROWN_SPEAR.create(level,EntitySpawnReason.EVENT);
            if(projectile==null)return false;
            projectile.configure(rider.getMainHandItem(),getUUID());projectile.setOwner(rider);
            projectile.setPos(rider.getX(),rider.getEyeY()-.1,rider.getZ());
            Vec3 aim=target.getEyePosition().subtract(projectile.position());
            projectile.shoot(aim.x,aim.y+aim.horizontalDistance()*.2,aim.z,2.5F,1.0F);
            if(!level.addFreshEntity(projectile))return false;
            thrownSpear=projectile.getUUID();rider.stopUsingItem();rider.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT);
            rider.playSound(net.minecraft.sounds.SoundEvents.TRIDENT_THROW.value(),1,1);
            rider.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.MACE));
            rider.setDropChance(EquipmentSlot.MAINHAND,.085F);
            spearChargeEnd=null;maceMode=1;maceTicks=0;maceSlams=0;
        }
        rider.getGoalSelector().removeAllGoals(g->true);if(maceMode<=3)rider.getNavigation().stop();rider.stopUsingItem();
        rider.setNoAi(false);rider.setAggressive(true);rider.setXRot(0);setRush(rider,false);
        maceTicks++;
        if(maceMode==1){
            rider.setDeltaMovement(new Vec3(0,rider.getDeltaMovement().y,0));
            if(maceTicks%4==0)level.sendParticles(ParticleTypes.SMOKE,rider.getX(),rider.getY()+1,rider.getZ(),5,.4,.5,.4,.025);
            if(maceTicks>=40 && rider.onGround() && !rider.isPassenger() && rider.getMainHandItem().is(Items.MACE)){
                rider.getPersistentData().putBoolean("SaintsMaceAirborne",true);
                Vec3 toward=target.position().subtract(rider.position()).multiply(1,0,1).normalize().scale(.28);
                rider.setOnGround(false);rider.setDeltaMovement(toward.x,1.18476502181,toward.z);rider.needsSync=true;
                rider.resetFallDistance();maceMode=2;maceTicks=0;
                level.sendParticles(ParticleTypes.FLAME,rider.getX(),rider.getY(),rider.getZ(),16,.4,.2,.4,.06);
            }
        }else if(maceMode==2){
            rider.resetFallDistance();
            if(maceTicks%2==0)level.sendParticles(ParticleTypes.FLAME,rider.getX(),rider.getY()+.5,rider.getZ(),3,.3,.4,.3,.015);
            Vec3 delta=target.position().subtract(rider.position()).multiply(1,0,1);
            Vec3 horizontal=delta.lengthSqr()>.09?delta.normalize().scale(.28):Vec3.ZERO;
            rider.setDeltaMovement(horizontal.x,rider.getDeltaMovement().y,horizontal.z);
            if(maceTicks>2 && rider.onGround()){
                maceImpact(level,rider);rider.getPersistentData().putBoolean("SaintsMaceAirborne",false);
                maceSlams++;maceMode=3;maceTicks=0;
            }else if(maceTicks>200 && (rider.isInWater() || rider.isInLava())){
                rider.getPersistentData().putBoolean("SaintsMaceAirborne",false);maceMode=3;maceTicks=0;
            }
        }else if(maceMode==3){
            rider.setDeltaMovement(new Vec3(0,rider.getDeltaMovement().y,0));
            if(maceTicks>=16){maceMode=maceSlams>=3&&!permanentMace?4:1;maceTicks=0;if(maceSlams>=3&&permanentMace)maceSlams=0;}
        }else if(maceMode==4){
            if(permanentMace){maceMode=1;maceSlams=0;maceTicks=0;}
            else if(thrownSpear!=null && level.getEntity(thrownSpear) instanceof SaintThrownSpear projectile){
                if(maceTicks%5==0)move(rider,projectile.position(),1.1);
                if(projectile.grounded() && rider.distanceToSqr(projectile)<4 && rider.hasLineOfSight(projectile)){
                    rider.setItemSlot(EquipmentSlot.MAINHAND,projectile.reclaim());rider.setDropChance(EquipmentSlot.MAINHAND,2.0F);
                    thrownSpear=null;maceMode=5;maceTicks=0;
                }
                // An inaccessible spear is retried after another attack sequence.
                else if(maceTicks>160){maceMode=1;maceTicks=0;maceSlams=0;}
            }else if(maceTicks>=20){
                // Missing/unloaded is not the same as stolen: never mint a replacement.
                maceMode=1;maceTicks=0;maceSlams=0;
            }
        }else if(maceMode==5){
            Entity horse=horses[2]==null?null:level.getEntity(horses[2]);
            if(horse instanceof SkeletonHorse mount && mount.isAlive() && !mount.isVehicle()){
                if(maceTicks%5==0)move(rider,mount.position(),1.1);
                if(rider.distanceToSqr(mount)<6.25){rider.startRiding(mount,true,false);maceMode=0;}
                else if(maceTicks>100){maceMode=0;spearThrowCooldown=60;}
            }else{maceMode=0;spearThrowCooldown=60;}
        }
        return true;
    }
    private static void maceImpact(ServerLevel level,AbstractSkeleton rider){
        if(rider.isPassenger() || !rider.getMainHandItem().is(Items.MACE)
                || rider.getPersistentData().getIntOr(CatacombSaints.ROLE_TAG,-1)!=2)return;
        rider.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT);
        rider.playSound(net.minecraft.sounds.SoundEvents.MACE_SMASH_GROUND,1.5F,.8F);
        level.sendParticles(ParticleTypes.EXPLOSION,rider.getX(),rider.getY()+.15,rider.getZ(),1,0,0,0,0);
        level.sendParticles(ParticleTypes.SMOKE,rider.getX(),rider.getY()+.15,rider.getZ(),25,1.5,.15,1.5,.06);
        for(Player player:level.getEntitiesOfClass(Player.class,rider.getBoundingBox().inflate(4.5,2,4.5),
                p->p.isAlive()&&!p.isCreative()&&!p.isSpectator())){
            double distance=player.position().subtract(rider.position()).horizontalDistance();
            if(distance>4.5 || !rider.hasLineOfSight(player))continue;
            float damage=(float)(18.0-10.0*distance/4.5);
            if(player.hurtServer(level,rider.damageSources().mobAttack(rider),damage)){
                Vec3 away=player.position().subtract(rider.position()).multiply(1,0,1).normalize();
                player.push(away.x*.65,.35,away.z*.65);player.syncVelocity=true;
            }
        }
    }
    private void assignFormation(ServerLevel level,Vec3 centre) {
        // Only 24 permutations: minimize travel while reserving one distinct slot
        // per saint. Reassignment has a cost to avoid swapping slots every cycle.
        double best=Double.POSITIVE_INFINITY;int[] chosen=formationSlots.clone();
        for(int a=0;a<4;a++)for(int b=0;b<4;b++)for(int c=0;c<4;c++)for(int d=0;d<4;d++) {
            if(a==b||a==c||a==d||b==c||b==d||c==d)continue;
            int[] slots={a,b,c,d};double cost=0;
            for(int i=0;i<4;i++) {
                if((deadMask&(1<<i))!=0 || riders[i]==null || !(level.getEntity(riders[i]) instanceof Mob rider))continue;
                Entity carrier=rider.getVehicle()==null?rider:rider.getVehicle();
                double angle=formationAngle+slots[i]*Math.PI/2;
                Vec3 point=centre.add(Math.cos(angle)*7,0,Math.sin(angle)*7);
                cost+=carrier.position().subtract(point).multiply(1,0,1).lengthSqr();
                if(formationAssigned && slots[i]!=formationSlots[i])cost+=9;
            }
            if(cost<best){best=cost;chosen=slots;}
        }
        System.arraycopy(chosen,0,formationSlots,0,4);formationAssigned=true;
    }
    private Vec3 intercept(ServerLevel level,List<ServerPlayer> players,AbstractSkeleton guard) {
        for(ServerPlayer player:players) {
            ItemStack held=player.isUsingItem()?player.getUseItem():player.getMainHandItem();
            if(!(held.getItem() instanceof net.minecraft.world.item.ProjectileWeaponItem || held.is(Items.TRIDENT)))continue;
            Vec3 origin=player.getEyePosition(),look=player.getLookAngle();
            for(int i:new int[]{0,2,3}) {
                if(!(level.getEntity(riders[i]) instanceof Mob ally)||!ally.isAlive())continue;
                Vec3 aim=ally.getEyePosition().subtract(origin);double forward=aim.dot(look);
                if(forward>0 && aim.subtract(look.scale(forward)).lengthSqr()<4.0) {
                    face(guard,player.getEyePosition());
                    return ally.position().add(player.position().subtract(ally.position()).normalize().scale(2.5));
                }
            }
        }
        return null;
    }
    private static void move(AbstractSkeleton rider,Vec3 destination,double speed) {
        Mob mount=rider.getVehicle() instanceof Mob m?m:rider;
        rider.getGoalSelector().removeAllGoals(g->true);
        rider.setNoAi(false);mount.setNoAi(false);
        if(mount instanceof SkeletonHorse horse) {
            horse.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(.24D);
            horse.setEating(false);horse.clearStanding();
        }
        // Keep the path if it is still usable; a failed path gets direct steering
        // with normal block collision (never the ghost introduction movement).
        if(!mount.getNavigation().moveTo(destination.x,destination.y,destination.z,speed))
            mount.getMoveControl().setWantedPosition(destination.x,destination.y,destination.z,speed);
    }
    private static void sleepFacing(AbstractSkeleton rider,CoffinEntity coffin) {
        float yaw=90.0F-coffin.getYRot();
        rider.setPose(Pose.SLEEPING);rider.setYRot(yaw);rider.setYHeadRot(yaw);rider.yBodyRot=yaw;
    }
    private static void setRush(AbstractSkeleton rider,boolean active) {
        Mob carrier=rider.getVehicle() instanceof Mob mob?mob:rider;
        var speed=carrier.getAttribute(Attributes.MOVEMENT_SPEED);
        if(speed==null)return;
        if(active && !speed.hasModifier(RUSH))speed.addTransientModifier(new AttributeModifier(
                RUSH,.25D,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        else if(!active)speed.removeModifier(RUSH);
    }
    private void prepareCombatHorse(SkeletonHorse horse){
        horse.getPersistentData().putString(CatacombSaints.ENCOUNTER_TAG,getUUID().toString());
        horse.setTamed(true);
        horse.getGoalSelector().removeAllGoals(g->true);
        // The encounter owns navigation. Ordinary wander/buck/jump goals must
        // not override formation movement or throw a saint off his own mount.
        horse.getGoalSelector().setControlFlag(Goal.Flag.JUMP,false);
    }
    private static void freeHorse(SkeletonHorse horse) {
        if(horse.getPersistentData().getBooleanOr("SaintsReleased",false))return;
        horse.getPersistentData().putBoolean("SaintsReleased",true);
        horse.getPersistentData().remove(CatacombSaints.ENCOUNTER_TAG);
        horse.setTarget(null);horse.setNoAi(false);horse.noPhysics=false;horse.setNoGravity(false);
        horse.setPermanentlyInvulnerable(false);horse.getNavigation().stop();horse.setTrap(false);
        horse.setTamed(true);horse.setEating(false);horse.clearStanding();
        horse.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(RUSH);
        horse.setItemSlot(EquipmentSlot.SADDLE,new ItemStack(Items.SADDLE));
        horse.getGoalSelector().removeAllGoals(g->true);
        ((SaintMobGoalsAccessor)horse).shamaschizm$registerGoals();
        for(Goal.Flag flag:Goal.Flag.values())horse.getGoalSelector().setControlFlag(flag,true);
    }
    private static void face(Mob mob,Vec3 point) {
        Vec3 delta=point.subtract(mob.position());float yaw=(float)(Math.atan2(delta.z,delta.x)*180/Math.PI)-90;
        mob.setYRot(yaw);mob.setYHeadRot(yaw);mob.yBodyRot=yaw;
    }
    private static void shoot(ServerLevel level,AbstractSkeleton rider,Player target) {
        SaintArrow arrow=CatacombSaints.ARROW.create(level,EntitySpawnReason.EVENT);if(arrow==null)return;
        arrow.setOwner(rider);arrow.setPos(rider.getX(),rider.getEyeY()-.1,rider.getZ());
        Vec3 aim=target.getEyePosition().subtract(arrow.position());
        arrow.shoot(aim.x,aim.y+aim.horizontalDistance()*.12,aim.z,1.6F,1.0F);
        arrow.setBaseDamage(2);arrow.setRemainingFireTicks(100);arrow.addEffect(new MobEffectInstance(MobEffects.POISON,100,0));
        arrow.pickup=AbstractArrow.Pickup.DISALLOWED;level.addFreshEntity(arrow);rider.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT);
    }
    public void recordDeath(UUID entity) {
        for(int i=0;i<4;i++)if(entity.equals(riders[i])) {
            deadMask|=1<<i;

            if(i==2 && level() instanceof ServerLevel level) {
                Entity fallen=level.getEntity(entity);
                dropUnclaimedSpear(level,fallen==null?position():fallen.position());
            }
        }
        if(Integer.bitCount(deadMask)>=3 && phase<4 && level() instanceof ServerLevel server)beginVindication(server);
    }
    public UUID finalBossId(){return finalBoss;}
    public boolean isRewarded(){return phase==4;}
    private void beginVindication(ServerLevel level){
        if(phase>=4)return;
        AbstractSkeleton survivor=null;
        for(int i=0;i<4;i++)if((deadMask&(1<<i))==0){
            if(riders[i]==null || !(level.getEntity(riders[i]) instanceof AbstractSkeleton loaded))return;
            survivor=loaded;break;
        }
        Vec3 at=survivor==null?position():survivor.position();
        var hit=level.clip(new net.minecraft.world.level.ClipContext(at.add(0,.1,0),at.add(0,-12,0),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,this));
        finalGround=hit.getType()==net.minecraft.world.phys.HitResult.Type.MISS?position():hit.getLocation();
        if(survivor!=null){lastSaint=survivor.getUUID();survivor.getPersistentData().putBoolean("SaintsConverting",true);survivor.setGlowingTag(true);survivor.stopRiding();survivor.stopUsingItem();survivor.setPose(Pose.STANDING);survivor.clearSleepingPos();survivor.setPermanentlyInvulnerable(true);survivor.setNoAi(true);survivor.getNavigation().stop();survivor.setPos(finalGround);survivor.setDeltaMovement(Vec3.ZERO);}
        for(UUID id:horses)if(id!=null&&level.getEntity(id) instanceof SkeletonHorse horse){horse.getNavigation().stop();horse.setDeltaMovement(Vec3.ZERO);}
        phase=5;phaseTicks=0;entityData.set(HUD_ACTIVE,false);
    }
    private void transformLastSaint(ServerLevel level){
        AbstractSkeleton survivor=lastSaint!=null && level.getEntity(lastSaint) instanceof AbstractSkeleton mob?mob:null;
        if(lastSaint!=null && survivor==null)return; // Wait for the existing entity to load.
        if(finalGround==null)finalGround=position();
        phaseTicks++;
        // Alternate the vanilla outline every 8 ticks (0.4 seconds) until the model swap.
        if(survivor!=null){survivor.getPersistentData().putBoolean("SaintsConverting",true);survivor.setGlowingTag((phaseTicks / 8) % 2 == 0);survivor.setPermanentlyInvulnerable(true);survivor.setNoAi(true);survivor.setPos(finalGround);survivor.setDeltaMovement(Vec3.ZERO);
            float intensity=Math.max(0,Math.min(1,(phaseTicks-40)/40F));
            float shake=(float)Math.sin(phaseTicks*(2.8F+intensity*.8F))*(26+intensity*36);
            survivor.setYRot(shake);survivor.setYHeadRot(shake);survivor.yBodyRot=-shake*.8F;
            survivor.setXRot((float)Math.sin(phaseTicks*(2.1F+intensity*.6F))*(18+intensity*20));
        }
        // The eruption marks the escalation, well before the model swap and rise.
        if(phaseTicks==40){
            level.sendParticles(ParticleTypes.LARGE_SMOKE,finalGround.x,finalGround.y+1,finalGround.z,100,.7,1,.7,.08);
            level.sendParticles(ParticleTypes.FLAME,finalGround.x,finalGround.y+1,finalGround.z,80,.7,1,.7,.06);
        }
        if(phaseTicks<80)return;
        var boss=net.beamex.shamaschizm.saints.vindication.VindicationRegistration.BOSS.create(level,EntitySpawnReason.EVENT);if(boss==null)return;
        boss.configure(getUUID(),finalGround);
        int survivorRole=0;for(int i=0;i<4;i++)if(lastSaint!=null && lastSaint.equals(riders[i]))survivorRole=i;
        boss.setMainSaint(survivorRole);boss.beginGroundedPrelude();if(!level.addFreshEntity(boss))return;
        finalBoss=boss.getUUID();phase=6;
        for(var piece:level.getEntitiesOfClass(net.beamex.shamaschizm.saints.vindication.SaintBoneEntity.class,getBoundingBox().inflate(160),p->p.belongsTo(getUUID())))piece.gather(boss,boss.slotForRole(piece.role()));
        if(survivor!=null){
            // Preserve the two guaranteed encounter weapon rewards if their owner transforms.
            for(EquipmentSlot slot:List.of(EquipmentSlot.MAINHAND,EquipmentSlot.OFFHAND)){
                ItemStack stack=survivor.getItemBySlot(slot);
                if(stack.is(CatacombSaints.LID)||isSaintSpear(stack))level.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(level,finalGround.x,finalGround.y+.5,finalGround.z,modernSpear(stack)));
            }
            survivor.discard();
        }
        dropUnclaimedSpear(level,finalGround);
    }
    public void vindicationDefeated(){
        if(phase==6 && level() instanceof ServerLevel level){
            net.beamex.shamaschizm.saints.audio.ArenaAudio.send(level,getUUID(),position(),false,false);
            deadMask=15;net.beamex.shamaschizm.experimental.BossGardenLocks.get(level).defeated(getUUID());reward(level);
        }
    }
    private static boolean isSaintSpear(ItemStack stack){return stack.is(Items.NETHERITE_SPEAR)||stack.is(net.beamex.shamaschizm.saints.vindication.VindicationRegistration.SPEAR);}
    private static ItemStack modernSpear(ItemStack stack){
        if(!isSaintSpear(stack))return stack.copy();
        ItemStack result=new ItemStack(net.beamex.shamaschizm.saints.vindication.VindicationRegistration.SPEAR);
        result.applyComponents(stack.getComponentsPatch());
        result.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,Component.literal("Witness of Sin"));return result;
    }
    private void dropUnclaimedSpear(ServerLevel level,Vec3 at) {
        if(spear!=null && level.getEntity(spear) instanceof PlantedSwordEntity display) {
            level.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(level,at.x,at.y,at.z,modernSpear(display.sword())));
            display.discard();spear=null;
        }
    }
    private void reward(ServerLevel level) {
        phase=4; // ordinary reload cannot repeat rewards
        SaintsDeathData.get(level).clear(getUUID());
        for(UUID id:horses)if(id!=null&&level.getEntity(id) instanceof SkeletonHorse horse) {
            freeHorse(horse);freedHorses.add(id);
        }
        CoffinEntity source=CatacombSaints.COFFIN2.create(level,EntitySpawnReason.EVENT);
        if(source!=null)for(int i=0;i<4;i++)source.releasePositiveLootAt(level,position());
        if(spear!=null&&level.getEntity(spear)!=null)level.getEntity(spear).discard();
    }
    @Override protected void addAdditionalSaveData(ValueOutput out) {
        saveId(out,"FinalBoss",finalBoss);saveId(out,"LastSaint",lastSaint);
        if(finalGround!=null){out.putDouble("FinalX",finalGround.x);out.putDouble("FinalY",finalGround.y);out.putDouble("FinalZ",finalGround.z);}
        out.putInt("MaceMode",maceMode);out.putInt("MaceTicks",maceTicks);out.putInt("MaceSlams",maceSlams);
        out.putInt("SpearThrowCooldown",spearThrowCooldown);out.putBoolean("PermanentMace",permanentMace);
        saveId(out,"ThrownSpear",thrownSpear);
        out.putDouble("FormationAngle",formationAngle);out.putBoolean("FormationAssigned",formationAssigned);
        for(int i=0;i<4;i++)out.putInt("FormationSlot"+i,formationSlots[i]);
        out.putBoolean("Initialized",initialized);out.putInt("Phase",phase);out.putInt("PhaseTicks",phaseTicks);out.putInt("DeadMask",deadMask);
        if(spear!=null)out.putString("Spear",spear.toString());
        for(int i=0;i<4;i++){saveId(out,"Coffin"+i,coffins[i]);saveId(out,"Rider"+i,riders[i]);saveId(out,"Horse"+i,horses[i]);}
    }
    private static void saveId(ValueOutput out,String key,UUID id){if(id!=null)out.putString(key,id.toString());}
    private static UUID readId(ValueInput in,String key){try{return UUID.fromString(in.getString(key).orElse(""));}catch(IllegalArgumentException e){return null;}}
    @Override protected void readAdditionalSaveData(ValueInput in) {
        finalBoss=readId(in,"FinalBoss");lastSaint=readId(in,"LastSaint");
        finalGround=new Vec3(in.getDoubleOr("FinalX",getX()),in.getDoubleOr("FinalY",getY()),in.getDoubleOr("FinalZ",getZ()));
        maceMode=in.getIntOr("MaceMode",0);maceTicks=in.getIntOr("MaceTicks",0);maceSlams=in.getIntOr("MaceSlams",0);
        spearThrowCooldown=in.getIntOr("SpearThrowCooldown",0);permanentMace=in.getBooleanOr("PermanentMace",false);thrownSpear=readId(in,"ThrownSpear");
        formationAngle=in.getDoubleOr("FormationAngle",0);formationAssigned=in.getBooleanOr("FormationAssigned",false);
        for(int i=0;i<4;i++)formationSlots[i]=in.getIntOr("FormationSlot"+i,i);
        initialized=in.getBooleanOr("Initialized",false);phase=in.getIntOr("Phase",0);phaseTicks=in.getIntOr("PhaseTicks",0);deadMask=in.getIntOr("DeadMask",0);
        spear=readId(in,"Spear");for(int i=0;i<4;i++){coffins[i]=readId(in,"Coffin"+i);riders[i]=readId(in,"Rider"+i);horses[i]=readId(in,"Horse"+i);}
    }
}
