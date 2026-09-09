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
 * R7: Shop mua 2 buoc that (doc config + mua qua Vault). Sell/Multiplier o R8-R9.
 */
public class PikaShop extends JavaPlugin implements CommandExecutor {

    private VaultHook vault;
    private ShopData shops;
    private ShopGui gui;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        vault = new VaultHook(this);
        shops = new ShopData();
        gui = new ShopGui(this);
        shops.load(this);
        getServer().getPluginManager().registerEvents(gui, this);
        register("shop");
        register("sell");
        register("sellmulti");
        register("sellhistory");
        register("worth");
        register("pikashop");
        if (vault.setup()) {
            getLogger().info("PikaShop da noi Vault Economy.");
        } else {
            getLogger().warning("Chua thay Vault/Economy - /shop se bao loi khi mua.");
        }
        getLogger().info("PikaShop R7: shop 2 buoc san sang.");
    }

    public VaultHook vault() { return vault; }
    public ShopData shops() { return shops; }
    public ShopGui gui() { return gui; }

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
        if (name.equals("shop")) {
            gui.openMain(p);
            return true;
        }
        // Sell / Multiplier o R8-R9
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
