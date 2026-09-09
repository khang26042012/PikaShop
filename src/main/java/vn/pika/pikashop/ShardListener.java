package vn.pika.pikashop;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Tu viet: giet player thuong Shard (chong farm) + teleport AFK dem nguoc. */
public class ShardListener implements Listener {

    private final PikaShop plugin;
    private final Set<UUID> teleporting = new HashSet<>();

    public ShardListener(PikaShop plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onKill(PlayerDeathEvent e) {
        if (!plugin.getConfig().getBoolean("shards.kill-enabled", true)) return;
        Player victim = e.getEntity();
        Player killer = victim.getKiller();
        if (killer == null) return;
        long amount = plugin.getConfig().getLong("shards.kill-base", 15);
        if (killer.hasPermission("pikashop.shard.extra_twenty")) {
            amount += plugin.getConfig().getLong("shards.kill-extra-twenty", 20);
        } else if (killer.hasPermission("pikashop.shard.extra_ten")) {
            amount += plugin.getConfig().getLong("shards.kill-extra-ten", 10);
        }
        if (!plugin.shards().claimKillReward(killer.getUniqueId(), victim.getUniqueId())) {
            killer.sendMessage(plugin.msg("messages.shard-kill-blocked"));
            return;
        }
        plugin.shards().add(killer.getUniqueId(), amount);
        Map<String, String> ph = new HashMap<>();
        ph.put("amount", String.valueOf(amount));
        ph.put("victim", victim.getName());
        killer.sendMessage(plugin.msg("messages.shard-kill", ph));
        killer.playSound(killer.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
    }

    public void startAfkTeleport(Player p) {
        Location loc = plugin.afk().location();
        if (loc == null) {
            p.sendMessage(plugin.msg("messages.shard-afk-not-set"));
            return;
        }
        if (!teleporting.add(p.getUniqueId())) {
            p.sendMessage(plugin.msg("messages.shard-afk-busy"));
            return;
        }
        int seconds = plugin.getConfig().getInt("shards.afk-countdown-seconds", 5);
        Map<String, String> ph = new HashMap<>();
        ph.put("time", String.valueOf(seconds));
        p.sendMessage(plugin.msg("messages.shard-afk-countdown", ph));
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            teleporting.remove(p.getUniqueId());
            if (!p.isOnline()) return;
            Location to = plugin.afk().location();
            if (to == null) return;
            p.teleport(to);
            p.sendMessage(plugin.msg("messages.shard-afk-done"));
            p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
        }, seconds * 20L);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        if (!teleporting.contains(id)) return;
        Location from = e.getFrom();
        Location to = e.getTo();
        if (to == null) return;
        if (from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY()
                || from.getBlockZ() != to.getBlockZ()) {
            teleporting.remove(id);
            e.getPlayer().sendMessage(plugin.msg("messages.shard-afk-cancelled"));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        teleporting.remove(e.getPlayer().getUniqueId());
    }
}
