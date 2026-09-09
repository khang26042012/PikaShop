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
 * R7: Shop mua 2 buoc that. R8: Sell tha-do + history + worth. R9: Multiplier.
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
        shops.load(this);
        sells.load(this);
        store.load();
        getServer().getPluginManager().registerEvents(gui, this);
        getServer().getPluginManager().registerEvents(sellGui, this);
        getServer().getPluginManager().registerEvents(multiGui, this);
        register("pshop");
        register("psell");
        register("psellmulti");
        register("psellhistory");
        register("pworth");
        register("pikashop");
        if (vault.setup()) {
            getLogger().info("PikaShop da noi Vault Economy.");
        } else {
            getLogger().warning("Chua thay Vault/Economy - mua/ban se bao loi.");
        }
        getLogger().info("PikaShop R9: shop 2 buoc + sell tha-do + multiplier san sang.");
    }

    public VaultHook vault() { return vault; }
    public ShopData shops() { return shops; }
    public ShopGui gui() { return gui; }
    public SellData sells() { return sells; }
    public SellStore store() { return store; }
    public SellGui sellGui() { return sellGui; }
    public Multiplier multi() { return multi; }
    public MultiGui multiGui() { return multiGui; }

    private void register(String name) {
        if (getCommand(name) != null) {
            getCommand(name).setExecutor(this);
        } else {
            getLogger().warning("Khong thay lenh trong plugin.yml: " + name);
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
                sender.sendMessage(msg("messages.reloaded"));
                return true;
            }
            sender.sendMessage(ChatColor.LIGHT_PURPLE + "/pikashop reload");
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage("Chi nguoi choi moi dung duoc lenh nay.");
            return true;
        }
        Player p = (Player) sender;
        if (!p.hasPermission("pikashop.use")) {
            p.sendMessage(msg("messages.no-permission"));
            return true;
        }
        if (name.equals("pshop")) {
            gui.openMain(p);
            return true;
        }
        if (name.equals("psell")) {
            sellGui.open(p);
            return true;
        }
        if (name.equals("psellhistory")) {
            sellGui.showHistory(p);
            return true;
        }
        if (name.equals("pworth")) {
            boolean all = args.length > 0 && args[0].equalsIgnoreCase("all");
            sellGui.showWorth(p, all);
            return true;
        }
        if (name.equals("psellmulti")) {
            multiGui.open(p);
            return true;
        }
        p.sendMessage(msg("messages.developing"));
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
