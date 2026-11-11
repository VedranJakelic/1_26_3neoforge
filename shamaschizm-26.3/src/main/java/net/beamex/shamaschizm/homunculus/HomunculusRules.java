package net.beamex.shamaschizm.homunculus;

/** Pure decisions shared by gameplay and the regression checks. */
final class HomunculusRules {
    static int foodRequired(int stage) { return stage == 0 ? 40 : stage == 1 ? 60 : Integer.MAX_VALUE; }
    static int maximumHealth(int stage) { return stage == 0 ? 10 : stage == 1 ? 20 : 50; }
    static boolean canRelocate(int sourceRank, long sourcePosition, int destinationRank, long destinationPosition) {
        return destinationRank > sourceRank || destinationRank == sourceRank && destinationPosition < sourcePosition;
    }
    private HomunculusRules() {}
}
