package net.beamex.shamaschizm.slab.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;

/** Per-listener substitution: never broadcasts the slab voice to other players. */
@EventBusSubscriber(modid = "shamaschizm", value = Dist.CLIENT)
public final class SlabZombieSoundClient {
    private static final double RANGE = 16.0D;
    private static final float CHANCE = 0.20F;
    private static final Identifier SLAB = Identifier.fromNamespaceAndPath("shamaschizm", "slab01");
    private static final Identifier VOICE = Identifier.fromNamespaceAndPath("shamaschizm", "zombie_return_slab");
    private static final RandomSource RANDOM = RandomSource.create();

    private SlabZombieSoundClient() {}

    @SubscribeEvent
    public static void onSound(PlaySoundEvent event) {
        SoundInstance sound = event.getSound();
        if (sound == null || sound.isRelative() || !isZombieIdle(sound.getIdentifier())) return;
        var player = Minecraft.getInstance().player;
        if (player == null || !player.isAlive()
                || player.distanceToSqr(sound.getX(), sound.getY(), sound.getZ()) > RANGE * RANGE) return;
        // Both visual slab variants use the same item. Inventory includes the offhand.
        if (!player.getInventory().contains(stack -> !stack.isEmpty()
                && SLAB.equals(BuiltInRegistries.ITEM.getKey(stack.getItem())))) return;
        if (RANDOM.nextFloat() >= CHANCE) return;

        // PlaySoundEvent fires BEFORE SoundEngine resolves the original instance.
        // getVolume() dereferences the selected sound, so resolve it first.
        if (sound.getSound() == null) sound.getOrResolve(Minecraft.getInstance().getSoundManager());
        if (sound.getSound() == null) return;

        // A normal positional sound, with the original category and volume.
        // Pitch 1 keeps the supplied voice intact, including for baby zombies.
        event.setSound(new SimpleSoundInstance(VOICE, sound.getSource(), sound.getVolume(), 1.0F,
                SoundInstance.createUnseededRandom(), false, 0, SoundInstance.Attenuation.LINEAR,
                sound.getX(), sound.getY(), sound.getZ(), false));
    }

    private static boolean isZombieIdle(Identifier id) {
        if (!id.getNamespace().equals("minecraft")) return false;
        return switch (id.getPath()) {
            case "entity.zombie.ambient", "entity.zombie_villager.ambient",
                    "entity.husk.ambient", "entity.drowned.ambient",
                    "entity.drowned.ambient_water" -> true;
            default -> false;
        };
    }
}
