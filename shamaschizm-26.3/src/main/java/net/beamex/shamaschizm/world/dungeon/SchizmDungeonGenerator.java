package net.beamex.shamaschizm.world.dungeon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.garden.GardenData;
import net.beamex.shamaschizm.registry.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.beamex.shamaschizm.world.block.SchizmStoneRegistration;
import net.beamex.shamaschizm.world.block.SchizmPortalBlock;
import net.beamex.shamaschizm.registry.ModBlocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.VaultBlock;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;
import net.minecraft.world.level.block.entity.vault.VaultConfig;
import net.minecraft.world.level.block.entity.vault.VaultState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.beamex.shamaschizm.plantedsword.PlantedSwordEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.storage.loot.LootTable;

/** Server-thread-only, bounded marker-based dungeon planning and placement. */
public final class SchizmDungeonGenerator {
    /** Sends placed states to clients without onPlace, neighbor, wire-shape, or redstone updates. */
    private static final int QUIET_STRUCTURE_FLAGS = Block.UPDATE_CLIENTS
            | Block.UPDATE_KNOWN_SHAPE
            | Block.UPDATE_SKIP_SHAPE_UPDATE_ON_WIRE
            | Block.UPDATE_SKIP_ON_PLACE;
    public static final int ADDITIONAL_ROOMS = 50;
    // Does not include the spawn room, which already requires an exit.
    public static final int FIRST_ROOMS_WITH_EXITS = 10;
    public static final int FAILED_SEARCHES_PER_EXIT = 5;
    public static final int DEAD_END_ATTEMPTS_PER_EXIT = 18;
    public static final int MAX_LOOP_ROOMS = 15;
    public static final int MAX_SUCCESSFUL_LOOPS = 5;
    public static final int MAX_LOOP_PAIR_ATTEMPTS = 64;
    public static final int MAX_LOOP_SEARCH_NODES_PER_PAIR = 5_000;
    public static final int MAX_LOOP_BEAM_WIDTH = 256;
    public static final int MAX_LOOP_PATHS_PER_FRONTIER = 3;
    public static final int MAX_LOOP_CANDIDATES = 64;
    public static final int MAX_LOOP_CANDIDATES_PER_PAIR = 3;
    public static final int MAX_LOOP_SELECTION_NODES = 100_000;
    public static final int MAX_ROOM_WIDTH = 64;
    public static final int MAX_ROOM_HEIGHT = 128;
    private static final String GRAND_ROOM_TAG = "grand";
    private static final float GRAND_RIDER_CHANCE = 0.15F;
    // Finite spatial boundary for ordinary rooms and garden extension branches.
    public static final int RESERVATION_RADIUS = (ADDITIONAL_ROOMS + 1) * MAX_ROOM_WIDTH;

    private static final ResourceKey<LootTable> SCHIZM_VAULT_LOOT = ResourceKey.create(
            Registries.LOOT_TABLE, Shamaschizm.id("chests/schizm_vault"));
    private static final VaultConfig SCHIZM_VAULT_CONFIG = new VaultConfig(
            SCHIZM_VAULT_LOOT,
            4.0D,
            4.5D,
            new ItemStack(Items.TRIAL_KEY),
            java.util.Optional.empty());
    private static final VaultConfig SCHIZM_OMINOUS_VAULT_CONFIG = new VaultConfig(
            SCHIZM_VAULT_LOOT,
            4.0D,
            4.5D,
            new ItemStack(Items.OMINOUS_TRIAL_KEY),
            java.util.Optional.empty());

    private record Exit(BlockPos local, Direction outward) {}
    private record Room(DungeonRoomConfig.Entry entry, String name, StructureTemplate template, BlockPos entrance,
                        List<Exit> exits, List<BlockPos> markers, List<BlockPos> vaults) {}
    private record Placed(Room room, BlockPos origin, Rotation rotation, BoundingBox box) {}
    private record OpenExit(Placed parent, BlockPos position, Direction outward) {}
    public record Result(boolean placed, int additionalRooms) {}

    /** One immutable configuration snapshot and one selected start, shared with the teleporter. */
    public static final class Prepared {
        private final DungeonRoomConfig config;
        private final Room start;
        private final List<Room> choices;
        private Prepared(DungeonRoomConfig config, Room start, List<Room> choices) {
            this.config = config;
            this.start = start;
            this.choices = List.copyOf(choices);
        }
        public StructureTemplate startTemplate() { return start.template; }
        public String startName() { return start.name; }
        public BlockPos startExit() { return start.exits.getFirst().local; }
    }

    /** Called once per new dungeon; existing saved destinations bypass configuration loading. */
    public static Prepared prepare(ServerLevel level, int bottomY, long seed) {
        try (var reader = level.getServer().getResourceManager()
                .openAsReader(Shamaschizm.id("dungeon/rooms.json"))) {
            DungeonRoomConfig config = DungeonRoomConfig.read(reader);
            List<Room> starts = new ArrayList<>();
            List<Double> weights = new ArrayList<>();
            for (var entry : config.spawnRooms()) {
                starts.add(load(level, entry, true));
                weights.add(config.weight(entry, bottomY, null));
            }
            int selected = DungeonRoomConfig.chooseIndex(weights, new Random(seed));
            if (selected < 0) throw new IllegalArgumentException("All spawn rooms have zero weight at Y=" + bottomY);
            List<Room> choices = new ArrayList<>();
            for (var entry : config.rooms()) {
                if(entry.template().equals("shamaschizm:entrance2") || entry.template().equals("shamaschizm:arena2"))continue;
                choices.add(load(level, entry, false));
            }
            return new Prepared(config, starts.get(selected), choices);
        } catch (Exception exception) {
            Shamaschizm.LOGGER.error("Schizm dungeon configuration/templates invalid: {}", exception.toString());
            return null;
        }
    }

    private SchizmDungeonGenerator() {}

