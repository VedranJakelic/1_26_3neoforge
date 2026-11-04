package net.beamex.shamaschizm.banner.client;

/** Client render-state extension (never queries the active world/level). */
public interface TatteredBannerState {
    void shamaschizm$setTattered(boolean value);
    boolean shamaschizm$isTattered();
}
