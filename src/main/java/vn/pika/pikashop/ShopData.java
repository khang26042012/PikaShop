package vn.pika.pikashop;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tu viet: nap menu shop chinh + category tu yml.
 * Hoc y tuong (menu chinh -> category -> mua), cau truc tu thiet ke.
 */
public class ShopData {

    public static class Category {
        public String id;
        public String title = "SHOP";
        public int size = 27;
        public String displayName = "?";
        public Material icon = Material.CHEST;
        public int slot = 0;
        public String file = "";
        public final Map<String, ShopItem> items = new LinkedHashMap<>();
    }

    public static class ShopItem {
        public String key;
        public String displayName = "?";
        public Material material = Material.STONE;
        public double price = 0;
        public int maxStack = 64;
        public int slot = -1;
    }

    public String mainTitle = "SHOP";
    public int mainSize = 27;
    public final Map<String, Category> categories = new LinkedHashMap<>();

    public void load(PikaShop plugin) {
        categories.clear();
        copy(plugin, "shop/main.yml");
        YamlConfiguration main = yaml(plugin, "shop/main.yml");
        mainTitle = main.getString("Title", "SHOP");
        mainSize = fixSize(main.getInt("Size", 27));
        ConfigurationSection cs = main.getConfigurationSection("categories");
        if (cs == null) {
            plugin.getLogger().warning("shop/main.yml thieu muc categories.");
            return;
        }
        for (String id : cs.getKeys(false)) {
            ConfigurationSection s = cs.getConfigurationSection(id);
            if (s == null) continue;
            Category c = new Category();
            c.id = id;
            c.displayName = s.getString("displayName", id);
            c.icon = match(s.getString("material", "CHEST"));
            c.slot = s.getInt("slot", 0);
            c.file = s.getString("file", id + ".yml");
            loadCategory(plugin, c);
            categories.put(id, c);
        }
        plugin.getLogger().info("Nap " + categories.size() + " category shop.");
    }

    private void loadCategory(PikaShop plugin, Category c) {
        String path = "shop/categories/" + c.file;
        copy(plugin, path);
        YamlConfiguration y = yaml(plugin, path);
        c.title = y.getString("Title", "SHOP - " + c.id.toUpperCase());
        c.size = fixSize(y.getInt("Size", 27));
        ConfigurationSection is = y.getConfigurationSection("items");
        if (is == null) return;
        for (String key : is.getKeys(false)) {
            if (key.equalsIgnoreCase("back-page")) continue;
            ConfigurationSection s = is.getConfigurationSection(key);
            if (s == null) continue;
            ShopItem it = new ShopItem();
            it.key = key;
            it.displayName = s.getString("displayName", key);
            it.material = match(s.getString("material", "STONE"));
            it.price = s.getDouble("price", 0);
            it.maxStack = Math.max(1, s.getInt("max-stack", it.material.getMaxStackSize()));
            it.slot = s.getInt("slot", -1);
            c.items.put(key, it);
        }
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

    private YamlConfiguration yaml(PikaShop plugin, String path) {
        return YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), path));
    }

    private Material match(String name) {
        Material m = Material.matchMaterial(name);
        return m != null ? m : Material.STONE;
    }

    private int fixSize(int size) {
        if (size < 9 || size > 54 || size % 9 != 0) return 27;
        return size;
    }
}