    private static StructurePlaceSettings settings(Rotation rotation) {
        // Dungeon rooms may contain persistent architectural entities such as
        // animated gates. They must be placed together with the room blocks.
        return new StructurePlaceSettings().setRotation(rotation).setIgnoreEntities(false)
                .setKnownShape(true)
                .addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);
    }

    private static BlockPos rotate(BlockPos point, Rotation rotation) {
        return StructureTemplate.transform(point, Mirror.NONE, rotation, BlockPos.ZERO);
    }

    private static Placed placed(Room room, BlockPos origin, Rotation rotation) {
        return new Placed(room, origin, rotation, room.template.getBoundingBox(settings(rotation), origin));
    }

    private static Room load(ServerLevel level, DungeonRoomConfig.Entry entry, boolean start) {
        String name = entry.template();
        StructureTemplate template = level.getStructureTemplateManager().get(Identifier.parse(name))
                .orElseThrow(() -> new IllegalArgumentException("Missing structure: " + name));
        var size = template.getSize();
        if (size.getX() < 3 || size.getZ() < 3 || size.getY() < 3
                || size.getX() > MAX_ROOM_WIDTH || size.getZ() > MAX_ROOM_WIDTH
                || size.getY() > MAX_ROOM_HEIGHT) {
            throw new IllegalArgumentException(name + ": expected dimensions 3..64 x 3..128 x 3..64");
        }
        var green = template.filterBlocks(BlockPos.ZERO, settings(Rotation.NONE), Blocks.WOOL.green());
        var red = template.filterBlocks(BlockPos.ZERO, settings(Rotation.NONE), Blocks.WOOL.red());
        if ((!start && green.size() != 1) || (start && !green.isEmpty())) {
            throw new IllegalArgumentException(name + ": requires " + (start ? "zero" : "one") + " green markers");
        }
        BlockPos entrance = start ? null : green.getFirst().pos();
        if (entrance != null && face(entrance, size.getX(), size.getZ(), name) != Direction.NORTH) {
            throw new IllegalArgumentException(name + ": green entrance must be on north wall (local Z=0)");
        }
        List<Exit> exits = new ArrayList<>();
        List<BlockPos> markers = new ArrayList<>();
        if (entrance != null) markers.add(entrance);
        for (var block : red) {
            exits.add(new Exit(block.pos(), face(block.pos(), size.getX(), size.getZ(), name)));
            markers.add(block.pos());
        }
        if (start && (exits.size() != 1 || exits.getFirst().outward != Direction.NORTH)) {
            throw new IllegalArgumentException(name + ": requires exactly one red marker on north wall");
        }
        List<BlockPos> vaults = template.filterBlocks(
                        BlockPos.ZERO, settings(Rotation.NONE), Blocks.VAULT)
                .stream().map(StructureTemplate.StructureBlockInfo::pos).toList();
        return new Room(entry, name, template, entrance, List.copyOf(exits),
                List.copyOf(markers), vaults);
    }

    private static Direction face(BlockPos p, int width, int depth, String name) {
        List<Direction> faces = new ArrayList<>();
        if (p.getZ() == 0) faces.add(Direction.NORTH);
        if (p.getZ() == depth - 1) faces.add(Direction.SOUTH);
        if (p.getX() == 0) faces.add(Direction.WEST);
        if (p.getX() == width - 1) faces.add(Direction.EAST);
        if (faces.size() != 1) throw new IllegalArgumentException(
                name + ": marker " + p + " must be on exactly one outer wall, away from corners");
        return faces.getFirst();
    }

    private static void addExits(Placed room, List<OpenExit> frontier) {
        for (Exit exit : room.room.exits) {
            frontier.add(new OpenExit(room, room.origin.offset(rotate(exit.local, room.rotation)),
                    room.rotation.rotate(exit.outward)));
        }
    }

    private static boolean hasEarlyBranch(List<Placed> layout) {
        return layout.stream().limit(5).anyMatch(room -> room.room.exits.size() > 2);
    }

    /** Bounded weighted search of the first five rooms; failed branches change no world blocks. */
    private static boolean planEarlyBranch(ServerLevel level, List<Placed> layout,
                                           List<OpenExit> frontier, Prepared prepared,
                                           Random random, BlockPos center, int[] budget) {
        if (hasEarlyBranch(layout)) return true;
        if (layout.size() >= 5 || frontier.isEmpty() || budget[0] <= 0) return false;
        List<OpenExit> exits = new ArrayList<>(frontier);
        Collections.shuffle(exits, random);
        for (OpenExit exit : exits) {
            List<Placed> candidates = new ArrayList<>();
            List<Double> weights = new ArrayList<>();
            for (Room room : prepared.choices) {
                if (room.name.equals("shamaschizm:garden1") || room.exits.isEmpty()) continue;
                if (layout.size() == 4 && room.exits.size() <= 2) continue;
                Placed candidate = attach(room, exit);
                double weight = prepared.config.weight(room.entry, candidate.box.minY(), exit.parent.room.entry.tag());
                if (weight > 0) { candidates.add(candidate); weights.add(weight); }
            }
            while (!candidates.isEmpty() && budget[0] > 0) {
                int selected = DungeonRoomConfig.chooseIndex(weights, random);
                if (selected < 0) break;
                Placed candidate = candidates.remove(selected);
                weights.remove(selected);
                budget[0]--;
                if (!available(level, candidate, exit, layout, center)) continue;
                List<Placed> trialLayout = new ArrayList<>(layout);
                List<OpenExit> trialFrontier = new ArrayList<>(frontier);
                trialLayout.add(candidate);
                trialFrontier.remove(exit);
                addExits(candidate, trialFrontier);
                if (planEarlyBranch(level, trialLayout, trialFrontier, prepared, random, center, budget)) {
                    layout.clear();
                    layout.addAll(trialLayout);
                    frontier.clear();
                    frontier.addAll(trialFrontier);
                    return true;
                }
            }
        }
        return false;
    }

    /** Allows only the shared connection wall with the parent; no other box overlap. */
    private static boolean fits(Placed candidate, OpenExit connection, List<Placed> layout) {
        for (Placed other : layout) {
            if (!candidate.box.intersects(other.box)) continue;
            if (other != connection.parent) return false;
            BoundingBox a = candidate.box;
            BoundingBox b = other.box;
            int coordinate = connection.outward.getAxis() == Direction.Axis.X
                    ? connection.position.getX() : connection.position.getZ();
            int low = connection.outward.getAxis() == Direction.Axis.X
                    ? Math.max(a.minX(), b.minX()) : Math.max(a.minZ(), b.minZ());
            int high = connection.outward.getAxis() == Direction.Axis.X
                    ? Math.min(a.maxX(), b.maxX()) : Math.min(a.maxZ(), b.maxZ());
            if (low != coordinate || high != coordinate) return false;
        }
        return true;
    }

    private static boolean untouched(ServerLevel level, BoundingBox box) {
        if (box.minY() < level.getMinY() || box.maxY() > level.getMaxY()
                || !level.getWorldBorder().isWithinBounds(new BlockPos(box.minX(), box.minY(), box.minZ()))
                || !level.getWorldBorder().isWithinBounds(new BlockPos(box.maxX(), box.maxY(), box.maxZ()))) return false;
        for (BlockPos p : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(),
                box.maxX(), box.maxY(), box.maxZ())) {
            BlockState state = level.getBlockState(p);
            if ((!state.is(SchizmStoneRegistration.SCHIZM_STONE) && !state.is(Blocks.STONE))
                    || level.getBlockEntity(p) != null) return false;
        }
        return true;
    }

    /** Plans before touching the world. Repeated room types are allowed. */
    public static Result generate(ServerLevel level, BlockPos origin) {
        Prepared prepared = prepare(level, origin.getY(), level.getSeed() ^ origin.asLong());
        return prepared == null ? new Result(false, 0) : generate(level, origin, prepared);
    }

    public static Result generate(ServerLevel level, BlockPos origin, Prepared prepared) {
        Random random = new Random(level.getSeed() ^ origin.asLong());
        List<Placed> layout = new ArrayList<>();
        List<OpenExit> frontier = new ArrayList<>();
        Placed first = placed(prepared.start, origin, Rotation.NONE);
        if (prepared.config.weight(prepared.start.entry, first.box.minY(), null) <= 0) return new Result(false, 0);
        if (!untouched(level, first.box)) return new Result(false, 0);
        layout.add(first);
        addExits(first, frontier);
        List<OpenExit> blocked = new ArrayList<>();
        Map<OpenExit, Integer> failures = new HashMap<>();
        Map<OpenExit, List<Room>> triedRooms = new HashMap<>();
        BlockPos reservationCenter = origin.offset(prepared.startExit());
        // The spawn room counts as room one. Plan a valid early branch before
        // committing any blocks, backtracking instead of accepting a bad prefix.
        if (!planEarlyBranch(level, layout, frontier, prepared, random, reservationCenter, new int[] {512})) {
            Shamaschizm.LOGGER.warn("Dungeon planning failed: no fitting positive-weight room with at least three red markers within the first five rooms (including spawn), within the search budget");
            return new Result(false, 0);
        }
        OpenExit lastFailedExit = null;
        while (layout.size() - 1 < ADDITIONAL_ROOMS && !frontier.isEmpty()) {
            OpenExit exit = removeRandomExit(frontier, lastFailedExit, random);
            Placed next = chooseRoom(level, exit, layout, prepared, random, reservationCenter,
                    triedRooms.computeIfAbsent(exit, key -> new ArrayList<>()));
            if (next == null) {
                lastFailedExit = exit;
                // Keep the connection available while trying other exits/room choices.
                // One weighted random room per attempt; switch exits on failure.
                if (failures.merge(exit, 1, Integer::sum) < FAILED_SEARCHES_PER_EXIT) {
                    frontier.add(exit);
                } else {
                    // Garden geometry may still fit this connection.
                    failures.remove(exit);
                    triedRooms.remove(exit);
                    blocked.add(exit);
                }
            } else {
                lastFailedExit = null;
                failures.remove(exit);
                triedRooms.remove(exit);
                layout.add(next);
                addExits(next, frontier);
            }
        }
        if (layout.size() - 1 == ADDITIONAL_ROOMS) {
            frontier.addAll(blocked);
            tryGarden(level, layout, frontier, prepared, random, reservationCenter);
        }
        List<BoundingBox> olderRoomBoxes = new ArrayList<>();
        for (Placed room : layout) {
            var box = room.box;
            AABB swordBounds = new AABB(box.minX(), box.minY(), box.minZ(),
                    box.maxX() + 1.0D, box.maxY() + 1.0D, box.maxZ() + 1.0D);
            // Do not reclassify an existing player-planted sword in this volume.
            java.util.Set<java.util.UUID> existingSwords = new java.util.HashSet<>();
            for (PlantedSwordEntity sword : level.getEntitiesOfClass(PlantedSwordEntity.class, swordBounds)) {
                existingSwords.add(sword.getUUID());
            }
            if (!room.room.template.placeInWorld(level, room.origin, room.origin,
                    settings(room.rotation).addProcessor(new OlderRoomPriorityProcessor(olderRoomBoxes)),
                    level.getRandom(), QUIET_STRUCTURE_FLAGS)) {
                // Restore the custom terrain block if a planned placement fails.
                // Restore all planned volumes if the template API reports failure.
                for (Placed rollback : layout) {
                    var b = rollback.box;
                    for (BlockPos p : BlockPos.betweenClosed(b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ())) {
                        level.setBlock(p, SchizmStoneRegistration.SCHIZM_STONE.defaultBlockState(), QUIET_STRUCTURE_FLAGS);
                    }
                }
                Shamaschizm.LOGGER.error("Dungeon placement failed for {} at {}", room.room.name, room.origin);
                return new Result(false, 0);
            }
            olderRoomBoxes.add(room.box);
            for (PlantedSwordEntity sword : level.getEntitiesOfClass(PlantedSwordEntity.class, swordBounds)) {
                if (!existingSwords.contains(sword.getUUID())) sword.markGeneratedInRoom();
            }
        }
        // Structure placement deliberately skips onPlace and neighbor updates. Correct
        // every generated portal component explicitly without waking redstone.
        orientGeneratedPortals(level, layout);
        // A structure file can contain a used vault, an old-format config, or an
        // interrupted ejection state. Recreate each vault so every generated room
        // starts with empty player history and a valid Schizm-specific config.
        for (Placed room : layout) {
            configureVaults(level, room, layout);
        }
        for(Placed room:layout){
            if(room.room.name.equals("shamaschizm:arena2"))
                net.beamex.shamaschizm.saints.audio.ArenaZones.get(level).add(room.box,room.origin,room.origin,true);
            if(room.room.name.equals("shamaschizm:entrance2") && room.room.entrance!=null && room.room.exits.size()==1)
                net.beamex.shamaschizm.saints.audio.ArenaZones.get(level).add(room.box,
                    room.origin.offset(rotate(room.room.entrance,room.rotation)),
                    room.origin.offset(rotate(room.room.exits.getFirst().local,room.rotation)),false);
        }
        // Do this last: aligned green/red markers can occupy the very same block.
        for (Placed room : layout) {
            for (BlockPos marker : room.room.markers) {
                BlockPos pos = room.origin.offset(rotate(marker, room.rotation));
                // A newer marker must not carve a hole in an older wall or portal.
                if (ownsPosition(layout, room, pos)) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), QUIET_STRUCTURE_FLAGS);
                }
            }
        }
        for (Placed room : layout) net.beamex.shamaschizm.world.SchizmTrapUpdates.queueRoom(level,room.box);
        for (Placed room : layout) {
            if (!room.room.name.equals("shamaschizm:garden1") && !room.room.name.equals("shamaschizm:arena2")
                    && room.room.entry.tag().equals(GRAND_ROOM_TAG)
                    && random.nextFloat() < GRAND_RIDER_CHANCE) {
                spawnGrandRoomRider(level, room, random);
            }
        }
        for (Placed room : layout) {
            if (room.room.name.equals("shamaschizm:garden1")) {
                GardenData.get(level).add(room.box);
                GardenZoneEntity zone = GardenZoneRegistration.GARDEN_ZONE.create(
                        level, EntitySpawnReason.STRUCTURE);
                if (zone != null) {
                    zone.configure(room.box);
                    if (!level.addFreshEntity(zone)) {
                        Shamaschizm.LOGGER.warn("Could not create garden audio zone at {}", room.origin);
                    }
                }
            }
        }
        for(Placed arena:layout)if(arena.room.name.equals("shamaschizm:arena2")){
            List<OpenExit> exits=new ArrayList<>();addExits(arena,exits);
            for(Placed garden:layout)if(garden.room.name.equals("shamaschizm:garden1")){
                BlockPos doorway=garden.origin.offset(rotate(garden.room.entrance,garden.rotation));
                if(exits.stream().anyMatch(e->e.position.equals(doorway)))
                    net.beamex.shamaschizm.experimental.BossGardenLocks.get(level).add(level,arena.box,garden.box,doorway);
            }
        }
        spawnRouteElves(level, layout);
        Shamaschizm.LOGGER.info("Schizm dungeon at {}: {} + {} rooms", origin, prepared.start.name, layout.size() - 1);
        return new Result(true, layout.size() - 1);
    }

    /** Breadth-first traversal counts rooms equally, including red-to-red loop links. */
    private static void spawnRouteElves(ServerLevel level, List<Placed> layout) {
        record Door(int room, BlockPos pos, Direction facing) {}
        List<Door> doors = new ArrayList<>();
        for (int i=0;i<layout.size();i++) {
            Placed room=layout.get(i);
            if(room.room.entrance!=null) doors.add(new Door(i,
                    room.origin.offset(rotate(room.room.entrance,room.rotation)),room.rotation.rotate(Direction.NORTH)));
            List<OpenExit> exits=new ArrayList<>();addExits(room,exits);
            for(OpenExit exit:exits)doors.add(new Door(i,exit.position,exit.outward));
        }
        int[] parent=new int[layout.size()];java.util.Arrays.fill(parent,-1);
        BlockPos[] connection=new BlockPos[layout.size()];
        java.util.ArrayDeque<Integer> queue=new java.util.ArrayDeque<>();queue.add(0);parent[0]=0;
        int garden=-1;
        while(!queue.isEmpty()) {
            int current=queue.removeFirst();
            if(layout.get(current).room.name.equals("shamaschizm:garden1")){garden=current;break;}
            for(Door a:doors)if(a.room==current)for(Door b:doors)
                if(parent[b.room]<0 && a.pos.equals(b.pos) && a.facing==b.facing.getOpposite()) {
                    parent[b.room]=current;connection[b.room]=a.pos;queue.addLast(b.room);
                }
        }
        if(garden<0)return;
        List<Integer> indices=new ArrayList<>();
        for(int i=garden;;i=parent[i]){indices.add(i);if(i==0)break;}
        java.util.Collections.reverse(indices);
        List<BlockPos> points=new ArrayList<>();
        Placed first=layout.get(0);BlockPos start=first.origin;
        for(BlockPos p:BlockPos.betweenClosed(first.box.minX(),first.box.minY(),first.box.minZ(),
                first.box.maxX(),first.box.maxY(),first.box.maxZ()))
            if(level.getBlockState(p).is(ModBlocks.SCHIZM_PORTAL)){start=p.immutable();break;}
        points.add(start);
        List<BoundingBox> boxes=new ArrayList<>();
        for(int index:indices){boxes.add(layout.get(index).box);if(index!=0)points.add(connection[index]);}
        Placed end=layout.get(garden);
        points.add(connection[garden].relative(end.rotation.rotate(Direction.SOUTH),3));
        net.beamex.shamaschizm.elf.ElfRoutes.spawn(level,points,boxes);
    }

    private static void orientGeneratedPortals(ServerLevel level, List<Placed> layout) {
        for (Placed room : layout) {
            BoundingBox box = room.box;
            for (BlockPos pos : BlockPos.betweenClosed(
                    box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
                if (level.getBlockState(pos).is(ModBlocks.SCHIZM_PORTAL)) {
                    SchizmPortalBlock.reorientConnected(level, pos);
                }
            }
        }
    }

    private static void spawnGrandRoomRider(ServerLevel level, Placed room, Random random) {
        EntityType<? extends Mob> mountType = random.nextBoolean()
                ? EntityTypes.CAMEL_HUSK
                : EntityTypes.ZOMBIE_HORSE;
        BlockPos spawnPos = findGrandRoomSpawn(level, room, mountType);
        if (spawnPos == null) {
            Shamaschizm.LOGGER.warn("Grand-room rider skipped: no suitable open floor in {} at {}",
                    room.room.name, room.origin);
            return;
        }

        Mob mount = mountType.create(level, EntitySpawnReason.STRUCTURE);
        Zombie rider = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.STRUCTURE);
        if (mount == null || rider == null) {
            Shamaschizm.LOGGER.warn("Grand-room rider skipped: entity creation failed in {}",
                    room.room.name);
            return;
        }

        float yaw = random.nextInt(4) * 90.0F;
        double x = spawnPos.getX() + 0.5D;
        double y = spawnPos.getY();
        double z = spawnPos.getZ() + 0.5D;
        mount.snapTo(x, y, z, yaw, 0.0F);
        rider.snapTo(x, y, z, yaw, 0.0F);
        mount.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos),
                EntitySpawnReason.STRUCTURE, null);
        rider.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos),
                EntitySpawnReason.STRUCTURE, null);
        rider.setBaby(false);

        mount.setPersistenceRequired();
        rider.setPersistenceRequired();
        equipGrandRoomRider(level, rider);
        rider.addEffect(new MobEffectInstance(MobEffects.STRENGTH, -1, 0, false, false));
        rider.startRiding(mount, true, false);

        if (!level.tryAddFreshEntityWithPassengers(mount)) {
            Shamaschizm.LOGGER.warn("Grand-room rider could not be added in {} at {}",
                    room.room.name, spawnPos);
        }
    }

    private static void equipGrandRoomRider(ServerLevel level, Zombie rider) {
        RandomSource random = rider.getRandom();
        ItemStack helmet = veryDamagedAncientArmor(EquipmentSlot.HEAD, random);
        HolderLookup.RegistryLookup<Enchantment> enchantments = level.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT);
        helmet.enchant(enchantments.getOrThrow(Enchantments.FIRE_PROTECTION), 7);
        helmet.enchant(enchantments.getOrThrow(Enchantments.THORNS), 2);

        rider.setItemSlot(EquipmentSlot.HEAD, helmet);
        rider.setItemSlot(EquipmentSlot.CHEST,
                veryDamagedAncientArmor(EquipmentSlot.CHEST, random));
        rider.setItemSlot(EquipmentSlot.LEGS,
                veryDamagedAncientArmor(EquipmentSlot.LEGS, random));
        rider.setItemSlot(EquipmentSlot.FEET,
                veryDamagedAncientArmor(EquipmentSlot.FEET, random));
        rider.setItemSlot(EquipmentSlot.MAINHAND,
                veryDamaged(new ItemStack(Items.NETHERITE_SPEAR), random));
        rider.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
    }

    private static ItemStack veryDamagedAncientArmor(EquipmentSlot slot, RandomSource random) {
        ItemStack stack = new ItemStack(switch (slot) {
            case HEAD -> Items.NETHERITE_HELMET;
            case CHEST -> Items.NETHERITE_CHESTPLATE;
            case LEGS -> Items.NETHERITE_LEGGINGS;
            case FEET -> Items.NETHERITE_BOOTS;
            default -> throw new IllegalArgumentException("Not an armor slot: " + slot);
        });
        stack.set(ModDataComponents.RAISED_ENCHANT_CAP, true);
        stack.set(ModDataComponents.ANCIENT_APPEARANCE, true);
        int remaining = 10 + random.nextInt(11);
        stack.setDamageValue(stack.getMaxDamage() - remaining);
        return stack;
    }

    /** Leaves only 2–5 percent durability, but never creates an already-broken item. */
    private static ItemStack veryDamaged(ItemStack stack, RandomSource random) {
        int remaining = Math.max(1,
                Math.round(stack.getMaxDamage() * (0.02F + random.nextFloat() * 0.03F)));
        stack.setDamageValue(stack.getMaxDamage() - remaining);
        return stack;
    }

    private static BlockPos findGrandRoomSpawn(ServerLevel level, Placed room,
                                                EntityType<? extends Mob> mountType) {
        BoundingBox box = room.box;
        BlockPos best = null;
        double bestScore = Double.MAX_VALUE;
        double centerX = (box.minX() + box.maxX()) * 0.5D;
        double centerZ = (box.minZ() + box.maxZ()) * 0.5D;

        // Five clear blocks accommodate both the camel husk and its mounted zombie.
        for (int y = box.minY() + 1; y <= box.maxY() - 4; y++) {
            for (int x = box.minX() + 1; x <= box.maxX() - 1; x++) {
                for (int z = box.minZ() + 1; z <= box.maxZ() - 1; z++) {
                    BlockPos candidate = new BlockPos(x, y, z);
                    BlockPos floor = candidate.below();
                    if (!level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)) continue;

                    boolean clear = true;
                    for (int above = 0; above < 5; above++) {
                        BlockPos occupied = candidate.above(above);
                        if (!level.getBlockState(occupied).getCollisionShape(level, occupied).isEmpty()
                                || !level.getFluidState(occupied).isEmpty()) {
                            clear = false;
                            break;
                        }
                    }
                    if (!clear || !level.noCollision(mountType.getSpawnAABB(
                            x + 0.5D, y, z + 0.5D))) continue;

                    double deltaX = x + 0.5D - centerX;
                    double deltaZ = z + 0.5D - centerZ;
                    double score = deltaX * deltaX + deltaZ * deltaZ
                            + (y - box.minY()) * 0.01D;
                    if (score < bestScore) {
                        best = candidate;
                        bestScore = score;
                    }
                }
            }
        }
        return best;
    }

    private static Placed attach(Room room, OpenExit exit) {
        for (Rotation rotation : Rotation.values()) {
            if (rotation.rotate(Direction.NORTH) == exit.outward.getOpposite()) {
                return placed(room, exit.position.subtract(rotate(room.entrance, rotation)), rotation);
            }
        }
        throw new IllegalStateException("Exit must face horizontally");
    }

    private static boolean available(ServerLevel level, Placed candidate, OpenExit exit,
                                     List<Placed> layout, BlockPos center) {
        BoundingBox b = candidate.box;
        // The teleporter uses this same reservation radius to separate dungeons.
        return b.minX() >= (long) center.getX() - RESERVATION_RADIUS
                && b.maxX() <= (long) center.getX() + RESERVATION_RADIUS
                && b.minZ() >= (long) center.getZ() - RESERVATION_RADIUS
                && b.maxZ() <= (long) center.getZ() + RESERVATION_RADIUS
                && fits(candidate, exit, layout) && untouched(level, b);
    }

    private static Placed chooseRoom(ServerLevel level, OpenExit exit, List<Placed> layout,
                                     Prepared prepared, Random random, BlockPos center,
                                     List<Room> tried) {
        return chooseRoom(level, exit, layout, prepared, random, center, tried, false);
    }

    private static Placed chooseRoom(ServerLevel level, OpenExit exit, List<Placed> layout,
                                     Prepared prepared, Random random, BlockPos center,
                                     List<Room> tried, boolean deadEndOnly) {
        List<Placed> candidates = new ArrayList<>();
        List<Double> weights = new ArrayList<>();
        for (Room room : prepared.choices) {
            if (room.name.equals("shamaschizm:garden1")) continue;
            if (deadEndOnly) {
                if (!room.exits.isEmpty()) continue;
            } else if (layout.size() - 1 < FIRST_ROOMS_WITH_EXITS && room.exits.isEmpty()) continue;
            Placed candidate = attach(room, exit);
            double weight = prepared.config.weight(room.entry, candidate.box.minY(), exit.parent.room.entry.tag());
            if (weight > 0) {
                candidates.add(candidate);
                weights.add(weight);
            }
        }
        // Try different templates at this exit before allowing repeats.
        if (!candidates.isEmpty() && candidates.stream().allMatch(candidate -> tried.contains(candidate.room))) {
            tried.clear();
        }
        for (int i = candidates.size() - 1; i >= 0; i--) {
            if (tried.contains(candidates.get(i).room)) {
                candidates.remove(i);
                weights.remove(i);
            }
        }
        int index = DungeonRoomConfig.chooseIndex(weights, random);
        if (index < 0) return null;
        Placed candidate = candidates.get(index);
        tried.add(candidate.room);
        return available(level, candidate, exit, layout, center) ? candidate : null;
    }

    /** After a failure, uniformly choose any OTHER available connection first. */
    private static OpenExit removeRandomExit(List<OpenExit> exits, OpenExit avoid, Random random) {
        int excluded = avoid == null ? -1 : exits.indexOf(avoid);
        if (excluded < 0 || exits.size() == 1) return exits.remove(random.nextInt(exits.size()));
        int selected = random.nextInt(exits.size() - 1);
        if (selected >= excluded) selected++;
        return exits.remove(selected);
    }

    private static OpenExit removeLowest(List<OpenExit> exits) {
        int lowest = 0;
        for (int i = 1; i < exits.size(); i++) {
            if (exits.get(i).position.getY() < exits.get(lowest).position.getY()) lowest = i;
        }
        return exits.remove(lowest);
    }

    private static List<Room> arenaRoute(ServerLevel level,String tag){
        List<Room> route=new ArrayList<>();
        for(String name:List.of("entrance2","arena2","garden1"))route.add(load(level,new DungeonRoomConfig.Entry("shamaschizm:"+name,tag,1.0),false));
        for(int i=0;i<2;i++)if(route.get(i).exits.size()!=1)
            throw new IllegalArgumentException(route.get(i).name+" must have exactly one red-wool exit for the experimental linear route");
        return route;
    }
    public static String validateArenaRoute(ServerLevel level){
        try{arenaRoute(level,"normal");return null;}
        catch(Exception exception){return exception.getMessage()==null?exception.toString():exception.getMessage();}
    }
    private static List<Placed> planEnding(ServerLevel level,List<Room> ending,OpenExit root,List<Placed> layout,BlockPos center){
        List<Placed> proposed=new ArrayList<>(),trial=new ArrayList<>(layout);OpenExit exit=root;
        for(int i=0;i<ending.size();i++){
            Placed next=attach(ending.get(i),exit);
            if(!available(level,next,exit,trial,center))return null;
            proposed.add(next);trial.add(next);
            if(i+1<ending.size()){
                List<OpenExit> following=new ArrayList<>();addExits(next,following);
                if(following.size()!=1)return null;
                exit=following.get(0);
            }
        }
        return proposed;
    }

    private static void tryGarden(ServerLevel level, List<Placed> layout, List<OpenExit> roots,
                                  Prepared prepared, Random random, BlockPos center) {
        List<Room> ending;
        try {
            // Plan the whole optional route before committing any part of it.
            ending = net.beamex.shamaschizm.experimental.ExperimentalSettings.get(level).arenaGarden()
                    ? arenaRoute(level,prepared.start.entry.tag())
                    : List.of(load(level,new DungeonRoomConfig.Entry("shamaschizm:garden1",prepared.start.entry.tag(),1.0),false));
        } catch (Exception exception) {
            Shamaschizm.LOGGER.error("Garden skipped: {}", exception.toString());
            return;
        }
        while (!roots.isEmpty()) {
            List<OpenExit> branch = new ArrayList<>();
            branch.add(removeLowest(roots));
            Map<OpenExit, Integer> failures = new HashMap<>();
            Map<OpenExit, List<Room>> triedRooms = new HashMap<>();
            OpenExit lastFailedExit = null;
            while (!branch.isEmpty()) {
                OpenExit exit = lastFailedExit == null ? removeLowest(branch)
                        : removeRandomExit(branch, lastFailedExit, random);
                List<Placed> route=planEnding(level,ending,exit,layout,center);
                if (route!=null) {
                    layout.addAll(route);
                    Shamaschizm.LOGGER.info("Garden route planned: {}",route.stream().map(p->p.room.name).toList());
                    tryLargeLoop(level, layout, prepared, random, center);
                    capRemainingExits(level, layout, prepared, random, center);
                    return;
                }
                Placed next = chooseRoom(level, exit, layout, prepared, random, center,
                        triedRooms.computeIfAbsent(exit, key -> new ArrayList<>()));
                if (next != null) {
                    lastFailedExit = null;
                    failures.remove(exit);
                    triedRooms.remove(exit);
                    layout.add(next);
                    addExits(next, branch);
                } else {
                    lastFailedExit = exit;
                    if (failures.merge(exit, 1, Integer::sum) < FAILED_SEARCHES_PER_EXIT) {
                        branch.add(exit);
                    } else {
                        failures.remove(exit);
                        triedRooms.remove(exit);
                    }
                }
            }
        }
        // No arbitrary room-count cutoff: every branch ends at dead ends or spatial limits.
        Shamaschizm.LOGGER.info("All available garden exits exhausted; retaining dungeon without garden");
    }

    private record Connection(BlockPos position, Direction outward) {}

    private record ExitPair(OpenExit start, OpenExit target, long distanceSquared) {}

    private record LoopHalf(List<Placed> rooms, OpenExit frontier, double priority) {}

    private record LoopPlan(List<Placed> rooms, OpenExit start, OpenExit target) {}

    private static final class LoopSelection {
        private List<LoopPlan> best = List.of();
        private int visited;
    }

    /** Reconstructs unused exits, including connections exhausted during garden search. */
    private static List<OpenExit> remainingExits(List<Placed> layout) {
        Set<Connection> used = new HashSet<>();
        List<OpenExit> open = new ArrayList<>();
        for (Placed room : layout) {
            addExits(room, open);
            if (room.room.entrance != null) {
                used.add(new Connection(room.origin.offset(rotate(room.room.entrance, room.rotation)),
                        room.rotation.rotate(Direction.NORTH).getOpposite()));
            }
        }
        open.removeIf(exit -> used.contains(new Connection(exit.position, exit.outward)));

        // A loop finishes by overlaying two red exit markers with opposing
        // outward directions. Treat both sides as used just like a normal
        // red-to-green room connection.
        Set<Connection> seen = new HashSet<>();
        Set<Connection> joined = new HashSet<>();
        for (OpenExit exit : open) {
            Connection connection = new Connection(exit.position, exit.outward);
            Connection opposite = new Connection(exit.position, exit.outward.getOpposite());
            if (seen.contains(opposite)) {
                joined.add(connection);
                joined.add(opposite);
            } else {
                seen.add(connection);
            }
        }
        open.removeIf(exit -> joined.contains(new Connection(exit.position, exit.outward)));
        return open;
    }

    /** Finds loop candidates from both ends, then commits the best compatible set. */
    private static void tryLargeLoop(ServerLevel level, List<Placed> layout,
                                     Prepared prepared, Random random, BlockPos center) {
        List<OpenExit> open = remainingExits(layout);
        if (open.size() < 2) {
            Shamaschizm.LOGGER.info("Loop skipped: fewer than two unused exits remain");
            return;
        }

        List<ExitPair> pairs = loopPairs(open);
        Collections.shuffle(pairs, random);
        pairs.sort(Comparator
                .comparingInt((ExitPair pair) -> pair.start.parent == pair.target.parent ? 1 : 0)
                .thenComparingLong(ExitPair::distanceSquared));

        List<LoopPlan> candidates = new ArrayList<>();
        int pairAttempts = Math.min(MAX_LOOP_PAIR_ATTEMPTS, pairs.size());
        for (int index = 0; index < pairAttempts
                && candidates.size() < MAX_LOOP_CANDIDATES; index++) {
            ExitPair pair = pairs.get(index);
            candidates.addAll(findLoopPlans(level, layout, pair, prepared, random, center,
                    Math.min(MAX_LOOP_CANDIDATES_PER_PAIR,
                            MAX_LOOP_CANDIDATES - candidates.size())));
        }

        if (candidates.isEmpty()) {
            Shamaschizm.LOGGER.info(
                    "No bidirectional dungeon loop found after testing {} exit pairs",
                    pairAttempts);
            return;
        }

        // Examine low-conflict plans first, while preferring larger loops when
        // two plans block the same number of alternatives.
        // Sorting temporarily moves/duplicates entries in the backing array.
        // Never derive comparator keys by scanning that same mutable list.
        Map<LoopPlan, Integer> conflicts = new java.util.IdentityHashMap<>();
        for (LoopPlan plan : candidates) {
            conflicts.put(plan, conflictCount(plan, candidates));
        }
        candidates.sort(Comparator
                .comparingInt((LoopPlan plan) -> conflicts.get(plan))
                .thenComparing(Comparator.comparingInt(
                        (LoopPlan plan) -> plan.rooms.size()).reversed()));
        LoopSelection selection = new LoopSelection();
        selectCompatibleLoops(candidates, 0, new ArrayList<>(), selection);
        for (LoopPlan plan : selection.best) {
            layout.addAll(plan.rooms);
            Shamaschizm.LOGGER.info(
                    "Bidirectional dungeon loop committed between {} and {} using {} rooms",
                    plan.start.position, plan.target.position, plan.rooms.size());
        }
        Shamaschizm.LOGGER.info(
                "Dungeon loop pass selected {} compatible loop(s) from {} candidates ({} selection nodes)",
                selection.best.size(), candidates.size(), selection.visited);
    }

    private static List<ExitPair> loopPairs(List<OpenExit> open) {
        List<ExitPair> pairs = new ArrayList<>();
        long horizontalReach = (long) MAX_LOOP_ROOMS * MAX_ROOM_WIDTH;
        long verticalReach = (long) MAX_LOOP_ROOMS * MAX_ROOM_HEIGHT;
        for (int first = 0; first < open.size() - 1; first++) {
            for (int second = first + 1; second < open.size(); second++) {
                OpenExit a = open.get(first);
                OpenExit b = open.get(second);
                long dx = Math.abs((long) a.position.getX() - b.position.getX());
                long dy = Math.abs((long) a.position.getY() - b.position.getY());
                long dz = Math.abs((long) a.position.getZ() - b.position.getZ());
                if (dx > horizontalReach || dz > horizontalReach || dy > verticalReach) continue;
                long distanceSquared = dx * dx + dy * dy + dz * dz;
                pairs.add(new ExitPair(a, b, distanceSquared));
            }
        }
        return pairs;
    }

    private static List<LoopPlan> findLoopPlans(
            ServerLevel level, List<Placed> layout, ExitPair pair,
            Prepared prepared, Random random, BlockPos center, int limit) {
        int firstDepth = MAX_LOOP_ROOMS / 2;
        int secondDepth = MAX_LOOP_ROOMS - firstDepth;
        List<LoopHalf> first = buildLoopHalves(
                layout, pair.start, pair.target, prepared, random, center, firstDepth);
        List<LoopHalf> second = buildLoopHalves(
                layout, pair.target, pair.start, prepared, random, center, secondDepth);

        Map<Connection, List<LoopHalf>> secondByFrontier = new HashMap<>();
        for (LoopHalf half : second) {
            secondByFrontier.computeIfAbsent(connection(half.frontier), key -> new ArrayList<>())
                    .add(half);
        }

        List<LoopPlan> result = new ArrayList<>();
        for (LoopHalf left : first) {
            Connection wanted = new Connection(
                    left.frontier.position, left.frontier.outward.getOpposite());
            for (LoopHalf right : secondByFrontier.getOrDefault(wanted, List.of())) {
                int roomCount = left.rooms.size() + right.rooms.size();
                if (roomCount == 0 || roomCount > MAX_LOOP_ROOMS
                        || !halvesCompatible(left, right)) continue;
                List<Placed> rooms = new ArrayList<>(left.rooms);
                rooms.addAll(right.rooms);
                if (!pathUntouched(level, rooms)) continue;
                result.add(new LoopPlan(List.copyOf(rooms), pair.start, pair.target));
                if (result.size() >= limit) return result;
            }
        }
        return result;
    }

    private static List<LoopHalf> buildLoopHalves(
            List<Placed> layout, OpenExit root, OpenExit oppositeRoot,
            Prepared prepared, Random random, BlockPos center, int maxDepth) {
        LoopHalf initial = new LoopHalf(List.of(), root,
                squaredDistance(root.position, oppositeRoot.position));
        List<LoopHalf> all = new ArrayList<>();
        all.add(initial);
        List<LoopHalf> layer = List.of(initial);
        int retainedNodes = 1;

        for (int depth = 1; depth <= maxDepth
                && !layer.isEmpty()
                && retainedNodes < MAX_LOOP_SEARCH_NODES_PER_PAIR; depth++) {
            Map<Connection, List<LoopHalf>> nextByFrontier = new HashMap<>();
            for (LoopHalf state : layer) {
                List<Placed> occupied = new ArrayList<>(layout);
                occupied.addAll(state.rooms);
                for (Room room : prepared.choices) {
                    if (room.name.equals("shamaschizm:garden1") || room.exits.isEmpty()) continue;
                    Placed candidate = attach(room, state.frontier);
                    double weight = prepared.config.weight(room.entry, candidate.box.minY(),
                            state.frontier.parent.room.entry.tag());
                    if (weight <= 0.0D) continue;

                    List<OpenExit> exits = new ArrayList<>();
                    addExits(candidate, exits);
                    List<OpenExit> closing = exits.stream()
                            .filter(exit -> closes(exit, oppositeRoot)).toList();
                    if (!closing.isEmpty()) {
                        if (!geometricallyAvailableForClosure(candidate, state.frontier,
                                oppositeRoot, occupied, center)) continue;
                        exits = closing;
                    } else if (!geometricallyAvailable(
                            candidate, state.frontier, occupied, center)) {
                        continue;
                    }

                    List<Placed> rooms = new ArrayList<>(state.rooms);
                    rooms.add(candidate);
                    for (OpenExit exit : exits) {
                        double priority = squaredDistance(
                                exit.position, oppositeRoot.position) / Math.max(0.001D, weight);
                        retainHalf(nextByFrontier, new LoopHalf(
                                List.copyOf(rooms), exit, priority));
                    }
                }
            }

            List<LoopHalf> next = new ArrayList<>();
            for (List<LoopHalf> alternatives : nextByFrontier.values()) {
                next.addAll(alternatives);
            }
            Collections.shuffle(next, random);
            next.sort(Comparator.comparingDouble(LoopHalf::priority));
            if (next.size() > MAX_LOOP_BEAM_WIDTH) {
                next = new ArrayList<>(next.subList(0, MAX_LOOP_BEAM_WIDTH));
            }
            int remaining = MAX_LOOP_SEARCH_NODES_PER_PAIR - retainedNodes;
            if (next.size() > remaining) next = new ArrayList<>(next.subList(0, remaining));
            retainedNodes += next.size();
            all.addAll(next);
            layer = next;
        }
        return all;
    }

    private static void retainHalf(Map<Connection, List<LoopHalf>> states, LoopHalf candidate) {
        List<LoopHalf> alternatives = states.computeIfAbsent(
                connection(candidate.frontier), key -> new ArrayList<>());
        alternatives.add(candidate);
        alternatives.sort(Comparator.comparingDouble(LoopHalf::priority));
        if (alternatives.size() > MAX_LOOP_PATHS_PER_FRONTIER) {
            alternatives.removeLast();
        }
    }

    private static Connection connection(OpenExit exit) {
        return new Connection(exit.position, exit.outward);
    }

    private static boolean halvesCompatible(LoopHalf first, LoopHalf second) {
        // The root itself is retained as a zero-room half so the other side can
        // legitimately complete the whole connection. There can be no
        // cross-half room collision when either side contains no rooms.
        if (first.rooms.isEmpty() || second.rooms.isEmpty()) return true;
        for (Placed left : first.rooms) {
            for (Placed right : second.rooms) {
                if (!left.box.intersects(right.box)) continue;
                boolean finalRooms = left == first.rooms.getLast()
                        && right == second.rooms.getLast();
                if (!finalRooms || !sharesConnectionWall(
                        left.box, right.box, first.frontier)) return false;
            }
        }
        return true;
    }

    private static int conflictCount(LoopPlan plan, List<LoopPlan> candidates) {
        int conflicts = 0;
        for (LoopPlan other : candidates) {
            if (plan != other && !plansCompatible(plan, other)) conflicts++;
        }
        return conflicts;
    }

    private static void selectCompatibleLoops(List<LoopPlan> candidates, int index,
                                              List<LoopPlan> selected,
                                              LoopSelection result) {
        if (++result.visited > MAX_LOOP_SELECTION_NODES) return;
        if (betterSelection(selected, result.best)) result.best = List.copyOf(selected);
        if (selected.size() >= MAX_SUCCESSFUL_LOOPS || index >= candidates.size()
                || selected.size() + candidates.size() - index < result.best.size()) return;

        LoopPlan candidate = candidates.get(index);
        if (selected.stream().allMatch(existing -> plansCompatible(existing, candidate))) {
            selected.add(candidate);
            selectCompatibleLoops(candidates, index + 1, selected, result);
            selected.removeLast();
        }
        selectCompatibleLoops(candidates, index + 1, selected, result);
    }

    private static boolean betterSelection(List<LoopPlan> candidate, List<LoopPlan> current) {
        if (candidate.size() != current.size()) return candidate.size() > current.size();
        return candidate.stream().mapToInt(plan -> plan.rooms.size()).sum()
                > current.stream().mapToInt(plan -> plan.rooms.size()).sum();
    }

    private static boolean plansCompatible(LoopPlan first, LoopPlan second) {
        if (sameExit(first.start, second.start) || sameExit(first.start, second.target)
                || sameExit(first.target, second.start) || sameExit(first.target, second.target)) {
            return false;
        }
        for (Placed firstRoom : first.rooms) {
            for (Placed secondRoom : second.rooms) {
                if (firstRoom.box.intersects(secondRoom.box)) return false;
            }
        }
        return true;
    }

    private static boolean sameExit(OpenExit first, OpenExit second) {
        return first.position.equals(second.position) && first.outward == second.outward;
    }

    private static boolean closes(OpenExit exit, OpenExit target) {
        return exit.position.equals(target.position)
                && exit.outward == target.outward.getOpposite();
    }

    private static boolean withinLoopReach(BlockPos from, BlockPos to, int roomsRemaining) {
        long horizontalReach = (long) roomsRemaining * MAX_ROOM_WIDTH;
        long verticalReach = (long) roomsRemaining * MAX_ROOM_HEIGHT;
        return Math.abs((long) from.getX() - to.getX()) <= horizontalReach
                && Math.abs((long) from.getZ() - to.getZ()) <= horizontalReach
                && Math.abs((long) from.getY() - to.getY()) <= verticalReach;
    }

    private static long squaredDistance(BlockPos first, BlockPos second) {
        long dx = (long) first.getX() - second.getX();
        long dy = (long) first.getY() - second.getY();
        long dz = (long) first.getZ() - second.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    /** Fast geometry-only test used while exploring speculative loop branches. */
    private static boolean geometricallyAvailable(Placed candidate, OpenExit connection,
                                                  List<Placed> layout, BlockPos center) {
        return insideReservation(candidate.box, center) && fits(candidate, connection, layout);
    }

    /** Allows the final room to share one wall with each end of the loop. */
    private static boolean geometricallyAvailableForClosure(
            Placed candidate, OpenExit connection, OpenExit target,
            List<Placed> layout, BlockPos center) {
        if (!insideReservation(candidate.box, center)) return false;
        for (Placed other : layout) {
            if (!candidate.box.intersects(other.box)) continue;
            boolean allowed = other == connection.parent
                    && sharesConnectionWall(candidate.box, other.box, connection);
            allowed |= other == target.parent
                    && sharesConnectionWall(candidate.box, other.box, target);
            if (!allowed) return false;
        }
        return true;
    }

    private static boolean insideReservation(BoundingBox box, BlockPos center) {
        return box.minX() >= (long) center.getX() - RESERVATION_RADIUS
                && box.maxX() <= (long) center.getX() + RESERVATION_RADIUS
                && box.minZ() >= (long) center.getZ() - RESERVATION_RADIUS
                && box.maxZ() <= (long) center.getZ() + RESERVATION_RADIUS;
    }

    private static boolean sharesConnectionWall(BoundingBox first, BoundingBox second,
                                                OpenExit connection) {
        int coordinate = connection.outward.getAxis() == Direction.Axis.X
                ? connection.position.getX() : connection.position.getZ();
        int low = connection.outward.getAxis() == Direction.Axis.X
                ? Math.max(first.minX(), second.minX()) : Math.max(first.minZ(), second.minZ());
        int high = connection.outward.getAxis() == Direction.Axis.X
                ? Math.min(first.maxX(), second.maxX()) : Math.min(first.maxZ(), second.maxZ());
        return low == coordinate && high == coordinate;
    }

    private static boolean pathUntouched(ServerLevel level, List<Placed> path) {
        for (Placed room : path) {
            if (!untouched(level, room.box)) return false;
        }
        return true;
    }

    /** Runs after the optional loop search; endpoint rooms are extra to the room target. */
    private static void capRemainingExits(ServerLevel level, List<Placed> layout,
                                          Prepared prepared, Random random, BlockPos center) {
        List<OpenExit> pending = remainingExits(layout);
        Map<OpenExit, Integer> failures = new HashMap<>();
        Map<OpenExit, List<Room>> tried = new HashMap<>();
        OpenExit lastFailed = null;
        while (!pending.isEmpty()) {
            OpenExit exit = removeRandomExit(pending, lastFailed, random);
            Placed cap = chooseRoom(level, exit, layout, prepared, random, center,
                    tried.computeIfAbsent(exit, key -> new ArrayList<>()), true);
            if (cap != null) {
                layout.add(cap);
                failures.remove(exit);
                tried.remove(exit);
                lastFailed = null;
                // Cap rooms have no exits, so never extend this work list.
            } else {
                lastFailed = exit;
                if (failures.merge(exit, 1, Integer::sum) < DEAD_END_ATTEMPTS_PER_EXIT) {
                    pending.add(exit);
                } else {
                    failures.remove(exit);
                    tried.remove(exit);
                }
            }
        }
    }

    /** The first planned room containing a position owns it, including its air. */
    private static boolean ownsPosition(List<Placed> layout, Placed room, BlockPos pos) {
        for (Placed candidate : layout) {
            if (candidate.box.isInside(pos)) return candidate == room;
        }
        return false;
    }

    private static void configureVaults(ServerLevel level, Placed room, List<Placed> layout) {
        for (BlockPos local : room.room.vaults) {
            BlockPos pos = room.origin.offset(rotate(local, room.rotation));
            if (!ownsPosition(layout, room, pos)) continue;
            var state = level.getBlockState(pos);
            if (!state.is(Blocks.VAULT)) {
                Shamaschizm.LOGGER.warn("Expected a vault from {} at {}, but found {}",
                        room.room.name, pos, state.getBlock());
                continue;
            }

            // Preserve FACING and OMINOUS, but erase saved vault progress/configuration.
            var cleanState = state.setValue(VaultBlock.STATE, VaultState.INACTIVE);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), QUIET_STRUCTURE_FLAGS);
            level.setBlock(pos, cleanState, QUIET_STRUCTURE_FLAGS);

            if (level.getBlockEntity(pos) instanceof VaultBlockEntity vault) {
                vault.setConfig(cleanState.getValue(VaultBlock.OMINOUS)
                        ? SCHIZM_OMINOUS_VAULT_CONFIG
                        : SCHIZM_VAULT_CONFIG);
                vault.setChanged();
            } else {
                Shamaschizm.LOGGER.error("Failed to create Schizm vault block entity at {}", pos);
            }
        }
    }
}
