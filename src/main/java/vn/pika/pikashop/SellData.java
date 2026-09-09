package vn.pika.pikashop;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tu viet: bang gia sell theo category (sell/multiplier/*.yml).
 * Moi category: base-multiplier + allowed-types {MATERIAL: price-per-unit}.
 * R8 dung gia co ban; R9 them level/level-prices tu config.yml.
 */
public class SellData {

    public static class Category {
        public String id;
        public double baseMultiplier = 1.0;
        public int slot = 0;
        public String progressTitle = "";
        public Material icon = null;
        public final Map<Material, Double> prices = new LinkedHashMap<>();
        public File file;
    }

    public final Map<String, Category> categories = new LinkedHashMap<>();

    public void load(PikaShop plugin) {
        categories.clear();
        copy(plugin, "sell/config.yml");
        for (String f : new String[]{"ores", "mob", "blocks", "crops", "fish", "food"}) {
            copy(plugin, "sell/multiplier/" + f + ".yml");
        }
        File dir = new File(plugin.getDataFolder(), "sell/multiplier");
        File[] files = dir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null || files.length == 0) {
            plugin.getLogger().warning("Khong co file gia sell nao trong sell/multiplier.");
            return;
        }
        int count = 0;
        for (File f : files) {
            try {
                YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
                Category c = new Category();
                c.id = f.getName().replaceFirst("\\.yml$", "");
                c.file = f;
                c.baseMultiplier = y.getDouble("base-multiplier", 1.0);
                c.slot = y.getInt("inventory-slot", 0);
                c.progressTitle = y.getString("progress-title", c.id);
                c.icon = Material.matchMaterial(y.getString("progress-icon-material", ""));
                ConfigurationSection at = y.getConfigurationSection("allowed-types");
                if (at != null) {
                    for (String key : at.getKeys(false)) {
                        Material m = Material.matchMaterial(key);
                        if (m == null) {
                            plugin.getLogger().warning("Bo vat lieu la trong " + f.getName() + ": " + key);
                            continue;
                        }
                        double price = at.getDouble(key + ".price-per-unit", 0);
                        if (price > 0) {
                            c.prices.put(m, price);
                            count++;
                        }
                    }
                }
                categories.put(c.id, c);
            } catch (Exception e) {
                plugin.getLogger().warning("Loi doc " + f.getName() + ": " + e.getMessage());
            }
        }
        plugin.getLogger().info("Nap " + categories.size() + " category sell, " + count + " muc gia.");
    }

    /** Gia don vi co ban cua vat lieu, -1 neu khong ban duoc. */
    public double unitPrice(Material m) {
        for (Category c : categories.values()) {
            Double p = c.prices.get(m);
            if (p != null) return p;
        }
        return -1;
    }

    /**
     * Admin dat gia sell: ghi vao category dang co vat lieu, neu chua co thi vao ores.yml.
     * Tra ve id category da ghi. Gia 0 = go khoi bang gia.
     */
    public String setPrice(Material m, double price) {
        Category target = categoryOf(m);
        if (target == null) {
            target = categories.get("ores");
            if (target == null && !categories.isEmpty()) {
                target = categories.values().iterator().next();
            }
            if (target == null) return "?";
        }
        YamlConfiguration y = YamlConfiguration.loadConfiguration(target.file);
        String base = "allowed-types." + m.name();
        if (price == 0) {
            y.set(base, null);
            target.prices.remove(m);
        } else {
            y.set(base + ".price-per-unit", price);
            target.prices.put(m, price);
        }
        try {
            y.save(target.file);
        } catch (java.io.IOException e) {
            // giu gia trong RAM du co loi ghi file
        }
        return target.id;
    }

    /** Category chua vat lieu nay, null neu khong co. */
    public Category categoryOf(Material m) {
        for (Category c : categories.values()) {
            if (c.prices.containsKey(m)) return c;
        }
        return null;
    }

    private void copy(PikaShop plugin, String path) {
        File f = new File(plugin.getDataFolder(), path);
        if (!f.exists()) {
            try {
                plugin.saveResource(path, false);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Khong co resource mac dinh: " + path);
            }
        }
    }
}
