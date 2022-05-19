package me.saif.betterstats.statistics;

import me.saif.betterstats.BetterStats;
import me.saif.betterstats.utils.Manager;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class StatisticManager extends Manager<BetterStats> {

    private final Map<Class<? extends Stat>, Set<Stat>> classStatMap = new ConcurrentHashMap<>();
    private final Map<String, Stat> nameStatMap = new ConcurrentHashMap<>();
    private final Map<JavaPlugin, Set<Stat>> pluginStatsMap = new ConcurrentHashMap<>();
    private final List<Stat> stats = new ArrayList<>();

    public StatisticManager(BetterStats plugin) {
        super(plugin);
        this.getPlugin().getDataManger().createTables();
    }

    public Stat getStatistic(String name) {
        return this.nameStatMap.get(name);
    }

    public Set<Stat> getStatistic(Class<? extends Stat> clazz) {
        return this.classStatMap.get(clazz);
    }

    public void registerStat(JavaPlugin plugin, Stat stat) {
        this.registerStats(plugin, stat);
    }

    public void registerStats(JavaPlugin plugin, Stat... stats) {
        Set<Stat> registeredStats;

        if (this.pluginStatsMap.containsKey(plugin))
            registeredStats = pluginStatsMap.get(plugin);
        else {
            registeredStats = new HashSet<>();
            this.pluginStatsMap.put(plugin, registeredStats);
        }

        Set<Stat> toRegister = new HashSet<>();
        for (Stat stat : stats) {
            try {
                if (nameStatMap.containsKey(stat.getInternalName())) {
                    getPlugin().getLogger().warning("Already a registered stat with the name: " + stat.getInternalName());
                    getPlugin().getLogger().warning("Ignoring registration of " + stat.getInternalName());
                    continue;
                }
                if (registeredStats.contains(stat)) {
                    getPlugin().getLogger().warning(stat.getInternalName() + " is already registered for " + plugin.getName());
                    getPlugin().getLogger().warning("Ignoring registration of " + stat.getInternalName());
                    continue;
                }
                getPlugin().getLogger().info("Registering " + stat.getInternalName() + " for " + plugin.getName());
                if (stat instanceof Listener)
                    Bukkit.getPluginManager().registerEvents((Listener) stat, plugin);
                toRegister.add(stat);
                registeredStats.add(stat);
                nameStatMap.put(stat.getInternalName(), stat);

                Set<Stat> statSet = classStatMap.get(stat.getClass());
                if (statSet == null)
                    this.classStatMap.put(stat.getClass(), statSet = new HashSet<>());
                statSet.add(stat);
            } catch (Exception e) {
                plugin.getLogger().severe("Error registering " + stat.getInternalName() + " for " + plugin.getName());
                e.printStackTrace();
            }
        }

        this.stats.addAll(toRegister);
        this.stats.sort(Comparator.comparing(Stat::getName));
        this.getPlugin().getDataManger().registerStatistics(toRegister.toArray(new Stat[]{}));
        this.getPlugin().getStatPlayerManager().registerStatistics(toRegister.toArray(new Stat[]{}));
        for (Stat stat : toRegister)
            try {
                stat.onRegister();
            } catch (Exception e) {
                plugin.getLogger().severe("Error executing onRegister for Stat:" + stat.getInternalName());
                e.printStackTrace();
            }
    }

    public void unRegisterStats(JavaPlugin plugin, Stat... stats) {
        Set<Stat> pluginStats = this.pluginStatsMap.get(plugin);

        if (pluginStats == null || pluginStats.size() == 0)
            return;

        Set<Stat> setWeGonnaUse = new HashSet<>();
        for (Stat stat : stats) {
            if (pluginStats.contains(stat))
                setWeGonnaUse.add(stat);
        }
        setWeGonnaUse.forEach(Stat::onRegister);


        this.getPlugin().getStatPlayerManager().unRegisterStatistics(stats);
        for (Stat stat : setWeGonnaUse) {
            this.nameStatMap.remove(stat.getInternalName());
            this.classStatMap.get(stat.getClass()).remove(stat);

            if (classStatMap.get(stat.getClass()).isEmpty())
                classStatMap.remove(stat.getClass());

            this.pluginStatsMap.get(plugin).remove(stat);
            this.stats.remove(stat);
            if (stat instanceof Listener)
                HandlerList.unregisterAll((Listener) stat);
        }
    }

    public void unRegisterStats(JavaPlugin plugin) {
        if (this.pluginStatsMap.containsKey(plugin)) {
            Set<Stat> stats = this.pluginStatsMap.get(plugin);
            stats.forEach(Stat::onUnregister);
            this.getPlugin().getStatPlayerManager().unRegisterStatistics(stats.toArray(new Stat[]{}));
            for (Stat stat : stats) {
                this.nameStatMap.remove(stat.getInternalName());
                this.classStatMap.get(stat.getClass()).remove(stat);

                if (classStatMap.get(stat.getClass()).isEmpty())
                    classStatMap.remove(stat.getClass());

                this.stats.remove(stat);
                if (stat instanceof Listener)
                    HandlerList.unregisterAll((Listener) stat);
            }
            this.pluginStatsMap.remove(plugin);
        }
    }

    public List<Stat> getRegisteredStats() {
        return new ArrayList<>(stats);
    }

    public List<Stat> getPersistentStats() {
        return stats.stream().filter(Stat::isPersistent).collect(Collectors.toList());
    }

    public List<Stat> getExternalStats() {
        return stats.stream().filter(stat -> stat instanceof OfflineExternalStat).collect(Collectors.toList());
    }

    public List<Stat> getDependantStats() {
        return stats.stream().filter(stat -> stat instanceof DependantStat).collect(Collectors.toList());
    }

    public List<Stat> getStatsForPlugin(JavaPlugin plugin) {
        return new ArrayList<>(pluginStatsMap.get(plugin));
    }

    public List<Stat> getVisibleStats() {
        return stats.stream().filter(Stat::isVisible).collect(Collectors.toList());
    }

    @EventHandler
    private void onPluginDisable(PluginDisableEvent event) {
        if (!(event.getPlugin() instanceof JavaPlugin))
            return;
        if (event.getPlugin() == this.getPlugin()) {
            for (JavaPlugin javaPlugin : this.pluginStatsMap.keySet()) {
                unRegisterStats(javaPlugin);
            }
            return;
        }

        unRegisterStats((JavaPlugin) event.getPlugin());
    }

    public <T extends Stat> Set<T> getStats(Class<T> statClass) {
        Set<T> set = new HashSet<>();
        Set<Stat> fromClassMap = classStatMap.get(statClass);

        if (fromClassMap != null) {
            for (Stat stat : classStatMap.get(statClass)) {
                set.add(statClass.cast(stat));
            }
        }
        return set;

    }


}
