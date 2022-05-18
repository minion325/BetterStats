package me.saif.betterstats.player;

import me.saif.betterstats.BetterStats;
import me.saif.betterstats.data.DataManger;
import me.saif.betterstats.statistics.Stat;
import me.saif.betterstats.statistics.StatisticManager;
import me.saif.betterstats.utils.Callback;
import me.saif.betterstats.utils.Manager;
import me.saif.betterstats.utils.Pair;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class StatPlayerManager extends Manager<BetterStats> {

    private final DataManger dataManger;
    private final StatisticManager statManager;

    private final Map<String, StatPlayer> nameStatMap = new ConcurrentHashMap<>();
    private final Map<UUID, StatPlayer> uuidStatMap = new ConcurrentHashMap<>();
    private final Map<UUID, Callback<StatPlayer>> uuidCallbackMap = new HashMap<>();
    private final Map<String, Callback<StatPlayer>> nameCallbackMap = new HashMap<>();

    public StatPlayerManager(BetterStats plugin) {
        super(plugin);

        this.dataManger = getPlugin().getDataManger();
        this.statManager = getPlugin().getStatisticManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onAsyncJoin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) return;

        dataManger.saveNameAndUUID(event.getName(), event.getUniqueId());

        UUID uuid = event.getUniqueId();
        String name = event.getName();

        if (this.uuidStatMap.containsKey(uuid)) {
            //data is loaded :D so we just need to check that name is the same
            if (!this.nameStatMap.containsKey(name)) {
                //name is different
                StatPlayer statPlayer = this.uuidStatMap.get(uuid);
                for (String s : this.nameStatMap.keySet()) {
                    if (this.nameStatMap.get(s).equals(statPlayer)) {
                        this.nameStatMap.remove(s);
                        this.nameStatMap.put(name, statPlayer);
                        break;
                    }
                }
            }
            return;
        }

        StatPlayer statPlayer = new StatPlayer(uuid, name);

        Map<Stat, Double> statDoubleMap = dataManger.getStatsFromUUID(uuid, statManager.getPersistentStats());
        Map<Stat, Double> toSave = new HashMap<>();
        for (Stat stat : statManager.getRegisteredStats()) {
            if (!statDoubleMap.containsKey(stat)) {
                statPlayer.addStatToMap(stat, stat.getDefaultValue());
                if (stat.isPersistent())
                    toSave.put(stat, stat.getDefaultValue());
                continue;
            }
            statPlayer.addStatToMap(stat, statDoubleMap.get(stat));
        }
        dataManger.forceSetStats(uuid, toSave);
        this.uuidStatMap.put(uuid, statPlayer);
        this.nameStatMap.put(name, statPlayer);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onQuit(PlayerQuitEvent event) {
        saveStatisticsAsync(Collections.singleton(this.uuidStatMap.get(event.getPlayer().getUniqueId())), statManager.getRegisteredStats().toArray(new Stat[]{}));
        this.uuidStatMap.remove(event.getPlayer().getUniqueId());
        this.nameStatMap.remove(event.getPlayer().getName());
    }

    public void registerStatistics(Stat... stats) {
        if (this.uuidStatMap.size() == 0) return;
        Map<UUID, Map<Stat, Double>> statData = this.dataManger.getStatsFromUUIDMultiple(this.uuidStatMap.keySet(), Arrays.stream(stats).filter(Stat::isPersistent).collect(Collectors.toList()));
        Map<UUID, Map<Stat, Double>> toSave = new HashMap<>();
        for (UUID uuid : statData.keySet()) {
            Map<Stat, Double> saveEntry = new HashMap<>();
            Map<Stat, Double> playerStats = statData.get(uuid);
            for (Stat stat : stats) {
                if (!playerStats.containsKey(stat) && stat.isPersistent()) {
                    saveEntry.put(stat, stat.getDefaultValue());
                    uuidStatMap.get(uuid).addStatToMap(stat, stat.getDefaultValue());
                    continue;
                }
                uuidStatMap.get(uuid).addStatToMap(stat, playerStats.get(stat));
            }
            toSave.put(uuid, saveEntry);
        }
        dataManger.forceSetStatsMultiple(toSave);
    }

    public void unRegisterStatistics(Stat... stats) {
        if (this.uuidStatMap.size() == 0) return;
        saveStatistics(new HashSet<>(this.uuidStatMap.values()), stats);
        for (StatPlayer value : this.uuidStatMap.values()) {
            for (Stat stat : stats) {
                value.removeStatFromMap(stat);
            }
        }
    }

    private void saveStatistics(Set<StatPlayer> statPlayers, Stat... stats) {
        Map<UUID, Map<Stat, Double>> data = getMapToSave(statPlayers, stats);
        for (StatPlayer statPlayer : statPlayers) {
            statPlayer.flushChanges(stats);
        }
        this.dataManger.saveStatChangesMultiple(data);
    }

    private void saveStatisticsAsync(Set<StatPlayer> statPlayers, Stat... stats) {
        Map<UUID, Map<Stat, Double>> data = getMapToSave(statPlayers, stats);
        for (StatPlayer statPlayer : statPlayers) {
            statPlayer.flushChanges(stats);
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                dataManger.saveStatChangesMultiple(data);
            }
        }.runTaskAsynchronously(this.getPlugin());
    }

    private Map<UUID, Map<Stat, Double>> getMapToSave(Set<StatPlayer> statPlayers, Stat... stats) {
        Map<UUID, Map<Stat, Double>> statMap = new HashMap<>();
        for (StatPlayer statPlayer : statPlayers) {
            Map<Stat, Double> statDoubleMap = statPlayer.getChanges();
            for (Stat stat : stats) {
                if (!statDoubleMap.containsKey(stat) && stat.isPersistent())
                    statDoubleMap.put(stat, statPlayer.getStat(stat));
            }
            statMap.put(statPlayer.getUuid(), statDoubleMap);
        }
        return statMap;
    }

    public Set<StatPlayer> getMultipleStats(UUID... uuids) {
        Set<StatPlayer> statPlayers = new HashSet<>();
        for (UUID uuid : uuids) {
            StatPlayer statPlayer = uuidStatMap.get(uuid);
            if (statPlayer != null) statPlayers.add(statPlayer);
        }
        return statPlayers;
    }

    public StatPlayer getStats(UUID uuid) {
        Set<StatPlayer> stats = getMultipleStats(uuid);
        if (stats.size() == 0) return null;
        return stats.iterator().next();
    }

    public Callback<StatPlayer> getStatPlayer(OfflinePlayer offlinePlayer) {
        return Callback.withResult(null);
    }

    public Callback<StatPlayer> getStatPlayer(UUID uuid) {
        return Callback.withResult(null);

    }

    public Callback<StatPlayer> getStatPlayer(String name) {
        return Callback.withResult(null);
    }
}
