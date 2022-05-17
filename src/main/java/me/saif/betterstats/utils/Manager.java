package me.saif.betterstats.utils;

import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class Manager<T extends JavaPlugin> implements Listener {

    private final T plugin;

    public Manager(T plugin) {
        this.plugin = plugin;
    }

    public T getPlugin() {
        return plugin;
    }

}
