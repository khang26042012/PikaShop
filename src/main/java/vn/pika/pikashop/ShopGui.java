package vn.pika.pikashop;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tu viet: Shop mua 2 buoc.
 * Buoc 1: /shop -> menu chinh (click category).
 * Buoc 2: menu category -> click item mo man hinh so luong -> chon -64/-16/-1/+1/+16/+64 -> CONFIRM/CANCEL.
 * Khoa Bedrock/Geyser: chi click chuot trai so luong co dinh + nut XAC NHAN, khong keo so.
 */
public class ShopGui implements Listener {

    private static final int[] STEPS = {-64, -16, -1, 1, 16, 64};
    private static final int BACK_SLOT = 18;
    private static final int CONFIRM_SLOT = 23;
    private static final int CANCEL_SLOT = 21;
    private static final int INFO_SLOT = 13;
    private static final int AMOUNT_SIZE = 27;

    private final PikaShop plugin;
    private final Map<UUID, Pending> pending = new HashMap<>();

    private static class Pending {
        String categoryId;
        String itemKey;
        int amount = 1;
    }

    private static class Holder implements InventoryHolder {
        final String kind;
        Holder(String kind) { this.kind = kind; }
        @Override public Inventory getInventory() { return null; }
    }

    public ShopGui(PikaShop plugin) {
        this.plugin = plugin;
    }

