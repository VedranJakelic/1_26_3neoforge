package net.beamex.shamaschizm.homunculus;

import java.text.Normalizer;
import java.util.Locale;

public enum HomunculusCommand {
    FOLLOW(0), STAY(0), SORT(0), FIGHT(1), GUARD(1), HARVEST(1), DESTROY(2);
    public final int requiredStage;
    HomunculusCommand(int stage) { requiredStage = stage; }
    public static HomunculusCommand parse(String text) {
        String word = Normalizer.normalize(text.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        return switch (word) {
            case "follow", "come", "sequere", "veni" -> FOLLOW;
            case "stay", "sit", "mane", "sede" -> STAY;
            case "sort", "ordina", "dispone" -> SORT;
            case "fight", "attack", "kill", "pugna", "oppugna", "interfice", "occide" -> FIGHT;
            case "guard", "custodi", "vigila" -> GUARD;
            case "harvest", "mete", "collige" -> HARVEST;
            case "go", "destroy", "i", "vade", "destrue", "dele" -> DESTROY;
            default -> null;
        };
    }
}
