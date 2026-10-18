package net.beamex.shamaschizm.world.dungeon;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/** Editable room catalogue and pure weighting logic. No Minecraft registration required. */
public record DungeonRoomConfig(double sameTagMultiplier, Map<String, Tag> tags,
                                List<Entry> spawnRooms, List<Entry> rooms) {
    public record Entry(String template, String tag, double weight) {}
    public record HeightRule(int minY, int maxY, double multiplier) {}
    public record Tag(List<HeightRule> rules) {
        public double at(int y) {
            for (HeightRule rule : rules) if (y >= rule.minY && y <= rule.maxY) return rule.multiplier;
            return 1.0;
        }
    }

    /** y is the candidate room's lowest world block, not the player's or parent's height. */
    public double weight(Entry room, int y, String parentTag) {
        double adjacency = room.tag.equals(parentTag) ? sameTagMultiplier : 1.0;
        return room.weight * tags.get(room.tag).at(y) * adjacency;
    }

    /** Returns -1 when every weight is zero. Zero-weight entries are never selected. */
    public static int chooseIndex(List<Double> weights, Random random) {
        double total = 0;
        for (double weight : weights) {
            if (!Double.isFinite(weight) || weight < 0) throw new IllegalArgumentException("Invalid effective weight");
            total += weight;
        }
        if (total == 0) return -1;
        if (!Double.isFinite(total)) throw new IllegalArgumentException("Weight total overflow");
        double roll = random.nextDouble() * total;
        int lastPositive = -1;
        for (int i = 0; i < weights.size(); i++) {
            double weight = weights.get(i);
            if (weight <= 0) continue;
            lastPositive = i;
            if (roll < weight) return i;
            roll -= weight;
        }
        return lastPositive; // floating-point rounding only; never fall back to an excluded room
    }

    public static DungeonRoomConfig read(Reader reader) {
        JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
        keys(root, "same_tag_multiplier", "tags", "spawn_rooms", "rooms");
        double adjacency = number(root, "same_tag_multiplier", 3);
        Map<String, Tag> tags = new LinkedHashMap<>();
        for (var entry : root.getAsJsonObject("tags").entrySet()) {
            String name = entry.getKey();
            if (!name.matches("[a-z0-9_./-]+")) throw new IllegalArgumentException("Invalid tag name: " + name);
            JsonObject value = entry.getValue().getAsJsonObject();
            keys(value, "y_modifiers");
            List<HeightRule> rules = new ArrayList<>();
            JsonArray ranges = value.has("y_modifiers") ? value.getAsJsonArray("y_modifiers") : new JsonArray();
            for (JsonElement element : ranges) {
                JsonObject rule = element.getAsJsonObject();
                keys(rule, "min_y", "max_y", "multiplier");
                int min = integer(rule, "min_y", Integer.MIN_VALUE);
                int max = integer(rule, "max_y", Integer.MAX_VALUE);
                if (min > max) throw new IllegalArgumentException("Inverted Y interval in tag " + name);
                rules.add(new HeightRule(min, max, number(rule, "multiplier", 1)));
            }
            rules.sort(Comparator.comparingInt(HeightRule::minY));
            for (int i = 1; i < rules.size(); i++) {
                if (rules.get(i).minY <= rules.get(i-1).maxY)
                    throw new IllegalArgumentException("Overlapping Y intervals in tag " + name);
            }
            tags.put(name, new Tag(List.copyOf(rules)));
        }
        if (tags.isEmpty()) throw new IllegalArgumentException("At least one tag is required");
        List<Entry> starts = entries(root.getAsJsonArray("spawn_rooms"), tags, "spawn_rooms");
        List<Entry> rooms = entries(root.getAsJsonArray("rooms"), tags, "rooms");
        return new DungeonRoomConfig(adjacency, Map.copyOf(tags), starts, rooms);
    }

    private static List<Entry> entries(JsonArray array, Map<String, Tag> tags, String label) {
        if (array == null || array.size() == 0 || array.size() > 256)
            throw new IllegalArgumentException(label + " requires 1..256 entries");
        List<Entry> result = new ArrayList<>();
        Set<String> names = new HashSet<>();
        for (JsonElement element : array) {
            JsonObject entry = element.getAsJsonObject();
            keys(entry, "template", "tag", "weight");
            String template = entry.get("template").getAsString();
            String tag = entry.get("tag").getAsString();
            if (!template.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"))
                throw new IllegalArgumentException("Use namespace:path for template " + template);
            if (!names.add(template)) throw new IllegalArgumentException("Duplicate template in " + label + ": " + template);
            if (!tags.containsKey(tag)) throw new IllegalArgumentException("Undefined tag: " + tag);
            result.add(new Entry(template, tag, number(entry, "weight", 1)));
        }
        return List.copyOf(result);
    }

    private static int integer(JsonObject object, String name, int fallback) {
        if (!object.has(name)) return fallback;
        if (!object.get(name).isJsonPrimitive() || !object.getAsJsonPrimitive(name).isNumber())
            throw new IllegalArgumentException(name + " must be an integer");
        return object.get(name).getAsBigDecimal().intValueExact();
    }
    private static double number(JsonObject object, String name, double fallback) {
        if (!object.has(name)) return fallback;
        if (!object.get(name).isJsonPrimitive() || !object.getAsJsonPrimitive(name).isNumber())
            throw new IllegalArgumentException(name + " must be a number");
        double value = object.get(name).getAsDouble();
        if (!Double.isFinite(value) || value < 0 || value > 1_000_000)
            throw new IllegalArgumentException(name + " must be finite and between 0 and 1000000");
        return value;
    }
    private static void keys(JsonObject object, String... allowed) {
        Set<String> valid = Set.of(allowed);
        for (String key : object.keySet()) if (!valid.contains(key))
            throw new IllegalArgumentException("Unknown configuration field: " + key);
    }
}
