package me.saif.betterstats;

import me.saif.betterstats.data.DataManger;
import me.saif.betterstats.data.MySQLDataManager;
import me.saif.betterstats.data.SQLiteDataManager;
import me.saif.betterstats.hooks.PlaceholderAPIHook;
import me.saif.betterstats.player.StatPlayerManager;
import me.saif.betterstats.statistics.StatisticManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class BetterStats extends JavaPlugin {

    private static BetterStatsAPI API;

    private StatisticManager statisticManager;
    private StatPlayerManager statPlayerManager;
    private DataManger dataManger;

    @Override
    public void onEnable() {
        API = new BetterStatsAPI(this);

        this.saveDefaultConfig();

        if (getConfig().getBoolean("sql.mysql", false))
            this.dataManger = new MySQLDataManager(this, getConfig().getString("server_name", "minecraft_server"));
        else {
            this.dataManger = new SQLiteDataManager(this, getConfig().getString("server_name", "minecraft_server"));
        }

        this.statisticManager = new StatisticManager(this);
        this.statPlayerManager = new StatPlayerManager(this);

        Bukkit.getPluginManager().registerEvents(this.statisticManager, this);
        Bukkit.getPluginManager().registerEvents(this.statPlayerManager, this);

        hookPAPI();
    }

    private void hookPAPI() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null)
            return;
        new PlaceholderAPIHook().register();
    }

    @Override
    public void onDisable() {
        this.dataManger.finishUp();
    }

    public static BetterStatsAPI getAPI() {
        return API;
    }

    public StatisticManager getStatisticManager() {
        return statisticManager;
    }

    public StatPlayerManager getStatPlayerManager() {
        return statPlayerManager;
    }

    public DataManger getDataManger() {
        return dataManger;
    }
}
