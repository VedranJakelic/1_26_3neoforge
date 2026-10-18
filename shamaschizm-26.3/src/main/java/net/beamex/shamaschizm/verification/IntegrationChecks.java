package net.beamex.shamaschizm.verification;

import com.mojang.authlib.GameProfile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.beamex.shamaschizm.*;
import net.beamex.shamaschizm.effect.ModEffects;
import net.beamex.shamaschizm.entity.ModEntities;
import net.beamex.shamaschizm.entity.custom.ShamanEntity;
import net.beamex.shamaschizm.event.ModItemBootstrap;
import net.beamex.shamaschizm.menu.ShamanTradeMenu;
import net.beamex.shamaschizm.registry.ModDataComponents;
import net.beamex.shamaschizm.world.*;
import net.beamex.shamaschizm.world.teleport.SchizmTeleporter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/** Development-only real-server checks. Excluded from the distributable jar. */
@EventBusSubscriber(modid = Shamaschizm.MOD_ID)
public final class IntegrationChecks {
    private static final List<String> checks = new ArrayList<>();
    private static Item foreignArmor;
    @SubscribeEvent public static void registerFixture(RegisterEvent event) {
        if (!Boolean.getBoolean("shamaschizm.verify")) return;
        event.register(Registries.ITEM, helper -> {
            var id = net.minecraft.resources.Identifier.fromNamespaceAndPath("verification_other_mod", "armor");
            foreignArmor = new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id))
                    .durability(512).component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.CHEST).build()));
            helper.register(id, foreignArmor);
        });
    }
    @SubscribeEvent public static void started(ServerStartedEvent event) {
        if (!Boolean.getBoolean("shamaschizm.verify")) return;
        MinecraftServer server = event.getServer();
        try {
            souls(server.overworld());
            trades(server.overworld());
            server.setDifficulty(Difficulty.NORMAL, true);
            upgrades(server);
            portals(server);
            Files.write(Path.of("verification-results.txt"), checks);
            Shamaschizm.LOGGER.info("SHAMASCHIZM VERIFICATION PASSED: {} checks", checks.size());
        } catch (Throwable error) {
            checks.add("FAIL: " + error);
            try { Files.write(Path.of("verification-results.txt"), checks); } catch (Exception ignored) {}
            Shamaschizm.LOGGER.error("SHAMASCHIZM VERIFICATION FAILED", error);
        } finally { server.halt(false); }
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks.add("PASS: " + message);
    }
    private static FakePlayer player(ServerLevel level) {
        return new FakePlayer(level, new GameProfile(UUID.randomUUID(), "Verify"));
    }
    private static CompoundTag save(net.minecraft.world.entity.Entity entity, ServerLevel level) {
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        entity.saveWithoutId(output);
        return output.buildResult();
    }
    private static void souls(ServerLevel level) {
        var player = player(level);
        check(SoulData.getSouls(player) == 1, "new player starts with one soul");
        SoulData.setSouls(player, 0);
        player.getPersistentData().remove("shamaschizm_souls_init");
        SoulData.ensureInitialized(player);
        check(SoulData.getSouls(player) == 0, "initialization preserves a saved zero");
        var replacement = player(level);
        NeoForge.EVENT_BUS.post(new PlayerEvent.Clone(replacement, player, true));
        check(SoulData.getSouls(replacement) == 0, "death clone preserves zero souls");
        SoulData.setSouls(player, 3);
        NeoForge.EVENT_BUS.post(new PlayerEvent.Clone(replacement, player, true));
        check(SoulData.getSouls(replacement) == 3, "death clone preserves multiple souls");
        var reconnect = player(level);
        reconnect.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), save(player, level)));
        check(SoulData.getSouls(reconnect) == 3, "player serialization and reconnect preserve souls");
        var legacy = save(player, level);
        legacy.remove("NeoForgeData");
        var oldData = new CompoundTag(); oldData.putInt("shamaschizm_souls", 0); legacy.put("ForgeData", oldData);
        var imported = player(level);
        imported.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), legacy));
        check(SoulData.getSouls(imported) == 0, "old Forge save imports a legitimate zero soul balance");
        SoulData.setSouls(imported, 2);
        var both = save(imported, level); both.put("ForgeData", oldData);
        var current = player(level); current.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), both));
        check(SoulData.getSouls(current) == 2, "current NeoForge balance takes precedence over legacy data");
        player.setHealth(10); SoulData.setSouls(player, 0); player.heal(2);
        check(player.getHealth() == 10, "zero souls blocks healing through the registered event");
        SoulData.setSouls(player, 1); player.heal(2);
        check(player.getHealth() == 12, "healing resumes after regaining a soul");
    }
    private static ShamanTradeMenu menu(FakePlayer player, ShamanEntity shaman) {
        shaman.setPos(player.position()); shaman.setTradingPlayer(player);
        var menu = new ShamanTradeMenu(1, player.getInventory(), shaman);
        player.containerMenu = menu;
        return menu;
    }
    private static void trades(ServerLevel level) {
        var player = player(level);
        var shaman = new ShamanEntity(ModEntities.SHAMAN.get(), level);
        check(shaman.isPersistenceRequired() && !shaman.removeWhenFarAway(100000), "shaman opts out of distance despawning");
        level.getServer().setDifficulty(Difficulty.PEACEFUL, true);
        shaman.checkDespawn();
        check(!shaman.isRemoved(), "shaman survives the peaceful-difficulty despawn check");
        var restored = new ShamanEntity(ModEntities.SHAMAN.get(), level);
        restored.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), save(shaman, level)));
        check(restored.isPersistenceRequired(), "shaman remains persistent after save/load");
        var menu = menu(player, shaman);
        menu.clicked(2, 0, ContainerInput.SWAP, player);
        check(SoulData.getSouls(player) == 1 && menu.getCarried().isEmpty(), "number-key result clicks cannot bypass soul trade logic");
        menu.clicked(2, 0, ContainerInput.PICKUP, player);
        check(SoulData.getSouls(player) == 0 && menu.getCarried().is(Items.ENCHANTED_BOOK), "soul trade pays exactly once and returns the book");
        menu.clicked(2, 0, ContainerInput.PICKUP, player);
        check(SoulData.getSouls(player) == 0 && menu.getCarried().getCount() == 1, "empty soul balance cannot repeat the trade");
        menu.setCarried(ItemStack.EMPTY); SoulData.setSouls(player, 1);
        for (int i=0;i<36;i++) player.getInventory().setItem(i,new ItemStack(Items.DIAMOND,64));
        menu.clicked(2,0,ContainerInput.QUICK_MOVE,player);
        check(SoulData.getSouls(player) == 1, "full inventory blocks shift trading before charging souls");
        player.getInventory().clearContent();
        menu.setSelectionHint(-1); menu.clicked(2,0,ContainerInput.PICKUP,player);
        check(SoulData.getSouls(player) == 1 && menu.getCarried().isEmpty(), "invalid trade index cannot pay out");
        menu.setSelectionHint(6); menu.broadcastChanges();
        check(menu.getSlot(2).getItem().isEmpty(), "blueprint trade is unavailable without tripping");
        player.addEffect(new MobEffectInstance(MobEffects.NAUSEA,200)); menu.broadcastChanges();
        check(menu.getSlot(2).getItem().isEmpty(), "ordinary nausea does not unlock mushroom trades");
        player.addEffect(new MobEffectInstance(ModEffects.TRIPPING,200)); menu.broadcastChanges();
        menu.clicked(2,0,ContainerInput.PICKUP,player);
        check(SoulData.getSouls(player) == 0 && menu.getCarried().is(ModItemBootstrap.ANCIENT_BLUEPRINT), "tripping unlocks the blueprint and consumes one soul");
        menu.setCarried(ItemStack.EMPTY); menu.setSelectionHint(7);
        menu.getSlot(0).set(new ItemStack(Items.NETHER_STAR)); menu.broadcastChanges();
        menu.clicked(2,0,ContainerInput.PICKUP,player);
        check(SoulData.getSouls(player) == 1 && menu.getSlot(0).getItem().isEmpty() && menu.getCarried().isEmpty(), "nether star trade restores a soul without giving a soul item");
        menu.setSelectionHint(0); menu.removed(player);
        check(player.getInventory().isEmpty(), "closing a currency trade never returns its display soul to inventory");
        check(shaman.getTradingPlayer() == null, "closing the menu releases the shaman for another player");
    }
    private static Holder<Enchantment> enchant(ServerLevel level, ResourceKey<Enchantment> id) {
        return level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(id);
    }
    private static ItemStack book(Holder<Enchantment> enchantment, int level) {
        ItemStack stack = new ItemStack(Items.ENCHANTED_BOOK);
        var value = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY); value.set(enchantment, level);
        stack.set(DataComponents.STORED_ENCHANTMENTS, value.toImmutable()); return stack;
    }
    private static ItemStack anvil(FakePlayer player, ItemStack left, ItemStack right) {
        var event = new AnvilUpdateEvent(left,right,null,ItemStack.EMPTY,0,0,player);
        NeoForge.EVENT_BUS.post(event);
        check(event.getXpCost() > 0 && event.getXpCost() < 40 && event.getMaterialCost() == 1, "anvil upgrade has a usable cost and consumes one material");
        return event.getOutput();
    }
    private static void upgrades(MinecraftServer server) {
        var level = server.overworld(); var player = player(level);
        for (Item item : List.of(Items.DIAMOND_CHESTPLATE, Items.DIAMOND_PICKAXE, foreignArmor)) {
            var original = new ItemStack(item); original.setDamageValue(17);
            original.set(DataComponents.CUSTOM_NAME,Component.literal("Original name"));
            CompoundTag extra = new CompoundTag(); extra.putString("foreign_mod_data","preserve me");
            original.set(DataComponents.CUSTOM_DATA,CustomData.of(extra));
            var input = new SmithingRecipeInput(new ItemStack(ModItemBootstrap.ANCIENT_BLUEPRINT),original,new ItemStack(Items.LAPIS_LAZULI));
            var recipe = server.getRecipeManager().getRecipeFor(RecipeType.SMITHING,input,level);
            check(recipe.isPresent(), "loaded smithing recipe accepts " + item);
            var upgraded = recipe.orElseThrow().value().assemble(input);
            check(upgraded.is(item) && upgraded.getDamageValue()==17 && upgraded.getHoverName().getString().equals("Original name")
                    && upgraded.get(DataComponents.CUSTOM_DATA).equals(original.get(DataComponents.CUSTOM_DATA)), "upgrade preserves original identity, damage, name, and foreign data");
            check(upgraded.getOrDefault(ModDataComponents.RAISED_ENCHANT_CAP,false)
                    && !original.getOrDefault(ModDataComponents.RAISED_ENCHANT_CAP,false), "upgrade marks the output without mutating the input");
            check(!upgraded.getOrDefault(ModDataComponents.ANCIENT_APPEARANCE,false),
                    "non-netherite equipment does not receive the ancient armor appearance");
            var ops=level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
            var saved=ItemStack.CODEC.encodeStart(ops,upgraded).getOrThrow();
            var loaded=ItemStack.CODEC.parse(ops,saved).getOrThrow();
            check(loaded.getOrDefault(ModDataComponents.RAISED_ENCHANT_CAP,false), "upgrade survives item serialization");
            var smithing = new net.minecraft.world.inventory.SmithingMenu(2,player.getInventory());
            check(smithing.getSlot(0).mayPlace(input.template()) && smithing.getSlot(1).mayPlace(original)
                    && smithing.getSlot(2).mayPlace(input.addition()), "smithing UI accepts template, equipment, and lapis");
            smithing.getSlot(0).set(new ItemStack(ModItemBootstrap.ANCIENT_BLUEPRINT,2));
            smithing.getSlot(1).set(original.copy()); smithing.getSlot(2).set(new ItemStack(Items.LAPIS_LAZULI,3));
            check(smithing.getSlot(3).getItem().getOrDefault(ModDataComponents.RAISED_ENCHANT_CAP,false), "smithing UI produces the marked output");
            smithing.clicked(3,0,ContainerInput.PICKUP,player);
            check(smithing.getCarried().is(item) && smithing.getSlot(0).getItem().getCount()==1
                    && smithing.getSlot(1).getItem().isEmpty() && smithing.getSlot(2).getItem().getCount()==2,
                    "taking the smithing result consumes exactly one of each input");
            smithing.clicked(3,0,ContainerInput.PICKUP,player);
            check(smithing.getCarried().getCount()==1, "empty smithing inputs cannot duplicate the result");
            var again = new SmithingRecipeInput(input.template(),loaded,input.addition());
            check(server.getRecipeManager().getRecipeFor(RecipeType.SMITHING,again,level).isEmpty(), "already upgraded equipment cannot consume another blueprint");
        }
        var netherite = new ItemStack(Items.NETHERITE_CHESTPLATE);
        netherite.setDamageValue(23);
        var ancientInput = new SmithingRecipeInput(
                new ItemStack(ModItemBootstrap.ANCIENT_BLUEPRINT), netherite, new ItemStack(Items.LAPIS_LAZULI));
        var ancient = server.getRecipeManager().getRecipeFor(RecipeType.SMITHING, ancientInput, level)
                .orElseThrow().value().assemble(ancientInput);
        check(ancient.is(Items.NETHERITE_CHESTPLATE) && ancient.getDamageValue() == 23
                        && ancient.getOrDefault(ModDataComponents.RAISED_ENCHANT_CAP, false)
                        && ancient.getOrDefault(ModDataComponents.ANCIENT_APPEARANCE, false),
                "netherite armor keeps its mechanics and receives the ancient appearance");
        var ancientOps = level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        var reloadedAncient = ItemStack.CODEC.parse(ancientOps,
                ItemStack.CODEC.encodeStart(ancientOps, ancient).getOrThrow()).getOrThrow();
        check(reloadedAncient.getOrDefault(ModDataComponents.ANCIENT_APPEARANCE, false),
                "ancient armor appearance survives item serialization");
        var fortune=enchant(level,Enchantments.FORTUNE);
        var gear=new ItemStack(Items.DIAMOND_PICKAXE); gear.set(ModDataComponents.RAISED_ENCHANT_CAP,true);gear.enchant(fortune,6);
        for(int i=6;i<10;i++) gear=anvil(player,gear,book(fortune,i));
        check(gear.getOrDefault(DataComponents.ENCHANTMENTS,ItemEnchantments.EMPTY).getLevel(fortune)==10, "equal-level book merges reach Fortune X");
        var sharpness=enchant(level,Enchantments.SHARPNESS); var smite=enchant(level,Enchantments.SMITE);
        gear=anvil(player,gear,book(sharpness,10));gear=anvil(player,gear,book(smite,10));
        var enchants=gear.getOrDefault(DataComponents.ENCHANTMENTS,ItemEnchantments.EMPTY);
        check(enchants.getLevel(sharpness)==10 && enchants.getLevel(smite)==10 && enchants.getLevel(fortune)==10,
                "upgraded gear accepts conflicting and normally inapplicable enchants without losing existing enchants");
        var vanilla=new ItemStack(Items.DIAMOND_PICKAXE);
        var event=new AnvilUpdateEvent(vanilla,book(fortune,6),null,ItemStack.EMPTY,0,0,player);
        NeoForge.EVENT_BUS.post(event);
        check(event.getOutput().isEmpty(), "unmarked equipment is left to vanilla anvil behavior");
    }
    private static void portals(MinecraftServer server) {
        ServerLevel schizm=server.getLevel(Schizm.KEY);
        check(schizm!=null && schizm.dimensionType().coordinateScale()==0.125, "Schizm loads with the eightfold destination scale");
        var level=server.overworld();var player=player(level);
        BlockPos source=new BlockPos(64,90,64);
        for(int dx=-2;dx<=3;dx++)for(int dz=-2;dz<=2;dz++) {
            level.setBlock(source.offset(dx,-1,dz),Blocks.STONE.defaultBlockState(),3);
            for(int dy=0;dy<=3;dy++)level.setBlock(source.offset(dx,dy,dz),Blocks.AIR.defaultBlockState(),3);
        }
        for(int dx=0;dx<=1;dx++)for(int dy=0;dy<3;dy++)level.setBlock(source.offset(dx,dy,0),ModItemBootstrap.SCHIZM_PORTAL_BLOCK.defaultBlockState(),3);
        player.setPos(source.getX()+0.5,source.getY(),source.getZ()+0.5);
        SchizmTeleporter.teleportOverworldToSchizm(player,source.above());
        var link=PortalLinks.get(schizm).source(source);
        check(link!=null && Math.abs(link.doorway().getX()-512)<=16 && Math.abs(link.doorway().getZ()-512)<=16, "entry records a safe arrival beside the scaled canonical portal position");
        check(player.level()==schizm, "portal moves the player into the Schizm");
        var marker=link.doorway().offset(0,0,1);schizm.setBlock(marker,Blocks.GOLD_BLOCK.defaultBlockState(),3);
        SchizmTeleporter.teleportSchizmToOverworld(player,link.doorway().offset(0,1,7));
        check(player.level()==level && player.blockPosition().distSqr(source)<100, "return uses the saved source portal");
        SchizmTeleporter.teleportOverworldToSchizm(player,source.offset(1,2,0));
        check(schizm.getBlockState(marker).is(Blocks.GOLD_BLOCK), "re-entering a different portal block does not regenerate or overwrite the room");
        var ops=schizm.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        var stored=PortalLinks.TYPE.codec().encodeStart(ops,PortalLinks.get(schizm)).getOrThrow();
        var loaded=PortalLinks.TYPE.codec().parse(ops,stored).getOrThrow();
        check(loaded.source(source).doorway().equals(link.doorway()), "portal links survive saved-data serialization");
    }
}
