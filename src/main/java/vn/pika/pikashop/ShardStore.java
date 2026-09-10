package vn.pika.pikashop;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tu viet: so du Shard luu data.yml (muc shards.<uuid>), kem chong farm giet lap lai.
 * Chinh sach hoc y tuong DonutShop: cung cap killer+victim co cooldown (mac dinh 300s).
 */
public class ShardStore {

    private final PikaShop plugin;
    private final Map<UUID, Long> balances = new ConcurrentHashMap<>();
    // key = killerUuid + ">" + victimUuid -> lan cuoi duoc thuong (ms)
    private final Map<String, Long> lastReward = new ConcurrentHashMap<>();

    public ShardStore(PikaShop plugin) {
        this.plugin = plugin;
    }

    public void load() {
        balances.clear();
        File file = dataFile();
        if (!file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        if (y.getConfigurationSection("shards") == null) return;
        for (String key : y.getConfigurationSection("shards").getKeys(false)) {
            try {
                balances.put(UUID.fromString(key), y.getLong("shards." + key, 0));
            } catch (IllegalArgumentException ignored) { }
        }
    }

    public void save() {
        File file = dataFile();
        YamlConfiguration y = file.exists()
                ? YamlConfiguration.loadConfiguration(file) : new YamlConfiguration();
        for (Map.Entry<UUID, Long> e : balances.entrySet()) {
            y.set("shards." + e.getKey(), e.getValue());
        }
        try {
            y.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Không lưu được shard: " + e.getMessage());
        }
    }

    private File dataFile() {
        return new File(plugin.getDataFolder(), "data.yml");
    }

    public long balance(UUID uuid) {
        return balances.getOrDefault(uuid, 0L);
    }

    public void set(UUID uuid, long amount) {
        balances.put(uuid, Math.max(0, amount));
        save();
    }

    public void add(UUID uuid, long amount) {
        set(uuid, balance(uuid) + amount);
    }

    /** Tru, tra false neu khong du (khong tru). */
    public boolean take(UUID uuid, long amount) {
        long cur = balance(uuid);
        if (cur < amount) return false;
        set(uuid, cur - amount);
        return true;
    }

    /** true neu cap killer/victim qua cooldown (duoc thuong), false neu dang bi chan farm. */
    public boolean claimKillReward(UUID killer, UUID victim) {
        if (killer.equals(victim)) return false;
        long cooldownMs = plugin.getConfig().getLong("shards.kill-cooldown-seconds", 300) * 1000L;
        String key = killer + ">" + victim;
        long now = System.currentTimeMillis();
        Long last = lastReward.get(key);
        if (last != null && now - last < cooldownMs) return false;
        lastReward.put(key, now);
        return true;
    }

    /** BXH giam dan, toi da limit muc. */
    public java.util.List<Map.Entry<UUID, Long>> top(int limit) {
        TreeMap<Long, java.util.List<UUID>> byBalance = new TreeMap<>(
                java.util.Collections.reverseOrder());
        for (Map.Entry<UUID, Long> e : balances.entrySet()) {
            byBalance.computeIfAbsent(e.getValue(), k -> new java.util.ArrayList<>()).add(e.getKey());
        }
        java.util.List<Map.Entry<UUID, Long>> out = new java.util.ArrayList<>();
        for (Map.Entry<Long, java.util.List<UUID>> e : byBalance.entrySet()) {
            for (UUID u : e.getValue()) {
                out.add(new java.util.AbstractMap.SimpleEntry<>(u, e.getKey()));
                if (out.size() >= limit) return out;
            }
        }
        return out;
    }
}
