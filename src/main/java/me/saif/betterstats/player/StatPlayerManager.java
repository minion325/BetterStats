package me.saif.betterstats.player;

import me.saif.betterstats.BetterStats;
import me.saif.betterstats.data.DataManger;
import me.saif.betterstats.statistics.Stat;
import me.saif.betterstats.utils.Callback;
import me.saif.betterstats.utils.Manager;
import me.saif.betterstats.utils.Pair;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class StatPlayerManager extends Manager {

    private final Map<String, StatPlayer> nameStatMap = new ConcurrentHashMap<>();
    private final Map<UUID, StatPlayer> uuidStatMap = new ConcurrentHashMap<>();
    private final Map<UUID, Callback<StatPlayer>> uuidCallbackMap = new HashMap<>();
    private final Map<String, Callback<StatPlayer>> nameCallbackMap = new HashMap<>();

    private final DataManger dataManger;

    public StatPlayerManager(BetterStats stats) {
        super(stats);
        this.dataManger = getPlugin().getDataManger();
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            dataManger.saveNameAndUUID(onlinePlayer.getName(), onlinePlayer.getUniqueId());
            StatPlayer statPlayer = getNewStatPlayer(onlinePlayer.getUniqueId());
            this.nameStatMap.put(onlinePlayer.getName(), statPlayer);
            this.uuidStatMap.put(onlinePlayer.getUniqueId(), statPlayer);
        }

        Bukkit.getScheduler().scheduleSyncRepeatingTask(getPlugin(), () -> {
            Set<StatPlayer> removed = new HashSet<>();
            for (String s : this.nameStatMap.keySet()) {
                Player player = Bukkit.getPlayer(s);
                if (player == null || !player.getName().equalsIgnoreCase(s)) {
                    removed.add(this.nameStatMap.remove(s));
                }
            }
            for (UUID uuid : this.uuidStatMap.keySet()) {
                if (Bukkit.getPlayer(uuid) == null) {
                    removed.add(this.uuidStatMap.remove(uuid));
                }
            }
            Set<StatPlayer> onlineStatPlayers = Bukkit.getOnlinePlayers().stream().map((Function<Player, StatPlayer>) player -> StatPlayerManager.this.uuidStatMap.get(player.getUniqueId())).collect(Collectors.toSet());

            Bukkit.getScheduler().runTaskAsynchronously(getPlugin(), () -> {
                this.dataManger.saveStatistics(removed, this.getPlugin().getStatisticManager().getPersistentStats());
                getPlugin().getLogger().info("Cleared " + removed.size() + " cached stat entries");

                getPlugin().getLogger().info("Saving stats for online players");
                this.dataManger.saveStatistics(onlineStatPlayers, this.getPlugin().getStatisticManager().getPersistentStats());
            });

        }, 1L, getPlugin().getConfig().getInt("cache-clearing-interval") * 20L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) return;

        initPlayer(event.getName(), event.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onLogout(PlayerQuitEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(this.getPlugin(), () -> {
            this.getPlugin().getDataManger().saveStatistics(this.uuidStatMap.get(event.getPlayer().getUniqueId()), this.getPlugin().getStatisticManager().getPersistentStats());
            this.uuidStatMap.remove(event.getPlayer().getUniqueId());
            this.nameStatMap.remove(event.getPlayer().getName());
        });
    }

    private void initPlayer(String name, UUID uuid) {
        dataManger.saveNameAndUUID(name, uuid);

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
        }

        Map<Stat, Double> statData = this.dataManger.getStatPlayerDataByUUID(uuid, getPlugin().getStatisticManager().getPersistentStats());

        StatPlayer statPlayer = getNewStatPlayer(uuid);

        if (statData != null) {
            statData.forEach(statPlayer::setStat);
        }

        this.uuidStatMap.put(uuid, statPlayer);
        this.nameStatMap.put(name, statPlayer);

    }

    public Set<StatPlayer> getMultipleStats(UUID... uuids) {
        Set<StatPlayer> statPlayers = new HashSet<>();
        for (UUID uuid : uuids) {
            StatPlayer statPlayer = uuidStatMap.get(uuid);
            if (statPlayer != null) statPlayers.add(statPlayer);
        }
        return statPlayers;
    }

    public void registerStatistics(Stat... stats) {
        if (this.uuidStatMap.size() == 0) return;
        Map<UUID, Map<Stat, Double>> statData = this.dataManger.getStatPlayersDataByUUIDs(this.uuidStatMap.keySet(), Arrays.stream(stats).filter(Stat::isPersistent).collect(Collectors.toList()));
        for (UUID uuid : statData.keySet()) {
            for (Stat stat : stats)
                ((StatPlayer) uuidStatMap.get(uuid)).addStatToMap(stat, stat.getDefaultValue());
            Map<Stat, Double> playerStats = statData.get(uuid);
            playerStats.forEach((stat, aDouble) -> uuidStatMap.get(uuid).setStat(stat, aDouble));
        }
    }

    public void unRegisterStatistics(Stat... stats) {
        if (this.uuidStatMap.size() == 0) return;
        this.dataManger.saveStatistics(new HashSet<>(this.uuidStatMap.values()), Arrays.stream(stats).toList());
        for (StatPlayer value : this.uuidStatMap.values()) {
            for (Stat stat : stats) {
                value.removeStatFromMap(stat);
            }
        }
    }

    public StatPlayer getStats(UUID uuid) {
        Set<StatPlayer> stats = getMultipleStats(uuid);
        if (stats.size() == 0) return null;
        return stats.iterator().next();
    }

    private StatPlayer getNewStatPlayer(UUID uuid) {
        StatPlayer statPlayer = new StatPlayer(uuid);
        for (Stat registeredStat : this.getPlugin().getStatisticManager().getRegisteredStats()) {
            statPlayer.addStatToMap(registeredStat, registeredStat.getDefaultValue());
        }
        return statPlayer;
    }

    public Callback<StatPlayer> getStatPlayer(UUID uuid) {
        if (uuidCallbackMap.containsKey(uuid)) return uuidCallbackMap.get(uuid);

        Callback<StatPlayer> callback = new Callback<>(this.getPlugin());

        if (this.uuidStatMap.containsKey(uuid)) {
            callback.setResult(this.uuidStatMap.get(uuid));
            return callback;
        }

        uuidCallbackMap.put(uuid, callback);
        Bukkit.getScheduler().runTaskAsynchronously(this.getPlugin(), () -> {
            Map<Stat, Double> statData = this.dataManger.getStatPlayerDataByUUID(uuid, getPlugin().getStatisticManager().getPersistentStats());
            if (statData == null) {
                callback.setResult(null);
                return;
            }

            StatPlayer statPlayer = getNewStatPlayer(uuid);
            statData.forEach(statPlayer::setStat);
            callback.setResult(statPlayer);
        });

        callback.addResultListener(() -> {
            this.uuidCallbackMap.remove(uuid);

            if (callback.getResult() != null) {
                this.uuidStatMap.put(uuid, callback.getResult());
                this.nameStatMap.put(callback.getResult().getPlayer().getName(), callback.getResult());
            }
        });
        return callback;
    }

    public Callback<StatPlayer> getStatPlayer(String name) {
        if (nameCallbackMap.containsKey(name)) return nameCallbackMap.get(name);

        Callback<StatPlayer> callback = new Callback<>(this.getPlugin());

        if (nameStatMap.containsKey(name)) {
            callback.setResult(this.nameStatMap.get(name));
            return callback;
        }

        nameCallbackMap.put(name, callback);
        Bukkit.getScheduler().runTaskAsynchronously(this.getPlugin(), () -> {
            Pair<UUID, Map<Stat, Double>> statData = this.dataManger.getStatPlayerDataByName(name, getPlugin().getStatisticManager().getPersistentStats());
            if (statData == null) {
                callback.setResult(null);
                return;
            }

            StatPlayer statPlayer = getNewStatPlayer(statData.getFirst());
            statData.getSecond().forEach(statPlayer::setStat);
            callback.setResult(statPlayer);
        });

        callback.addResultListener(() -> {
            this.nameCallbackMap.remove(name);
            if (callback.getResult() != null) {
                this.nameStatMap.put(name, callback.getResult());
                this.uuidStatMap.put(callback.getResult().getUuid(), callback.getResult());
            }
        });
        return callback;
    }


}
