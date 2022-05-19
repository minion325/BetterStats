package me.saif.betterstats;

import me.saif.betterstats.commands.BetterStatsCommand;
import me.saif.betterstats.data.DataManger;
import me.saif.betterstats.data.MySQLDataManager;
import me.saif.betterstats.data.SQLiteDataManager;
import me.saif.betterstats.hooks.PlaceholderAPIHook;
import me.saif.betterstats.leaderboard.LeaderboardManager;
import me.saif.betterstats.player.StatPlayerManager;
import me.saif.betterstats.statistics.Stat;
import me.saif.betterstats.statistics.StatisticManager;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.DrilldownPie;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import revxrsal.commands.CommandHandler;
import revxrsal.commands.bukkit.core.BukkitHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class BetterStats extends JavaPlugin {

    private static final int bStatsID = 15173;

    private static BetterStatsAPI API;

    private StatisticManager statisticManager;
    private StatPlayerManager statPlayerManager;
    private DataManger dataManger;
    private LeaderboardManager leaderboardManager;

    private boolean multiServer = false;

    @Override
    public void onEnable() {
        API = new BetterStatsAPI(this);

        this.saveDefaultConfig();

        this.setMultiServer(this.multiServer = getConfig().getBoolean("multiserver", false));

        if (getConfig().getBoolean("sql.mysql", false))
            this.dataManger = new MySQLDataManager(this, getConfig().getString("server-name", "minecraft_server"));
        else {
            this.dataManger = new SQLiteDataManager(this, getConfig().getString("server-name", "minecraft_server"));
        }
        this.statisticManager = new StatisticManager(this);
        this.statPlayerManager = new StatPlayerManager(this);
        this.leaderboardManager = new LeaderboardManager(this);

        Bukkit.getPluginManager().registerEvents(this.statisticManager, this);
        Bukkit.getPluginManager().registerEvents(this.statPlayerManager, this);

        setupCommands();
        if (getConfig().getBoolean("use-placeholders", true))
            hookPAPI();
        setupMetrics();
    }

    private void hookPAPI() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null)
            return;
        new PlaceholderAPIHook().register();
    }

    private void setupMetrics() {
        Metrics metrics = new Metrics(this, bStatsID);
        metrics.addCustomChart(new SimplePie("storage_method", () -> this.dataManger.getType()));
        metrics.addCustomChart(new DrilldownPie("registered_statistics", () -> {
            Map<String,Map<String, Integer>> pluginMap = new HashMap<>();
            for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
                if (plugin instanceof JavaPlugin) {
                    List<Stat> stats = this.statisticManager.getStatsForPlugin(((JavaPlugin) plugin));
                    if (stats != null && stats.size() != 0) {
                        Map<String,Integer> map = new HashMap<>();
                        for (Stat stat : stats) {
                            map.put(stat.getName(), 1);
                        }
                        pluginMap.put(plugin.getName(), map);
                    }
                }
            }
            return pluginMap;
        }));
    }

    private void setupCommands() {
        CommandHandler commandHandler = new BukkitHandler(this)
                .getAutoCompleter().registerSuggestion("stats", (args, sender, command) -> {
                    String lastArg = args.get(args.size() - 1).toLowerCase(Locale.ROOT);
                    return BetterStats.getAPI().getRegisteredStats().stream().map(Stat::getInternalName).filter(s -> s.startsWith(lastArg)).collect(Collectors.toList());
                })
                .registerSuggestion("players", (args, sender, command) -> {
                    String last = args.get(args.size() - 1).toLowerCase(Locale.ROOT);
                    return Bukkit.getOnlinePlayers().stream().map((Function<Player, String>) HumanEntity::getName).filter(s -> s.toLowerCase(Locale.ROOT).startsWith(last)).collect(Collectors.toList());
                }).and()
                .register(new BetterStatsCommand());

        ((BukkitHandler) commandHandler).registerBrigadier();

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

    public boolean isMultiServer() {
        return multiServer;
    }

    public void setMultiServer(boolean multiServer) {
        this.multiServer = multiServer;
    }
}
