package vn.pika.pikashop;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Tu viet: /pshard shop (mua lenh console) + leaderboard dau player. */
public class ShardGui implements Listener {

    private final PikaShop plugin;

    private static class Holder implements InventoryHolder {
        final String kind;
        Holder(String kind) { this.kind = kind; }
        @Override public Inventory getInventory() { return null; }
    }

    public ShardGui(PikaShop plugin) {
        this.plugin = plugin;
    }

    // ---------- shop ----------
    public void openShop(Player p) {
        int size = 27;
        String title = ChatColor.LIGHT_PURPLE + "Shard Shop";
        Inventory inv = Bukkit.createInventory(new Holder("shop"), size, title);
        List<ShardShop.ShopEntry> entries = plugin.shardShop().entries();
        for (int i = 0; i < entries.size() && i < size - 9; i++) {
            ShardShop.ShopEntry e = entries.get(i);
            ItemStack it = new ItemStack(e.material);
            ItemMeta meta = it.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(color(e.name));
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.WHITE + "Gia: " + ChatColor.LIGHT_PURPLE + e.price + " shard");
                lore.add(ChatColor.GRAY + "Nhan de mua");
                meta.setLore(lore);
                it.setItemMeta(meta);
            }
            inv.setItem(e.slot >= 0 ? e.slot : i, it);
        }
        p.openInventory(inv);
    }

    // ---------- leaderboard ----------
    public void openTop(Player p, int page) {
        int perPage = 45;
        List<Map.Entry<UUID, Long>> top = plugin.shards().top(perPage * (page + 1));
        int maxPage = Math.max(0, (plugin.shards().top(Integer.MAX_VALUE).size() - 1) / perPage);
        if (page > maxPage) page = maxPage;
        if (page < 0) page = 0;
        final int pg = page;
        Inventory inv = Bukkit.createInventory(new Holder("top:" + pg), 54,
                ChatColor.LIGHT_PURPLE + "Shard Top (" + (pg + 1) + "/" + (maxPage + 1) + ")");
        int from = pg * perPage;
        int slot = 0;
        for (int i = from; i < top.size() && slot < perPage; i++, slot++) {
            Map.Entry<UUID, Long> e = top.get(i);
            String name = Bukkit.getOfflinePlayer(e.getKey()).getName();
            if (name == null) name = e.getKey().toString().substring(0, 8);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(Bukkit.getOfflinePlayer(e.getKey()));
                meta.setDisplayName(ChatColor.LIGHT_PURPLE + "" + (from + slot + 1) + ". " + name);
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.WHITE + "Shard: " + ChatColor.LIGHT_PURPLE + e.getValue());
                meta.setLore(lore);
                head.setItemMeta(meta);
            }
            inv.setItem(slot, head);
        }
        if (pg > 0) inv.setItem(48, named(Material.ARROW, ChatColor.YELLOW + "Trang truoc"));
        if (pg < maxPage) inv.setItem(50, named(Material.ARROW, ChatColor.YELLOW + "Trang sau"));
        p.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        if (!(e.getView().getTopInventory().getHolder() instanceof Holder)) return;
        Holder h = (Holder) e.getView().getTopInventory().getHolder();
        e.setCancelled(true);
        Player p = (Player) e.getWhoClicked();
        if (h.kind.equals("shop")) {
            int slot = e.getRawSlot();
            plugin.shardShop().buy(p, slot);
        } else if (h.kind.startsWith("top:")) {
            int pg = Integer.parseInt(h.kind.substring(4));
            int slot = e.getRawSlot();
            if (slot == 48) openTop(p, pg - 1);
            else if (slot == 50) openTop(p, pg + 1);
        }
    }

    private ItemStack named(Material m, String name) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            it.setItemMeta(meta);
        }
        return it;
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
