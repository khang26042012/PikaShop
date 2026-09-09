package vn.pika.pikashop;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tu viet: shop shard doc tu shards/shop.yml.
 * Moi item: slot/material/name/price/commands (console, %player%).
 * Tru shard truoc, lenh loi -> refund.
 */
public class ShardShop {

    public static class ShopEntry {
        public String key;
        public int slot = -1;
        public Material material = Material.STONE;
        public String name = "?";
        public long price = 0;
        public final List<String> commands = new ArrayList<>();
    }

    private final PikaShop plugin;
    private final List<ShopEntry> entries = new ArrayList<>();

    public ShardShop(PikaShop plugin) {
        this.plugin = plugin;
    }

    public void load() {
        entries.clear();
        File f = new File(plugin.getDataFolder(), "shards/shop.yml");
        if (!f.exists()) {
            try {
                plugin.saveResource("shards/shop.yml", false);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Khong co resource shards/shop.yml.");
                return;
            }
        }
        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
        ConfigurationSection is = y.getConfigurationSection("items");
        if (is == null) return;
        for (String key : is.getKeys(false)) {
            ConfigurationSection s = is.getConfigurationSection(key);
            if (s == null) continue;
            ShopEntry e = new ShopEntry();
            e.key = key;
            e.slot = s.getInt("slot", -1);
            Material m = Material.matchMaterial(s.getString("material", "STONE"));
            e.material = m != null ? m : Material.STONE;
            e.name = s.getString("name", key);
            e.price = s.getLong("price", 0);
            e.commands.addAll(s.getStringList("commands"));
            if (e.price > 0 && !e.commands.isEmpty()) entries.add(e);
        }
        plugin.getLogger().info("Nap " + entries.size() + " mon shard shop.");
    }

    public List<ShopEntry> entries() {
        return entries;
    }

    public void buy(Player p, int slot) {
        ShopEntry found = null;
        for (ShopEntry e : entries) {
            if (e.slot == slot) {
                found = e;
                break;
            }
        }
        if (found == null) return;
        if (!plugin.shards().take(p.getUniqueId(), found.price)) {
            Map<String, String> ph = new HashMap<>();
            ph.put("price", String.valueOf(found.price));
            p.sendMessage(plugin.msg("messages.shard-not-enough", ph));
            return;
        }
        // thua cho truoc khi chay lenh? khong - chi chay lenh console
        boolean ok = true;
        for (String cmd : found.commands) {
            String real = cmd.replace("%player%", p.getName());
            if (real.startsWith("[player] ")) {
                real = real.substring(9);
                ok = p.performCommand(real) && ok;
            } else {
                ok = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), real) && ok;
            }
        }
        if (!ok) {
            plugin.shards().add(p.getUniqueId(), found.price);
            p.sendMessage(plugin.msg("messages.shard-refunded"));
            return;
        }
        Map<String, String> ph = new HashMap<>();
        ph.put("product", found.name.replace("&", ""));
        ph.put("price", String.valueOf(found.price));
        p.sendMessage(plugin.msg("messages.shard-bought", ph));
        p.closeInventory();
    }
}
