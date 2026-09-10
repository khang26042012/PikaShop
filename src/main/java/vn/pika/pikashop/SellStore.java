package vn.pika.pikashop;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tu viet: luu lich su ban + tong spent theo category vao data.yml.
 * R9 dung spent de tinh multiplier level.
 */
public class SellStore {

    public static class Entry {
        public long time;
        public String item;
        public int amount;
        public double total;
    }

    private final PikaShop plugin;
    private File file;
    private YamlConfiguration data;

    public SellStore(PikaShop plugin) {
        this.plugin = plugin;
    }

    public void load() {
        file = new File(plugin.getDataFolder(), "data.yml");
        if (!file.exists()) {
            data = new YamlConfiguration();
            save();
        } else {
            data = YamlConfiguration.loadConfiguration(file);
        }
    }

    public void save() {
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Không lưu được data.yml: " + e.getMessage());
        }
    }

    public void addSale(UUID uuid, String item, int amount, double total) {
        List<Map<?, ?>> list = data.getMapList("history." + uuid);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<?, ?> m : list) {
            Map<String, Object> e = new LinkedHashMap<>();
            for (Map.Entry<?, ?> en : m.entrySet()) e.put(String.valueOf(en.getKey()), en.getValue());
            out.add(e);
        }
        Map<String, Object> e = new LinkedHashMap<>();
        e.put("time", System.currentTimeMillis() / 1000);
        e.put("item", item);
        e.put("amount", amount);
        e.put("total", total);
        out.add(e);
        while (out.size() > 50) out.remove(0);
        data.set("history." + uuid, out);
        save();
    }

    public List<Entry> history(UUID uuid) {
        List<Entry> out = new ArrayList<>();
        for (Map<?, ?> m : data.getMapList("history." + uuid)) {
            Entry e = new Entry();
            e.time = toLong(m.get("time"));
            e.item = String.valueOf(m.get("item"));
            e.amount = (int) toLong(m.get("amount"));
            e.total = toDouble(m.get("total"));
            out.add(e);
        }
        return out;
    }

    public double getSpent(UUID uuid, String category) {
        return data.getDouble("spent." + uuid + "." + category, 0);
    }

    public void addSpent(UUID uuid, String category, double amount) {
        data.set("spent." + uuid + "." + category, getSpent(uuid, category) + amount);
        save();
    }

    private long toLong(Object o) {
        if (o instanceof Number) return ((Number) o).longValue();
        try { return Long.parseLong(String.valueOf(o)); } catch (Exception e) { return 0; }
    }

    private double toDouble(Object o) {
        if (o instanceof Number) return ((Number) o).doubleValue();
        try { return Double.parseDouble(String.valueOf(o)); } catch (Exception e) { return 0; }
    }
}
