package vn.pika.pikashop;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;

/** Tu viet: AFK zone - tra thuong dinh ky cho player dung trong ban kinh. */
public class ShardAfk implements Runnable {

    private final PikaShop plugin;
    private Location loc;
    private int taskId = -1;

    public ShardAfk(PikaShop plugin) {
        this.plugin = plugin;
    }

    public void load() {
        File f = new File(plugin.getDataFolder(), "afk.yml");
        loc = null;
        if (!f.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
        String world = y.getString("world", "");
        if (world.isEmpty() || Bukkit.getWorld(world) == null) return;
        loc = new Location(Bukkit.getWorld(world), y.getDouble("x"), y.getDouble("y"), y.getDouble("z"),
                (float) y.getDouble("yaw"), (float) y.getDouble("pitch"));
    }

    public void save(Location l) {
        File f = new File(plugin.getDataFolder(), "afk.yml");
        YamlConfiguration y = new YamlConfiguration();
        y.set("world", l.getWorld().getName());
        y.set("x", l.getX());
        y.set("y", l.getY());
        y.set("z", l.getZ());
        y.set("yaw", l.getYaw());
        y.set("pitch", l.getPitch());
        try {
            y.save(f);
        } catch (IOException e) {
            plugin.getLogger().warning("Không lưu được afk.yml: " + e.getMessage());
        }
        loc = l.clone();
    }

    public Location location() {
        return loc == null ? null : loc.clone();
    }

    public void start() {
        stop();
        long interval = Math.max(1, plugin.getConfig().getLong("shards.afk-interval-seconds", 60)) * 20L;
        taskId = Bukkit.getScheduler().runTaskTimer(plugin, this, interval, interval).getTaskId();
    }

    public void stop() {
        if (taskId >= 0) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    @Override
    public void run() {
        if (loc == null || loc.getWorld() == null) return;
        if (!plugin.getConfig().getBoolean("shards.afk-enabled", true)) return;
        double radius = plugin.getConfig().getDouble("shards.afk-radius", 10.0);
        long amount = plugin.getConfig().getLong("shards.afk-per-interval", 10);
        for (Player p : loc.getWorld().getPlayers()) {
            if (!p.isOnline() || p.isDead()) continue;
            if (p.getLocation().distanceSquared(loc) > radius * radius) continue;
            plugin.shards().add(p.getUniqueId(), amount);
            java.util.Map<String, String> ph = new java.util.HashMap<>();
            ph.put("amount", String.valueOf(amount));
            p.sendMessage(plugin.msg("messages.shard-afk-reward", ph));
            p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
        }
    }
}