    // ---------- menu chinh ----------
    public void openMain(Player p) {
        ShopData data = plugin.shops();
        Inventory inv = Bukkit.createInventory(new Holder("main"), data.mainSize, color(data.mainTitle));
        for (ShopData.Category c : data.categories.values()) {
            ItemStack icon = new ItemStack(c.icon);
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(color(c.displayName));
                meta.setLore(Arrays.asList(ChatColor.GRAY + "Nhan de xem gian hang",
                        ChatColor.DARK_GRAY + c.id));
                icon.setItemMeta(meta);
            }
            if (c.slot >= 0 && c.slot < inv.getSize()) inv.setItem(c.slot, icon);
        }
        p.openInventory(inv);
    }

    // ---------- menu category ----------
    public void openCategory(Player p, String categoryId) {
        ShopData.Category c = plugin.shops().categories.get(categoryId);
        if (c == null) {
            p.sendMessage(plugin.msg("messages.category-not-found"));
            return;
        }
        Inventory inv = Bukkit.createInventory(new Holder("cat:" + categoryId), c.size, color(c.title));
        for (ShopData.ShopItem it : c.items.values()) {
            ItemStack icon = new ItemStack(it.material);
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(color(it.displayName));
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.WHITE + "Gia mua: " + ChatColor.GREEN + "$" + money(it.price));
                lore.add(ChatColor.GRAY + "Nhan de chon so luong");
                lore.add(ChatColor.DARK_GRAY + c.id + "/" + it.key);
                meta.setLore(lore);
                icon.setItemMeta(meta);
            }
            if (it.slot >= 0 && it.slot < inv.getSize()) inv.setItem(it.slot, icon);
        }
        // nut quay lai
        ItemStack back = named(Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "QUAY LAI");
        if (BACK_SLOT < inv.getSize()) inv.setItem(BACK_SLOT, back);
        p.openInventory(inv);
    }

    // ---------- man hinh so luong ----------
    public void openAmount(Player p, String categoryId, String itemKey) {
        ShopData.Category c = plugin.shops().categories.get(categoryId);
        if (c == null) return;
        ShopData.ShopItem it = c.items.get(itemKey);
        if (it == null) return;
        Pending pd = pending.computeIfAbsent(p.getUniqueId(), k -> new Pending());
        pd.categoryId = categoryId;
        pd.itemKey = itemKey;
        pd.amount = Math.min(pd.amount <= 0 ? 1 : pd.amount, it.maxStack);
        renderAmount(p, c, it, pd);
    }

    private void renderAmount(Player p, ShopData.Category c, ShopData.ShopItem it, Pending pd) {
        Inventory inv = Bukkit.createInventory(new Holder("amount"), AMOUNT_SIZE,
                color("MUA " + strip(it.displayName)));
        int i = 9;
        for (int step : STEPS) {
            String name = (step < 0 ? ChatColor.RED + "" + step : ChatColor.GREEN + "+" + step);
            inv.setItem(i++, named(step < 0 ? Material.RED_STAINED_GLASS_PANE : Material.LIME_STAINED_GLASS_PANE, name));
        }
        // thong tin giua
        ItemStack info = new ItemStack(it.material);
        ItemMeta meta = info.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(it.displayName));
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.WHITE + "So luong: " + ChatColor.YELLOW + pd.amount);
            lore.add(ChatColor.WHITE + "Tong: " + ChatColor.GREEN + "$" + money(it.price * pd.amount));
            meta.setLore(lore);
            info.setItemMeta(meta);
        }
        info.setAmount(Math.max(1, Math.min(pd.amount, 64)));
        inv.setItem(INFO_SLOT, info);
        inv.setItem(CONFIRM_SLOT, named(Material.LIME_STAINED_GLASS_PANE, ChatColor.GREEN + "XAC NHAN"));
        inv.setItem(CANCEL_SLOT, named(Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "HUY"));
        p.openInventory(inv);
    }

    // ---------- xu ly click ----------
    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        if (!(e.getView().getTopInventory().getHolder() instanceof Holder)) return;
        Holder h = (Holder) e.getView().getTopInventory().getHolder();
        // chi xu ly click trong GUI tren, khong cho lay do ra
        if (e.getClickedInventory() == null || !e.getClickedInventory().equals(e.getView().getTopInventory())) {
            if (e.getClick().isShiftClick()) e.setCancelled(true);
            return;
        }
        e.setCancelled(true);
        Player p = (Player) e.getWhoClicked();
        int slot = e.getRawSlot();
        if (h.kind.equals("main")) {
            for (ShopData.Category c : plugin.shops().categories.values()) {
                if (c.slot == slot) {
                    openCategory(p, c.id);
                    return;
                }
            }
        } else if (h.kind.startsWith("cat:")) {
            String catId = h.kind.substring(4);
            if (slot == BACK_SLOT) {
                openMain(p);
                return;
            }
            ItemStack cur = e.getCurrentItem();
            String tag = tag(cur);
            if (tag != null && tag.contains("/")) {
                String[] parts = tag.split("/", 2);
                openAmount(p, parts[0], parts[1]);
            }
        } else if (h.kind.equals("amount")) {
            Pending pd = pending.get(p.getUniqueId());
            if (pd == null) {
                p.closeInventory();
                return;
            }
            ShopData.Category c = plugin.shops().categories.get(pd.categoryId);
            ShopData.ShopItem it = c != null ? c.items.get(pd.itemKey) : null;
            if (c == null || it == null) {
                p.closeInventory();
                return;
            }
            if (slot >= 9 && slot <= 14) {
                int step = STEPS[slot - 9];
                pd.amount = clamp(pd.amount + step, 1, it.maxStack);
                renderAmount(p, c, it, pd);
                return;
            }
            if (slot == CONFIRM_SLOT) {
                buy(p, c, it, pd.amount);
                return;
            }
            if (slot == CANCEL_SLOT) {
                openCategory(p, pd.categoryId);
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof Holder) {
            Holder h = (Holder) e.getView().getTopInventory().getHolder();
            if (!h.kind.equals("amount")) return;
            // giu pending de mo lai van nho so luong; khong can xoa
        }
    }

    // ---------- mua ----------
    private void buy(Player p, ShopData.Category c, ShopData.ShopItem it, int amount) {
        double total = it.price * amount;
        if (!plugin.vault().ready()) {
            p.sendMessage(plugin.msg("messages.no-economy"));
            p.closeInventory();
            return;
        }
        if (plugin.vault().get().getBalance(p) < total) {
            p.sendMessage(plugin.msg("messages.not-enough-balance"));
            return;
        }
        // mo phong them do truoc (khong cham inventory that): het cho thi bao, chua tru tien
        if (!fits(p, it.material, amount)) {
            p.sendMessage(plugin.msg("messages.not-enough-space"));
            return;
        }
        Map<Integer, ItemStack> overflow = p.getInventory().addItem(new ItemStack(it.material, amount));
        if (!overflow.isEmpty()) {
            // hiem khi xay ra (ai do nhan do giua chung): go phan da them, khong tru tien
            int added = amount;
            for (ItemStack o : overflow.values()) added -= o.getAmount();
            if (added > 0) p.getInventory().removeItem(new ItemStack(it.material, added));
            p.sendMessage(plugin.msg("messages.not-enough-space"));
            return;
        }
        plugin.vault().get().withdrawPlayer(p, total);
        Map<String, String> ph = new HashMap<>();
        ph.put("amount", String.valueOf(amount));
        ph.put("item", strip(it.displayName));
        ph.put("total", money(total));
        p.sendMessage(plugin.msg("messages.buying", ph));
        pending.remove(p.getUniqueId());
        p.closeInventory();
    }

    /** Mo phong: lieu tui co chua het amount vat lieu nay? (stack that + o trong). */
    private boolean fits(Player p, Material material, int amount) {
        int max = material.getMaxStackSize();
        int need = amount;
        for (ItemStack cur : p.getInventory().getStorageContents()) {
            if (need <= 0) return true;
            if (cur == null || cur.getType().isAir()) {
                need -= max;
            } else if (cur.getType() == material && cur.getAmount() < max
                    && !cur.hasItemMeta()) {
                need -= (max - cur.getAmount());
            }
        }
        return need <= 0;
    }

    // ---------- tien ich ----------
    private ItemStack named(Material m, String name) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            it.setItemMeta(meta);
        }
        return it;
    }

    private String tag(ItemStack it) {
        if (it == null || !it.hasItemMeta() || it.getItemMeta().getLore() == null) return null;
        for (String line : it.getItemMeta().getLore()) {
            String s = ChatColor.stripColor(line);
            if (s != null && s.contains("/")) return s.trim();
        }
        return null;
    }

    private String strip(String s) {
        String r = ChatColor.stripColor(color(s));
        return r != null ? r : s;
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    private String money(double v) {
        if (v == Math.floor(v)) return String.valueOf((long) v);
        return String.valueOf(v);
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
