package vn.pika.pikashop;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;

/** Tu viet: lay Economy tu Vault, khong phu thuoc jar nao luc chay ngoai Vault. */
public class VaultHook {
    private final PikaShop plugin;
    private Economy economy;

    public VaultHook(PikaShop plugin) {
        this.plugin = plugin;
    }

    public boolean setup() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp =
                plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        return economy != null;
    }

    public Economy get() {
        return economy;
    }

    public boolean ready() {
        return economy != null;
    }
}
