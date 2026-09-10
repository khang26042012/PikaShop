package vn.pika.pikashop;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

/**
 * PikaShop - shop rieng cua PikaMC, tu viet (clean-room).
 * R7: Shop mua 2 buoc that. R8: Sell tha-do + history + worth. R9: Multiplier. R13: Shards v2.
 */
public class PikaShop extends JavaPlugin implements CommandExecutor {

    private VaultHook vault;
    private ShopData shops;
    private ShopGui gui;
    private SellData sells;
    private SellStore store;
    private SellGui sellGui;
    private Multiplier multi;
    private MultiGui multiGui;
    private ShardStore shards;
    private ShardAfk afk;
    private ShardListener shardListener;
    private ShardGui shardGui;
    private ShardShop shardShop;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        vault = new VaultHook(this);
        shops = new ShopData();
        gui = new ShopGui(this);
        sells = new SellData();
        store = new SellStore(this);
        sellGui = new SellGui(this);
        multi = new Multiplier(this);
        multiGui = new MultiGui(this);
        shards = new ShardStore(this);
        afk = new ShardAfk(this);
        shardListener = new ShardListener(this);
        shardGui = new ShardGui(this);
        shardShop = new ShardShop(this);
        shops.load(this);
        sells.load(this);
        store.load();
        shards.load();
        afk.load();
        shardShop.load();
        afk.start();
        getServer().getPluginManager().registerEvents(gui, this);
        getServer().getPluginManager().registerEvents(sellGui, this);
        getServer().getPluginManager().registerEvents(multiGui, this);
        getServer().getPluginManager().registerEvents(shardListener, this);
        getServer().getPluginManager().registerEvents(shardGui, this);
        register("shard");
        register("sellprice");
        register("shop");
        register("sell");
        register("sellall");
        register("sellmulti");
        register("sellhistory");
        register("worth");
        register("pikashop");
        if (vault.setup()) {
            getLogger().info("PikaShop đã nối Vault Economy.");
        } else {
            getLogger().warning("Chưa thấy Vault/Economy - mua/bán sẽ báo lỗi.");
        }
        getLogger().info("PikaShop R13: shop + sell + multiplier + shards sẵn sàng.");
    }

    @Override
    public void onDisable() {
        if (afk != null) afk.stop();
        if (shards != null) shards.save();
        getLogger().info("PikaShop đã tắt (shard đã lưu).");
    }

    public VaultHook vault() { return vault; }
    public ShopData shops() { return shops; }
    public ShopGui gui() { return gui; }
    public SellData sells() { return sells; }
    public SellStore store() { return store; }
    public SellGui sellGui() { return sellGui; }
    public Multiplier multi() { return multi; }
    public MultiGui multiGui() { return multiGui; }
    public ShardStore shards() { return shards; }
    public ShardAfk afk() { return afk; }
    public ShardListener shardListener() { return shardListener; }
    public ShardGui shardGui() { return shardGui; }
    public ShardShop shardShop() { return shardShop; }

    private void register(String name) {
        if (getCommand(name) != null) {
            getCommand(name).setExecutor(this);
        } else {
            getLogger().warning("Không thấy lệnh trong plugin.yml: " + name);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String name = cmd.getName().toLowerCase();
        if (name.equals("pikashop")) {
            if (!sender.hasPermission("pikashop.admin")) {
                sender.sendMessage(msg("messages.no-permission"));
                return true;
            }
            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                reloadConfig();
                shops.load(this);
                sells.load(this);
                shardShop.load();
                sender.sendMessage(msg("messages.reloaded"));
                return true;
            }
            sender.sendMessage(ChatColor.LIGHT_PURPLE + "/pikashop reload");
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage("Chỉ người chơi mới dùng được lệnh này.");
            return true;
        }
        Player p = (Player) sender;
        if (!p.hasPermission("pikashop.use")) {
            p.sendMessage(msg("messages.no-permission"));
            return true;
        }
        if (name.equals("shop")) {
            gui.openMain(p);
            return true;
        }
        if (name.equals("sell")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("hand")) {
                sellGui.sellHand(p);
                return true;
            }
            if (args.length > 0 && args[0].equalsIgnoreCase("all")) {
                sellGui.sellAll(p);
                return true;
            }
            sellGui.open(p);
            return true;
        }
        if (name.equals("sellall")) {
            sellGui.sellAll(p);
            return true;
        }
        if (name.equals("sellhistory")) {
            sellGui.showHistory(p);
            return true;
        }
        if (name.equals("worth")) {
            boolean all = args.length > 0 && args[0].equalsIgnoreCase("all");
            sellGui.showWorth(p, all);
            return true;
        }
        if (name.equals("sellmulti")) {
            multiGui.open(p);
            return true;
        }
        if (name.equals("shard")) {
            return shardCommand(p, args);
        }
        if (name.equals("sellprice")) {
            if (!p.hasPermission("pikashop.admin")) {
                p.sendMessage(msg("messages.no-permission"));
                return true;
            }
            if (args.length != 1) {
                p.sendMessage(msg("messages.sellprice-usage"));
                return true;
            }
            org.bukkit.inventory.ItemStack hand = p.getInventory().getItemInMainHand();
            if (hand == null || hand.getType().isAir()) {
                p.sendMessage(msg("messages.worth-empty-hand"));
                return true;
            }
            double price;
            try {
                price = Double.parseDouble(args[0]);
            } catch (NumberFormatException e) {
                p.sendMessage(msg("messages.sellprice-usage"));
                return true;
            }
            if (price < 0) {
                p.sendMessage(msg("messages.sellprice-usage"));
                return true;
            }
            String catId = sells.setPrice(this, hand.getType(), price);
            java.util.Map<String, String> ph = new java.util.HashMap<>();
            ph.put("item", hand.getType().name());
            ph.put("price", String.valueOf(price));
            ph.put("category", catId);
            p.sendMessage(msg(price == 0 ? "messages.sellprice-removed" : "messages.sellprice-set", ph));
            return true;
        }
        p.sendMessage(msg("messages.developing"));
        return true;
    }

    /** /shard <balance|pay|shop|top|afk|set|give|take|see|setafk>. */
    public boolean shardCommand(Player p, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("balance")) {
            java.util.Map<String, String> ph = new java.util.HashMap<>();
            ph.put("balance", String.valueOf(shards.balance(p.getUniqueId())));
            p.sendMessage(msg("messages.shard-balance", ph));
            return true;
        }
        String sub = args[0].toLowerCase();
        if (sub.equals("shop")) {
            shardGui.openShop(p);
            return true;
        }
        if (sub.equals("top") || sub.equals("leaderboard")) {
            shardGui.openTop(p, 0);
            return true;
        }
        if (sub.equals("afk")) {
            shardListener.startAfkTeleport(p);
            return true;
        }
        if (sub.equals("pay") && args.length == 3) {
            Player t = getServer().getPlayerExact(args[1]);
            if (t == null) {
                java.util.Map<String, String> ph = new java.util.HashMap<>();
                ph.put("target", args[1]);
                p.sendMessage(msg("messages.player-not-found", ph));
                return true;
            }
            if (t.getUniqueId().equals(p.getUniqueId())) {
                p.sendMessage(msg("messages.shard-cant-self"));
                return true;
            }
            long amount;
            try {
                amount = Long.parseLong(args[2]);
            } catch (NumberFormatException e) {
                p.sendMessage(msg("messages.shard-usage"));
                return true;
            }
            if (amount <= 0 || !shards.take(p.getUniqueId(), amount)) {
                p.sendMessage(msg("messages.shard-no-enough"));
                return true;
            }
            shards.add(t.getUniqueId(), amount);
            java.util.Map<String, String> ph = new java.util.HashMap<>();
            ph.put("amount", String.valueOf(amount));
            ph.put("target", t.getName());
            p.sendMessage(msg("messages.shard-paid", ph));
            java.util.Map<String, String> ph2 = new java.util.HashMap<>();
            ph2.put("amount", String.valueOf(amount));
            ph2.put("from", p.getName());
            t.sendMessage(msg("messages.shard-received", ph2));
            return true;
        }
        // admin: set/give/take/see/setafk
        if (sub.equals("setafk")) {
            if (!p.hasPermission("pikashop.admin")) {
                p.sendMessage(msg("messages.no-permission"));
                return true;
            }
            afk.save(p.getLocation());
            p.sendMessage(msg("messages.shard-afk-set"));
            return true;
        }
        if ((sub.equals("set") || sub.equals("give") || sub.equals("take") || sub.equals("see"))
                && args.length == (sub.equals("see") ? 2 : 3)) {
            if (!p.hasPermission("pikashop.admin")) {
                p.sendMessage(msg("messages.no-permission"));
                return true;
            }
            @SuppressWarnings("deprecation")
            org.bukkit.OfflinePlayer t = getServer().getOfflinePlayer(args[1]);
            if (t.getName() == null) {
                java.util.Map<String, String> ph = new java.util.HashMap<>();
                ph.put("target", args[1]);
                p.sendMessage(msg("messages.player-not-found", ph));
                return true;
            }
            if (sub.equals("see")) {
                java.util.Map<String, String> ph = new java.util.HashMap<>();
                ph.put("target", t.getName());
                ph.put("balance", String.valueOf(shards.balance(t.getUniqueId())));
                p.sendMessage(msg("messages.shard-see", ph));
                return true;
            }
            long amount;
            try {
                amount = Long.parseLong(args[2]);
            } catch (NumberFormatException e) {
                p.sendMessage(msg("messages.shard-admin-usage"));
                return true;
            }
            java.util.Map<String, String> ph = new java.util.HashMap<>();
            ph.put("target", t.getName());
            ph.put("amount", String.valueOf(amount));
            if (sub.equals("set")) {
                shards.set(t.getUniqueId(), amount);
                p.sendMessage(msg("messages.shard-set", ph));
            } else if (sub.equals("give")) {
                shards.add(t.getUniqueId(), amount);
                p.sendMessage(msg("messages.shard-gave", ph));
            } else {
                if (!shards.take(t.getUniqueId(), amount)) {
                    p.sendMessage(msg("messages.shard-no-enough"));
                    return true;
                }
                p.sendMessage(msg("messages.shard-took", ph));
            }
            return true;
        }
        p.sendMessage(msg(p.hasPermission("pikashop.admin")
                ? "messages.shard-admin-usage" : "messages.shard-usage"));
        return true;
    }

    public String msg(String path) {
        String prefix = ChatColor.translateAlternateColorCodes('&',
                getConfig().getString("messages.prefix", "&d&lPikaShop &7» &f"));
        String body = ChatColor.translateAlternateColorCodes('&',
                getConfig().getString(path, path));
        return prefix + body;
    }

    public String msg(String path, Map<String, String> ph) {
        String out = msg(path);
        if (ph != null) {
            for (Map.Entry<String, String> e : ph.entrySet()) {
                out = out.replace("%" + e.getKey() + "%", e.getValue());
            }
        }
        return out;
    }
}
