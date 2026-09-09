package vn.pika.pikashop;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * PikaShop 0.1.0 - khung MVP (R6).
 * Tu viet (clean-room): chi hoc y tuong Shop 2 buoc / Sell tha do / Multiplier moc.
 * R6 moi co lenh stub + config mau. R7-R9 se them Shop that, Sell that, Multiplier that.
 */
public class PikaShop extends JavaPlugin implements CommandExecutor {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        register("shop");
        register("sell");
        register("sellmulti");
        register("sellhistory");
        register("worth");
        register("pikashop");
        getLogger().info("PikaShop 0.1.0 scaffold da san sang (R6).");
    }

    @Override
    public void onDisable() {
        getLogger().info("PikaShop da tat.");
    }

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
                sender.sendMessage(color("messages.no-permission"));
                return true;
            }
            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                reloadConfig();
                sender.sendMessage(color("messages.reloaded"));
                return true;
            }
            sender.sendMessage(ChatColor.LIGHT_PURPLE + "/pikashop reload");
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage("Chi nguoi choi moi dung duoc lenh nay.");
            return true;
        }
        sender.sendMessage(prefix() + getConfig().getString("messages.developing", "tinh nang dang phat trien (R6 scaffold)."));
        return true;
    }

    private String prefix() {
        return ChatColor.translateAlternateColorCodes('&',
                getConfig().getString("messages.prefix", "&d&lPikaShop &7» &f"));
    }

    private String color(String path) {
        return prefix() + ChatColor.translateAlternateColorCodes('&',
                getConfig().getString(path, ""));
    }
}
