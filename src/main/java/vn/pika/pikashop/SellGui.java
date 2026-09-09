package vn.pika.pikashop;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tu viet: GUI ban do.
 * /sell mo GUI 36 o trong (tha do vao) + hang nut duoi (BAN / HUY / XEM GIA).
 * Dong inventory = tu dong ban het do hop le, cong tien Vault, ghi lich su.
 * Do KHONG ban duoc duoc tra lai tui (neu day thi roi xuong dat).
 */
public class SellGui implements Listener {

    private static final int GUI_SIZE = 54;
    private static final int SELL_ROWS = 4; // 36 o dau de do ban
    private static final int BTN_SELL = 48;
    private static final int BTN_WORTH = 49;
    private static final int BTN_CANCEL = 50;

    private final PikaShop plugin;

    private static class Holder implements InventoryHolder {
        @Override public Inventory getInventory() { return null; }
    }

    public SellGui(PikaShop plugin) {
        this.plugin = plugin;
    }

    public void open(Player p) {
        Inventory inv = Bukkit.createInventory(new Holder(), GUI_SIZE, color("BAN DO"));
        inv.setItem(BTN_SELL, named(Material.LIME_STAINED_GLASS_PANE, ChatColor.GREEN + "BAN HET",
                ChatColor.GRAY + "Nhan de ban, hoac dong menu"));
        inv.setItem(BTN_WORTH, named(Material.BOOK, ChatColor.YELLOW + "XEM GIA",
                ChatColor.GRAY + "Xem tong gia do trong menu"));
        inv.setItem(BTN_CANCEL, named(Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "HUY",
                ChatColor.GRAY + "Tra lai toan bo do"));
        p.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        if (!(e.getView().getTopInventory().getHolder() instanceof Holder)) return;
        Player p = (Player) e.getWhoClicked();
        // click trong GUI tren
        if (e.getClickedInventory() != null && e.getClickedInventory().equals(e.getView().getTopInventory())) {
            int slot = e.getRawSlot();
            if (slot < SELL_ROWS * 9) return; // o chua do: cho thao tac tu do
            e.setCancelled(true);
            if (slot == BTN_SELL) {
                settle(p, e.getView().getTopInventory(), false);
            } else if (slot == BTN_WORTH) {
                preview(p, e.getView().getTopInventory());
            } else if (slot == BTN_CANCEL) {
                cancel(p, e.getView().getTopInventory());
            }
            return;
        }
        // shift-click tu tui vao GUI: chi cho neu do ban duoc
        if (e.getClick().isShiftClick() && e.getClickedInventory() != null
                && e.getClickedInventory().equals(e.getView().getBottomInventory())) {
            ItemStack cur = e.getCurrentItem();
            if (cur != null && !cur.getType().isAir() && plugin.sells().unitPrice(cur.getType()) < 0) {
                e.setCancelled(true);
                p.sendMessage(plugin.msg("messages.not-sellable"));
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        if (!(e.getView().getTopInventory().getHolder() instanceof Holder)) return;
        // keo vao hang nut thi cam
        for (int slot : e.getRawSlots()) {
            if (slot >= SELL_ROWS * 9) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player)) return;
        if (!(e.getView().getTopInventory().getHolder() instanceof Holder)) return;
        // dong menu = ban het (giong DonutShop settle on close)
        settle((Player) e.getPlayer(), e.getView().getTopInventory(), true);
    }

    /** Xem truoc tong gia (khong ban) - da nhan multiplier. */
    private void preview(Player p, Inventory inv) {
        Quote q = quote(p, inv);
        if (q.total <= 0) {
            p.sendMessage(plugin.msg("messages.sell-empty"));
            return;
        }
        Map<String, String> ph = new HashMap<>();
        ph.put("total", money(q.total));
        ph.put("count", String.valueOf(q.count));
        p.sendMessage(plugin.msg("messages.sell-preview", ph));
    }

    /** Huy: tra lai toan bo do, khong ban. */
    private void cancel(Player p, Inventory inv) {
        List<ItemStack> back = takeAll(inv);
        giveBack(p, back);
        p.closeInventory();
        p.sendMessage(plugin.msg("messages.sell-cancelled"));
    }

    /** Quyet toan: ban do hop le (nhan multiplier), tra lai do khong ban duoc. */
    private void settle(Player p, Inventory inv, boolean onClose) {
        Quote q = quote(p, inv);
        List<ItemStack> unsellable = new ArrayList<>();
        for (int i = 0; i < SELL_ROWS * 9; i++) {
            ItemStack it = inv.getItem(i);
            if (it == null || it.getType().isAir()) continue;
            inv.setItem(i, null);
            if (plugin.sells().unitPrice(it.getType()) < 0) {
                unsellable.add(it);
            }
        }
        giveBack(p, unsellable);
        if (q.total <= 0) {
            if (!onClose) p.sendMessage(plugin.msg("messages.sell-empty"));
            else if (!unsellable.isEmpty()) p.sendMessage(plugin.msg("messages.sell-unsellable-back"));
            if (!onClose) p.closeInventory();
            return;
        }
        if (!plugin.vault().ready()) {
            // khong cong duoc tien: tra lai het de chong mat do
            giveBack(p, q.items);
            p.sendMessage(plugin.msg("messages.no-economy"));
            if (!onClose) p.closeInventory();
            return;
        }
        plugin.vault().get().depositPlayer(p, q.total);
        // ghi lich su + spent RIENG theo tung category (de len level dung)
        Map<String, Double> spentByCat = new HashMap<>();
        for (ItemStack it : q.items) {
            SellData.Category c = plugin.sells().categoryOf(it.getType());
            double line = q.priced.get(it) != null ? q.priced.get(it) : 0.0;
            plugin.store().addSale(p.getUniqueId(), it.getType().name(), it.getAmount(), line);
            if (c != null) spentByCat.put(c.id, spentByCat.getOrDefault(c.id, 0.0) + line);
        }
        int newLevel = 0;
        String levelCat = null;
        for (Map.Entry<String, Double> e : spentByCat.entrySet()) {
            int before = plugin.multi().level(p.getUniqueId(), e.getKey());
            plugin.store().addSpent(p.getUniqueId(), e.getKey(), e.getValue());
            int after = plugin.multi().level(p.getUniqueId(), e.getKey());
            if (after > before && after > newLevel) {
                newLevel = after;
                levelCat = e.getKey();
            }
        }
        if (levelCat != null) {
            Map<String, String> lv = new HashMap<>();
            lv.put("category", levelCat);
            lv.put("level", String.valueOf(newLevel));
            p.sendMessage(plugin.msg("messages.level-up", lv));
        }
        Map<String, String> ph = new HashMap<>();
        ph.put("total", money(q.total));
        ph.put("count", String.valueOf(q.count));
        p.sendMessage(plugin.msg("messages.sold-total", ph));
        if (!unsellable.isEmpty()) p.sendMessage(plugin.msg("messages.sell-unsellable-back"));
        if (!onClose) p.closeInventory();
    }

    private static class Quote {
        double total = 0;
        int count = 0;
        final List<ItemStack> items = new ArrayList<>();
        final Map<ItemStack, Double> priced = new HashMap<>();
    }

    private Quote quote(Player p, Inventory inv) {
        Quote q = new Quote();
        for (int i = 0; i < SELL_ROWS * 9; i++) {
            ItemStack it = inv.getItem(i);
            if (it == null || it.getType().isAir()) continue;
            double unit = plugin.sells().unitPrice(it.getType());
            if (unit < 0) continue;
            SellData.Category c = plugin.sells().categoryOf(it.getType());
            double line = unit * it.getAmount() * plugin.multi().factor(p.getUniqueId(), c);
            q.total += line;
            q.count += it.getAmount();
            ItemStack key = it.clone();
            q.items.add(key);
            q.priced.put(key, line);
        }
        return q;
    }

    private List<ItemStack> takeAll(Inventory inv) {
        List<ItemStack> out = new ArrayList<>();
        for (int i = 0; i < SELL_ROWS * 9; i++) {
            ItemStack it = inv.getItem(i);
            if (it == null || it.getType().isAir()) continue;
            inv.setItem(i, null);
            out.add(it);
        }
        return out;
    }

    private void giveBack(Player p, List<ItemStack> items) {
        for (ItemStack it : items) {
            Map<Integer, ItemStack> left = p.getInventory().addItem(it);
            for (ItemStack l : left.values()) {
                p.getWorld().dropItemNaturally(p.getLocation(), l);
            }
        }
        if (!items.isEmpty() && items.stream().anyMatch(i -> plugin.sells().unitPrice(i.getType()) < 0)) {
            // da bao rieng o settle; giu im lang o cancel
        }
    }

    /** Mo lich su dang text (50 giao dich gan nhat). */
    public void showHistory(Player p) {
        UUID id = p.getUniqueId();
        List<SellStore.Entry> list = plugin.store().history(id);
        if (list.isEmpty()) {
            p.sendMessage(plugin.msg("messages.history-empty"));
            return;
        }
        p.sendMessage(ChatColor.YELLOW + "Lich su ban (moi nhat cuoi):");
        int from = Math.max(0, list.size() - 10);
        for (int i = from; i < list.size(); i++) {
            SellStore.Entry e = list.get(i);
            p.sendMessage(ChatColor.GRAY + "- " + e.amount + "x " + e.item
                    + ChatColor.GREEN + " +$" + money(e.total));
        }
    }

    /** /worth: xem gia do dang cam hoac tong tui. */
    public void showWorth(Player p, boolean all) {
        if (all) {
            double total = 0;
            int count = 0;
            for (ItemStack it : p.getInventory().getStorageContents()) {
                if (it == null || it.getType().isAir()) continue;
                double unit = plugin.sells().unitPrice(it.getType());
                if (unit < 0) continue;
                total += unit * it.getAmount();
                count += it.getAmount();
            }
            Map<String, String> ph = new HashMap<>();
            ph.put("total", money(total));
            ph.put("count", String.valueOf(count));
            p.sendMessage(plugin.msg("messages.worth-all", ph));
            return;
        }
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand == null || hand.getType().isAir()) {
            p.sendMessage(plugin.msg("messages.worth-empty-hand"));
            return;
        }
        double unit = plugin.sells().unitPrice(hand.getType());
        if (unit < 0) {
            p.sendMessage(plugin.msg("messages.not-sellable"));
            return;
        }
        Map<String, String> ph = new HashMap<>();
        ph.put("item", hand.getType().name());
        ph.put("unit", money(unit));
        ph.put("total", money(unit * hand.getAmount()));
        p.sendMessage(plugin.msg("messages.worth-hand", ph));
    }

    private ItemStack named(Material m, String name, String lore) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            List<String> l = new ArrayList<>();
            l.add(lore);
            meta.setLore(l);
            it.setItemMeta(meta);
        }
        return it;
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    private String money(double v) {
        if (v == Math.floor(v)) return String.valueOf((long) v);
        return String.format("%.2f", v);
    }
}
