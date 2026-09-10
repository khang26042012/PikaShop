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
                meta.setLore(Arrays.asList(ChatColor.GRAY + "Nhấn để xem gian hàng",
                        ChatColor.DARK_GRAY + c.id));
                icon.setItemMeta(meta);
            }
            if (c.slot >= 0 && c.slot < inv.getSize()) inv.setItem(c.slot, icon);
        }
        p.openInventory(inv);
    }

    // ---------- menu category (co phan trang, hien gia buy+sell) ----------
    private static final int PAGE_SIZE = 45; // 5 hang do, hang cuoi lam thanh dieu huong
    private static final int NAV_PREV = 45;
    private static final int NAV_NEXT = 53;
    private static final int NAV_BACK = 49;

    public void openCategory(Player p, String categoryId) {
        openCategory(p, categoryId, 0);
    }

    public void openCategory(Player p, String categoryId, int page) {
        ShopData.Category c = plugin.shops().categories.get(categoryId);
        if (c == null) {
            p.sendMessage(plugin.msg("messages.category-not-found"));
            return;
        }
        List<ShopData.ShopItem> all = new ArrayList<>(c.items.values());
        int pages = Math.max(1, (all.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        page = Math.max(0, Math.min(page, pages - 1));
        int size = 54;
        String title = c.title + (pages > 1 ? " (" + (page + 1) + "/" + pages + ")" : "");
        Inventory inv = Bukkit.createInventory(new Holder("cat:" + categoryId + ":" + page), size, color(title));
        int from = page * PAGE_SIZE;
        int to = Math.min(all.size(), from + PAGE_SIZE);
        for (int i = from; i < to; i++) {
            ShopData.ShopItem it = all.get(i);
            ItemStack icon = buildShopIcon(it);
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(color(it.displayName));
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.WHITE + "Giá mua: " + ChatColor.GREEN + "$" + money(it.price));
                if (it.sellPrice > 0) {
                    lore.add(ChatColor.WHITE + "Giá bán: " + ChatColor.GOLD + "$" + money(it.sellPrice));
                }
                lore.add(ChatColor.GRAY + "Nhấn để chọn số lượng");
                lore.add(ChatColor.DARK_GRAY + c.id + "/" + it.key);
                meta.setLore(lore);
                icon.setItemMeta(meta);
            }
            inv.setItem(i - from, icon);
        }
        if (page > 0) inv.setItem(NAV_PREV, named(Material.ARROW, ChatColor.YELLOW + "TRANG TRƯỚC"));
        if (page < pages - 1) inv.setItem(NAV_NEXT, named(Material.ARROW, ChatColor.YELLOW + "TRANG SAU"));
        inv.setItem(NAV_BACK, named(Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "QUAY LẠI"));
        p.openInventory(inv);
    }

    /** Dung NBT that cho item hien thi + mua ve: potion/enchant/spawner (tu viet). */
    private ItemStack buildShopIcon(ShopData.ShopItem it) {
        ItemStack icon = new ItemStack(it.material);
        try {
            if (it.potion != null && icon.getItemMeta() instanceof org.bukkit.inventory.meta.PotionMeta) {
                org.bukkit.inventory.meta.PotionMeta pm = (org.bukkit.inventory.meta.PotionMeta) icon.getItemMeta();
                org.bukkit.potion.PotionType pt = null;
                try { pt = org.bukkit.potion.PotionType.valueOf(it.potion); } catch (IllegalArgumentException ignored) {}
                if (pt != null) pm.setBasePotionType(pt);
                icon.setItemMeta(pm);
            } else if (it.enchant != null && it.enchant.contains(":")) {
                String[] parts = it.enchant.split(":", 2);
                org.bukkit.enchantments.Enchantment en = org.bukkit.enchantments.Enchantment.getByKey(
                        org.bukkit.NamespacedKey.minecraft(parts[0].toLowerCase(java.util.Locale.ROOT)));
                int lvl = 1;
                try { lvl = Integer.parseInt(parts[1]); } catch (NumberFormatException ignored) {}
                if (en != null) icon.addUnsafeEnchantment(en, Math.max(1, lvl));
            } else if (it.spawner != null && icon.getItemMeta() instanceof org.bukkit.inventory.meta.BlockStateMeta) {
                org.bukkit.inventory.meta.BlockStateMeta bm = (org.bukkit.inventory.meta.BlockStateMeta) icon.getItemMeta();
                if (bm.getBlockState() instanceof org.bukkit.block.CreatureSpawner) {
                    org.bukkit.block.CreatureSpawner cs = (org.bukkit.block.CreatureSpawner) bm.getBlockState();
                    try {
                        org.bukkit.entity.EntityType et = org.bukkit.entity.EntityType.valueOf(it.spawner);
                        cs.setSpawnedType(et);
                        bm.setBlockState(cs);
                        icon.setItemMeta(bm);
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        } catch (Exception ignored) {
            // giu icon vanilla neu version khong ho tro
        }
        return icon;
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
            lore.add(ChatColor.WHITE + "Số lượng: " + ChatColor.YELLOW + pd.amount);
            lore.add(ChatColor.WHITE + "Tổng: " + ChatColor.GREEN + "$" + money(it.price * pd.amount));
            meta.setLore(lore);
            info.setItemMeta(meta);
        }
        info.setAmount(Math.max(1, Math.min(pd.amount, 64)));
        inv.setItem(INFO_SLOT, info);
        inv.setItem(CONFIRM_SLOT, named(Material.LIME_STAINED_GLASS_PANE, ChatColor.GREEN + "XÁC NHẬN"));
        inv.setItem(CANCEL_SLOT, named(Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "HỦY"));
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
            String rest = h.kind.substring(4);
            String catId = rest;
            int page = 0;
            int ci = rest.lastIndexOf(':');
            if (ci >= 0) {
                try { page = Integer.parseInt(rest.substring(ci + 1)); catId = rest.substring(0, ci); }
                catch (NumberFormatException ignored) {}
            }
            if (slot == NAV_BACK) {
                openMain(p);
                return;
            }
            if (slot == NAV_PREV) {
                openCategory(p, catId, page - 1);
                return;
            }
            if (slot == NAV_NEXT) {
                openCategory(p, catId, page + 1);
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
        net.milkbowl.vault.economy.EconomyResponse wd = plugin.vault().get().withdrawPlayer(p, total);
        if (wd == null || !wd.transactionSuccess()) {
            p.sendMessage(plugin.msg("messages.not-enough-balance"));
            return;
        }
        ItemStack give = buildShopIcon(it);
        give.setAmount(amount);
        Map<Integer, ItemStack> overflow = p.getInventory().addItem(give);
        if (!overflow.isEmpty()) {
            // hiem khi xay ra (ai do nhan do giua chung): go phan da them + hoan tien
            int added = amount;
            for (ItemStack o : overflow.values()) added -= o.getAmount();
            if (added > 0) p.getInventory().removeItem(new ItemStack(it.material, added));
            plugin.vault().get().depositPlayer(p, total);
            p.sendMessage(plugin.msg("messages.not-enough-space"));
            return;
        }
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
