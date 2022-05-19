package me.saif.betterstats;

import me.saif.betterstats.player.StatPlayer;
import me.saif.betterstats.statistics.Stat;
import me.saif.betterstats.utils.Callback;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class BetterStatsAPI {

    private final BetterStats plugin;

    BetterStatsAPI(BetterStats plugin) {
        this.plugin = plugin;
    }

    /**
     *
     * @param player Online player's stats you would like to receive
     * @return Players stats
     */
    public StatPlayer getPlayerStats(Player player) {
        return this.plugin.getStatPlayerManager().getStats(player.getUniqueId());
    }

    /**
     * @param player offlineplayer instance of the player's stats you wish to retrieve.
     * @return a callback which you can then add a completion listener to run code when the stat returns
     */
    public Callback<StatPlayer> getPlayerStatsCallback(OfflinePlayer player) {
        return this.plugin.getStatPlayerManager().getStatPlayer(player.getUniqueId());
    }

    /**
     * @param name name of the player's stats you wish to retrieve.
     * @return a callback which you can then add a completion listener to run code when the stat returns
     */
    public Callback<StatPlayer> getPlayerStats(String name) {
        return this.plugin.getStatPlayerManager().getStatPlayer(name);
    }

    /**
     * @param uuid UUid of the player's stats you wish to retrieve.
     * @return a callback which you can then add a completion listener to run code when the stat returns
     */
    public Callback<StatPlayer> getPlayerStats(UUID uuid) {
        return this.plugin.getStatPlayerManager().getStatPlayer(uuid);
    }

    /**
     * @param plugin plugin that is registering the stat
     * @param stats  Register stats for your plugin
     */
    public void registerStats(JavaPlugin plugin, Stat... stats) {
        this.plugin.getStatisticManager().registerStats(plugin, stats);
    }

    /**
     * @param plugin Unregisters all the stats for your plugin saving them in the process.
     *               This is not necessary on shutdown of your plugin as it is done automatically.
     */
    public void unregisterStats(JavaPlugin plugin) {
        this.plugin.getStatisticManager().unRegisterStats(plugin);
    }

    /**
     * @param plugin plugin that is registering the stat
     * @param stats   Unregisters these stats for your plugin saving it in the process.
     *               This is not necessary on shutdown of your plugin as it is done automatically.
     */
    public void unregisterStat(JavaPlugin plugin, Stat... stats) {
        this.plugin.getStatisticManager().unRegisterStats(plugin, stats);
    }

    /**
     *
     * @return All currently registered statistics in alphabetical order
     */
    public List<Stat> getRegisteredStats() {
        return this.plugin.getStatisticManager().getRegisteredStats();
    }

    /**
     *
     * @return All currently registered statistics in alphabetical order that are visible
     */
    public List<Stat> getVisibleStats() {
        return this.plugin.getStatisticManager().getVisibleStats();
    }

    /**
     *
     * @param statClass class of statstic you want to get
     * @param <T> Instance of stat
     * @return an instance of the stat registered with the statClass. returns null if none is registered
     */
    public <T extends Stat> Set<T> getStat(Class<T> statClass) {
        return this.plugin.getStatisticManager().getStats(statClass);
    }

    /**
     *
     * @param name name of the statistic
     * @return whether a statistic is registered with that name
     */
    public boolean isRegistered(String name) {
        return getStat(name) != null;
    }

    /**
     *
     * @param clazz class of the statistic
     * @return whether a statistic is registered with that class
     */
    public boolean isRegistered(Class<? extends Stat> clazz) {
        return getStat(clazz)  != null;
    }

    /**
     *
     * @param name Internal name of the statistic (All lower case and spaces replaced with '_';
     * @return The statistic with that Internal name;
     */
    public Stat getStat(String name) {
        return this.plugin.getStatisticManager().getStatistic(name);
    }

    /**
     * This should be called if your plugin is setup in bungee mode and want to make sure that multiserver mode is enabled.
     * It does not change the value in the config.yml
     * @param multiServer Whether this plugin should be in multiserver mode.
     */
    public void setMultiServer(boolean multiServer){
        plugin.setMultiServer(true);
    }
}
