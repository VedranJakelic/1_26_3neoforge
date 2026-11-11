package net.beamex.shamaschizm.homunculus;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** One carried stack, saved with the entity. Transfers revalidate both inventories at arrival. */
final class HomunculusWork {
    private final HomunculusEntity mob;
    private final Set<Long> tickets = new HashSet<>();
    private List<BlockPos> chests = List.of();
    private ItemStack cargo = ItemStack.EMPTY;
    private final Deque<ItemStack> harvestCargo = new ArrayDeque<>();
    private BlockPos source, destination, crop;
    private int sourceSlot, refresh, jobTicks;
    private final Map<BlockPos, Integer> unreachable = new HashMap<>();
    HomunculusWork(HomunculusEntity mob) { this.mob = mob; }
    void resetJob() { source = destination = crop = null; refresh = jobTicks = 0; chests = List.of(); unreachable.clear(); }
    void updateTickets() {
        if (!(mob.level() instanceof ServerLevel level)) return;
        Set<Long> needed = new HashSet<>();
        if (mob.command() == HomunculusCommand.SORT && mob.isAlive()) {
            BlockPos a = mob.anchor();
            for (int x = (a.getX()-15)>>4; x <= (a.getX()+15)>>4; x++)
                for (int z = (a.getZ()-15)>>4; z <= (a.getZ()+15)>>4; z++) needed.add(ChunkPos.pack(x,z));
        }
        if (mob.command() == HomunculusCommand.DESTROY && mob.isAlive()) {
            var center=ChunkPos.containing(mob.blockPosition());
            for(int x=center.x()-1;x<=center.x()+1;x++)for(int z=center.z()-1;z<=center.z()+1;z++)needed.add(ChunkPos.pack(x,z));
        }
        for (long key : new HashSet<>(tickets)) if (!needed.contains(key)) force(level, key, false);
        for (long key : needed) if (!tickets.contains(key)) force(level, key, true);
    }
    private void force(ServerLevel level, long key, boolean add) {
        ChunkPos p = ChunkPos.unpack(key);
        HomunculusRegistration.TICKETS.forceChunk(level, mob.getUUID(), p.x(), p.z(), add, true);
        if (add) tickets.add(key); else tickets.remove(key);
    }
    void releaseTickets() {
        if (mob.level() instanceof ServerLevel level) for (long key : new HashSet<>(tickets)) force(level, key, false);
    }
    private boolean within(BlockPos p) { return p.distSqr(mob.anchor()) <= 225; }
    private boolean blocked(BlockPos p) { return unreachable.getOrDefault(p, 0) > mob.tickCount; }
    private Container chest(BlockPos pos) {
        if (pos == null || !within(pos) || !mob.level().hasChunkAt(pos)) return null;
        BlockState state = mob.level().getBlockState(pos);
        if (state.getBlock() instanceof EnderChestBlock) {
            var owner = mob.ownerPlayer();
            return owner != null && owner.level() == mob.level()
                    && !mob.level().getBlockState(pos.above()).isRedstoneConductor(mob.level(), pos.above())
                    ? owner.getEnderChestInventory() : null;
        }
        if (state.getBlock() instanceof ChestBlock block) {
            if (!(mob.level().getBlockEntity(pos) instanceof ChestBlockEntity be) || be.isLocked()) return null;
            if (state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
                BlockPos other = pos.relative(ChestBlock.getConnectedDirection(state));
                if (!within(other) || !(mob.level().getBlockEntity(other) instanceof ChestBlockEntity second) || second.isLocked()) return null;
            }
            return ChestBlock.getContainer(block, state, mob.level(), pos, false);
        }
        return null;
    }
    private void scan() {
        List<BlockPos> result = new ArrayList<>();
        boolean enderAdded = false;
        for (BlockPos p : BlockPos.betweenClosed(mob.anchor().offset(-15,-15,-15), mob.anchor().offset(15,15,15))) {
            if (!within(p) || !mob.level().hasChunkAt(p)) continue;
            BlockState s = mob.level().getBlockState(p);
            // A double chest is one inventory, not two independent sorting destinations.
            if (s.getBlock() instanceof ChestBlock && s.getValue(ChestBlock.TYPE) == ChestType.RIGHT) continue;
            boolean ender = s.getBlock() instanceof EnderChestBlock;
            if (ender && enderAdded) continue;
            if ((s.getBlock() instanceof ChestBlock || ender) && chest(p) != null) {
                result.add(p.immutable()); if (ender) enderAdded = true;
            }
        }
        result.sort(Comparator.comparingLong(BlockPos::asLong)); chests = result;
        unreachable.entrySet().removeIf(e -> e.getValue() <= mob.tickCount);
    }
    private int count(Container c, ItemStack stack) {
        int total = 0;
        for (int i=0;i<c.getContainerSize();i++) if (ItemStack.isSameItemSameComponents(c.getItem(i),stack)) total += c.getItem(i).getCount();
        return total;
    }
    private boolean room(Container c, ItemStack stack) {
        for (int i=0;i<c.getContainerSize();i++) {
            ItemStack s = c.getItem(i);
            if (c.canPlaceItem(i,stack) && (s.isEmpty() || ItemStack.isSameItemSameComponents(s,stack)
                    && s.getCount() < Math.min(s.getMaxStackSize(),c.getMaxStackSize()))) return true;
        }
        return false;
    }
    private boolean pure(Container c, ItemStack stack) {
        for(int i=0;i<c.getContainerSize();i++)if(!c.getItem(i).isEmpty()&&!ItemStack.isSameItemSameComponents(c.getItem(i),stack))return false;
        return true;
    }
    private boolean primary(Container c, ItemStack stack) {
        int amount=count(c,stack);
        String id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        for(int i=0;i<c.getContainerSize();i++) {
            ItemStack other=c.getItem(i);if(other.isEmpty()||ItemStack.isSameItemSameComponents(other,stack))continue;
            int n=count(c,other);
            if(n>amount || n==amount && net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(other.getItem()).toString().compareTo(id)<0)return false;
        }
        return true;
    }
    private BlockPos best(ItemStack stack, BlockPos from, boolean allowEmpty) {
        Container src=chest(from);
        int sourceRank=src==null?-1:pure(src,stack)?2:1;
        BlockPos best=null;int bestRank=-1;
        for(BlockPos p:chests) {
            if(p.equals(from)||blocked(p))continue;
            Container c=chest(p);if(c==null||!room(c,stack))continue;
            int n=count(c,stack);
            int rank=n>0?(pure(c,stack)?2:1):0;
            if(n==0 && (!c.isEmpty() || !(allowEmpty || src!=null&&!primary(src,stack))))continue;
            if(src!=null && rank>0 && (!HomunculusRules.canRelocate(sourceRank,from.asLong(),rank,p.asLong())))continue;
            if(rank>bestRank){best=p;bestRank=rank;}
        }
        return best;
    }
    private void insert(Container c) {
        for (int pass=0;pass<2 && !cargo.isEmpty();pass++) for (int i=0;i<c.getContainerSize() && !cargo.isEmpty();i++) {
            if (!c.canPlaceItem(i,cargo)) continue;
            ItemStack s = c.getItem(i);
            if (pass == 0 && !s.isEmpty() && ItemStack.isSameItemSameComponents(s,cargo)) {
                int n = Math.min(cargo.getCount(),Math.min(s.getMaxStackSize(),c.getMaxStackSize())-s.getCount());
                if (n > 0) { s.grow(n); cargo.shrink(n); c.setChanged(); }
            } else if (pass == 1 && s.isEmpty()) {
                int n = Math.min(cargo.getCount(),Math.min(cargo.getMaxStackSize(),c.getMaxStackSize()));
                c.setItem(i,cargo.split(n)); c.setChanged();
            }
        }
    }
    private boolean arrive(BlockPos p) {
        if (p == null) return false;
        if (mob.distanceToSqr(Vec3.atCenterOf(p)) <= 6.25 && visible(p)) { mob.getNavigation().stop(); jobTicks = 0; return true; }
        if ((jobTicks += 10) > 240) { unreachable.put(p,mob.tickCount+600); source=destination=crop=null; jobTicks=0; return false; }
        // Path to adjacent walkable space instead of the solid chest/crop itself.
        Vec3 best = null; double d = Double.MAX_VALUE;
        for (var dir : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            Vec3 at = Vec3.atBottomCenterOf(p.relative(dir));
            if (!within(BlockPos.containing(at))) continue;
            var path = mob.workPath(p.relative(dir));
            if (path != null && path.canReach() && mob.distanceToSqr(at) < d) { best=at; d=mob.distanceToSqr(at); }
        }
        if (best != null) mob.walkTo(best);
        return false;
    }
    private boolean visible(BlockPos p) {
        var hit = mob.level().clip(new net.minecraft.world.level.ClipContext(mob.getEyePosition(),Vec3.atCenterOf(p),
                net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,mob));
        return hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(p);
    }
    void tick(boolean harvest) {
        if (mob.position().distanceToSqr(Vec3.atBottomCenterOf(mob.anchor()))>225) { mob.walkTo(Vec3.atBottomCenterOf(mob.anchor())); return; }
        if(cargo.isEmpty()&&!harvestCargo.isEmpty())cargo=harvestCargo.removeFirst();
        if (--refresh <= 0) { scan(); refresh = 10; }
        if (!cargo.isEmpty()) {
            if (destination == null || chest(destination) == null || !room(chest(destination),cargo)) destination = best(cargo,null,true);
            if (destination != null && arrive(destination)) { Container c=chest(destination); if(c!=null)insert(c); destination=null; }
            return; // If every chest is full, keep the stack safely until space becomes available.
        }
        if (source != null) {
            Container c=chest(source);
            if(c==null){source=null;return;}
            if(arrive(source)) {
                // Replan using the live source stack; players/hoppers may have changed it.
                ItemStack s=c.getItem(sourceSlot);
                BlockPos to=s.isEmpty()?null:best(s,source,false);
                if(to!=null){cargo=c.removeItem(sourceSlot,Math.min(16,s.getCount()));c.setChanged();destination=to;}
                source=null;
            }
            return;
        }
        if (harvest) { harvest(); return; }
        for(BlockPos p:chests) {
            if(blocked(p))continue;
            Container c=chest(p); if(c==null)continue;
            for(int i=0;i<c.getContainerSize();i++) {
                ItemStack s=c.getItem(i);
                if(!s.isEmpty() && best(s,p,false)!=null){source=p;sourceSlot=i;jobTicks=0;return;}
            }
        }
    }
    private void harvest() {
        ServerLevel level=(ServerLevel)mob.level();
        if(!level.getGameRules().get(GameRules.MOB_GRIEFING))return;
        if(crop!=null) {
            BlockState state=level.getBlockState(crop);
            if(!(state.getBlock() instanceof CropBlock b) || !b.isMaxAge(state)){crop=null;return;}
            if(arrive(crop)) {
                List<ItemStack> drops=Block.getDrops(state,level,crop,null,mob,ItemStack.EMPTY);
                Item seed = state.is(Blocks.WHEAT)?Items.WHEAT_SEEDS:state.is(Blocks.CARROTS)?Items.CARROT:
                        state.is(Blocks.POTATOES)?Items.POTATO:state.is(Blocks.BEETROOTS)?Items.BEETROOT_SEEDS:null;
                boolean replanted=false;
                if(seed!=null) for(ItemStack s:drops)if(s.is(seed)&&!s.isEmpty()){s.shrink(1);replanted=true;break;}
                if(replanted) {
                    level.setBlock(crop,b.getStateForAge(0),3);
                    level.levelEvent(2001,crop,Block.getId(state));
                    for(ItemStack s:drops)if(!s.isEmpty()){
                        if(cargo.isEmpty())cargo=s;
                        else harvestCargo.addLast(s);
                    }
                }
                crop=null;
            }
            return;
        }
        for(ItemEntity item:level.getEntitiesOfClass(ItemEntity.class,new AABB(mob.anchor()).inflate(15),e->e.isAlive()
                && within(e.blockPosition()) && isFarmLoot(e.getItem()))) {
            if(mob.distanceToSqr(item)<4){cargo=item.getItem().copy();item.discard();return;}
            mob.walkTo(item.position());return;
        }
        // Scan once a second, not once per block per tick.
        if(mob.tickCount%20!=0)return;
        for(BlockPos p:BlockPos.betweenClosed(mob.anchor().offset(-15,-4,-15),mob.anchor().offset(15,4,15))) {
            if(!within(p)||blocked(p)||!level.hasChunkAt(p))continue;
            BlockState s=level.getBlockState(p);
            if((s.is(Blocks.WHEAT)||s.is(Blocks.CARROTS)||s.is(Blocks.POTATOES)||s.is(Blocks.BEETROOTS))
                    && s.getBlock() instanceof CropBlock b && b.isMaxAge(s)){crop=p.immutable();jobTicks=0;return;}
        }
    }
    private static boolean isFarmLoot(ItemStack s) {
        return s.is(Items.WHEAT)||s.is(Items.WHEAT_SEEDS)||s.is(Items.CARROT)||s.is(Items.POTATO)
                ||s.is(Items.POISONOUS_POTATO)||s.is(Items.BEETROOT)||s.is(Items.BEETROOT_SEEDS);
    }
    void dropCargo(ServerLevel level) { if(!cargo.isEmpty()){mob.spawnAtLocation(level,cargo);cargo=ItemStack.EMPTY;} for(ItemStack s:harvestCargo)mob.spawnAtLocation(level,s);harvestCargo.clear(); }
    void save(ValueOutput out) {
        out.store("HarvestCargo",ItemStack.CODEC.listOf(),new ArrayList<>(harvestCargo));
        if(!cargo.isEmpty())out.store("WorkCargo",ItemStack.CODEC,cargo);
        out.putString("WorkTickets",tickets.stream().map(String::valueOf).sorted().collect(java.util.stream.Collectors.joining(",")));
    }
    void load(ValueInput in) {
        harvestCargo.clear();harvestCargo.addAll(in.read("HarvestCargo",ItemStack.CODEC.listOf()).orElse(List.of()));
        cargo=in.read("WorkCargo",ItemStack.CODEC).orElse(ItemStack.EMPTY);tickets.clear();
        for(String s:in.getStringOr("WorkTickets","").split(","))try{tickets.add(Long.parseLong(s));}catch(NumberFormatException ignored){}
        resetJob();
    }
}
