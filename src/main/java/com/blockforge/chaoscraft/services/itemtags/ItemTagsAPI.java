package com.blockforge.chaoscraft.services.itemtags;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Static API for reading / writing custom string tags on ItemStacks
 * via the PersistentDataContainer (semicolon-delimited under one key).
 */
public final class ItemTagsAPI {

    private ItemTagsAPI() {}

    private static NamespacedKey key() {
        return Objects.requireNonNull(ChaosCraftPlugin.getInstance(), "ChaosCraftPlugin not loaded")
                .getItemTagsKey();
    }

    // ---- Read ----

    public static Set<String> getTags(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return new LinkedHashSet<>();

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return new LinkedHashSet<>();

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String raw = pdc.get(key(), PersistentDataType.STRING);
        if (raw == null || raw.isEmpty()) return new LinkedHashSet<>();

        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String part : raw.split(";")) {
            String s = part.trim();
            if (!s.isEmpty()) set.add(s);
        }
        return set;
    }

    // ---- Write ----

    public static void setTags(ItemStack item, Collection<String> tags) {
        if (item == null || item.getType() == Material.AIR) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        String joined = tags.stream()
                .map(s -> s.replace(";", "").trim())
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(";"));

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (joined.isEmpty()) {
            pdc.remove(key());
        } else {
            pdc.set(key(), PersistentDataType.STRING, joined);
        }
        item.setItemMeta(meta);
    }

    public static boolean addTag(ItemStack item, String tag) {
        if (tag == null) return false;
        String clean = tag.replace(";", "").trim();
        if (clean.isEmpty()) return false;

        Set<String> set = getTags(item);
        boolean changed = set.add(clean);
        if (changed) setTags(item, set);
        return changed;
    }

    public static boolean removeTag(ItemStack item, String tag) {
        if (tag == null) return false;
        String clean = tag.replace(";", "").trim();
        if (clean.isEmpty()) return false;

        Set<String> set = getTags(item);
        boolean changed = set.remove(clean);
        if (changed) setTags(item, set);
        return changed;
    }

    // ---- Query ----

    public static boolean hasTag(ItemStack item, String tag) {
        if (tag == null) return false;
        String clean = tag.replace(";", "").trim();
        return !clean.isEmpty() && getTags(item).contains(clean);
    }

    public static boolean hasAny(ItemStack item, String... tags) {
        if (tags == null || tags.length == 0) return false;
        for (String t : tags) {
            if (hasTag(item, t)) return true;
        }
        return false;
    }

    public static boolean hasAll(ItemStack item, String... tags) {
        if (tags == null || tags.length == 0) return true;
        for (String t : tags) {
            if (!hasTag(item, t)) return false;
        }
        return true;
    }
}
