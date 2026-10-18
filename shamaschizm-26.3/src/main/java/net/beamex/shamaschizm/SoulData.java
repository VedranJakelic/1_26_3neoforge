package net.beamex.shamaschizm;

import net.minecraft.server.level.ServerPlayer;

/** One server-owned balance. Missing means a new player; zero is a valid saved balance. */
public final class SoulData {
    private static final String SOULS = "shamaschizm_souls";
    private static final String INITIALIZED = "shamaschizm_souls_init";
    private SoulData() {}

    public static int getSouls(ServerPlayer player) {
        ensureInitialized(player);
        return Math.max(0, player.getPersistentData().getInt(SOULS).orElseThrow());
    }

    public static void setSouls(ServerPlayer player, int value) {
        player.getPersistentData().putInt(SOULS, Math.max(0, value));
        player.getPersistentData().putInt(INITIALIZED, 1);
    }

    public static void ensureInitialized(ServerPlayer player) {
        var saved = player.getPersistentData().getInt(SOULS);
        // Retain existing zero balances, even if the old initialization flag is absent.
        setSouls(player, saved.orElse(1));
    }

    public static boolean spend(ServerPlayer player, int amount) {
        if (amount <= 0) return false;
        int balance = getSouls(player);
        if (balance < amount) return false;
        setSouls(player, balance - amount);
        return true;
    }

    public static boolean gain(ServerPlayer player, int amount) {
        int balance = getSouls(player);
        if (amount <= 0 || balance > Integer.MAX_VALUE - amount) return false;
        setSouls(player, balance + amount);
        return true;
    }

    public static void copy(ServerPlayer original, ServerPlayer replacement) {
        setSouls(replacement, getSouls(original));
    }
}
