package io.github.reconsolidated.jediscommunicator;

import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class JedisCommunicator extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        getServer().getServicesManager().register(JedisCommunicator.class, this, this, ServicePriority.Normal);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
