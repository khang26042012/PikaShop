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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Tu viet: GUI /sellmulti - 1 icon moi category sell.
 * Xanh (COMPLETE) khi max level, vang (WORKING) khi dang cay.
 * Lore: thanh progress + he so + spent hien tai / moc ke tiep.
 */
public class MultiGui implements Listener {

    private final PikaShop plugin;

    private static class Holder implements InventoryHolder {
        @Override public Inventory getInventory() { return null; }
    }

    public MultiGui(PikaShop plugin) {
        this.plugin = plugin;
    }

    public void open(Player p) {
        UUID id = p.getUniqueId();
        int size = plugin.getConfig().getInt("Multiplier-Menu.Size", 27);
        if (size < 9 || size > 54 || size % 9 != 0) size = 27;
        String title = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("Multiplier-Menu.Title", "Sell Categories"));
        Inventory inv = Bukkit.createInventory(new Holder(), size, title);
        for (SellData.Category c : plugin.sells().categories.values()) {
            int lv = plugin.multi().level(id, c.id);
            double factor = plugin.multi().factor(id, c);
            double spent = plugin.store().getSpent(id, c.id);
            double next = plugin.multi().nextMark(id, c.id);
            boolean max = next < 0;
            Material icon = c.icon != null ? c.icon : firstMaterial(c);
            ItemStack it = new ItemStack(icon);
            ItemMeta meta = it.getItemMeta();
            if (meta != null) {
                meta.setDisplayName((max ? ChatColor.GREEN : ChatColor.YELLOW)
                        + c.progressTitle + " (Lv " + lv + ")");
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + plugin.multi().bar(id, c.id)
                        + " " + plugin.multi().percent(id, c.id) + "%");
                lore.add(ChatColor.WHITE + "He so: " + ChatColor.GREEN + String.format("%.2fx", factor));
                if (max) {
                    lore.add(ChatColor.GREEN + "Da max level!");
                } else {
                    lore.add(ChatColor.GRAY + "$" + money(spent) + " / $" + money(next));
                }
                meta.setLore(lore);
                it.setItemMeta(meta);
            }
            int slot = c.slot;
            if (slot >= 0 && slot < inv.getSize()) inv.setItem(slot, it);
        }
        p.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        if (!(e.getView().getTopInventory().getHolder() instanceof Holder)) return;
        e.setCancelled(true);
    }

    private Material firstMaterial(SellData.Category c) {
        for (Material m : c.prices.keySet()) return m;
        return Material.BOOK;
    }

    private String money(double v) {
        if (v == Math.floor(v)) return String.valueOf((long) v);
        return String.format("%.2f", v);
    }
}
