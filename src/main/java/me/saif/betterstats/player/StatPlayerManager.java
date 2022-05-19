package me.saif.betterstats.player;

import me.saif.betterstats.BetterStats;
import me.saif.betterstats.data.DataManger;
import me.saif.betterstats.statistics.Stat;
import me.saif.betterstats.statistics.StatisticManager;
import me.saif.betterstats.utils.Callback;
import me.saif.betterstats.utils.Manager;
import me.saif.betterstats.utils.Pair;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class StatPlayerManager extends Manager<BetterStats> implements PluginMessageListener {

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

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onAsyncJoin(new AsyncPlayerPreLoginEvent(onlinePlayer.getName(), null, onlinePlayer.getUniqueId()));
        }

        Bukkit.getScheduler().scheduleSyncRepeatingTask(getPlugin(), () -> {
            Set<StatPlayer> onlineStatPlayers = Bukkit.getOnlinePlayers().stream().map((Function<Player, StatPlayer>) player -> StatPlayerManager.this.uuidStatMap.get(player.getUniqueId())).collect(Collectors.toSet());

            saveStatisticsAsync(onlineStatPlayers, this.statManager.getRegisteredStats());

        }, getPlugin().getConfig().getInt("autosave-interval") * 20L, getPlugin().getConfig().getInt("autosave-interval") * 20L);

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

            if(removed.size() == 0)
                return;

            saveStatisticsAsync(removed, statManager.getPersistentStats());
            getPlugin().getLogger().info("Cleared " + removed.size() + " cached stat entries");

        }, getPlugin().getConfig().getInt("cache-clearing-interval") * 20L, getPlugin().getConfig().getInt("cache-clearing-interval") * 20L);
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, @NotNull byte[] message) {
        System.out.println("Message received");
        System.out.println(new String(message));
    }

    private void sendMessage(Player player) {
        player.sendPluginMessage(getPlugin(), getPlugin().getChannelName(), "Hello".getBytes());
    }

    @EventHandler
    private void onPlayerInteract(PlayerInteractEvent event){
        sendMessage(event.getPlayer());
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

        Pair<String, Map<Stat, Double>> data = dataManger.getStatsFromUUID(uuid, statManager.getPersistentStats());
        Map<Stat, Double> statDoubleMap = data == null ? new HashMap<>() : data.getSecond();
        Map<Stat, Double> toSave = new HashMap<>();
        for (Stat stat : statManager.getRegisteredStats()) {
            if (statDoubleMap.get(stat) == null) {
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


    /**
     *
     * If bungee is being used, we want to reload the stats some time after it gets a chance to be saved from the previous server.
     */
    @EventHandler
    private void onPlayerJoin(PlayerJoinEvent event) {
        if (!getPlugin().isMultiServer())
            return;

        UUID uuid = event.getPlayer().getUniqueId();
        List<Stat> registered = statManager.getRegisteredStats();
        Bukkit.getScheduler().runTaskAsynchronously(getPlugin(), () -> {
            Pair<String, Map<Stat, Double>> data = dataManger.getStatsFromUUID(uuid, registered);
            Bukkit.getScheduler().runTask(getPlugin(), () -> {
                Map<Stat, Double> statDoubleMap = data == null ? new HashMap<>() : data.getSecond();

                StatPlayer statPlayer = getStats(uuid);

                Map<Stat, Double> changes = statPlayer.getChanges();

                for (Stat stat : statDoubleMap.keySet()) {
                    statPlayer.setBaseValue(stat, statDoubleMap.get(stat));
                }
                statPlayer.addChanges(changes);
            });
        });


    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onQuit(PlayerQuitEvent event) {
        saveStatisticsAsync(Collections.singleton(this.uuidStatMap.get(event.getPlayer().getUniqueId())), statManager.getRegisteredStats());
        this.uuidStatMap.remove(event.getPlayer().getUniqueId());
        this.nameStatMap.remove(event.getPlayer().getName());
    }

    public void registerStatistics(Stat... stats) {
        if (this.uuidStatMap.size() == 0) return;
        Map<UUID, Pair<String, Map<Stat, Double>>> statData = this.dataManger.getStatsFromUUIDMultiple(this.uuidStatMap.keySet(), Arrays.stream(stats).filter(Stat::isPersistent).collect(Collectors.toList()));
        Map<UUID, Map<Stat, Double>> toSave = new HashMap<>();
        for (UUID uuid : statData.keySet()) {
            Map<Stat, Double> saveEntry = new HashMap<>();
            Map<Stat, Double> playerStats = statData.get(uuid).getSecond();
            for (Stat stat : stats) {
                if (playerStats.get(stat) != null && stat.isPersistent()) {
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
        saveStatistics(new HashSet<>(this.uuidStatMap.values()), Arrays.stream(stats).toList());
        for (StatPlayer value : this.uuidStatMap.values()) {
            for (Stat stat : stats) {
                value.removeStatFromMap(stat);
            }
        }
    }

    //TODO
    private void saveStatistics(Set<StatPlayer> statPlayers, List<Stat> stats) {
        Map<UUID, StatPlayerSnapshot> data = new HashMap<>();
        for (StatPlayer statPlayer : statPlayers) {
            data.put(statPlayer.getUuid(), statPlayer.snapshot());
            statPlayer.flushChanges(stats);
        }
        this.dataManger.saveStatChangesMultiple(data, stats);
    }

    private void saveStatisticsAsync(Set<StatPlayer> statPlayers, List<Stat> stats) {
        Map<UUID, StatPlayerSnapshot> data = new HashMap<>();
        for (StatPlayer statPlayer : statPlayers) {
            data.put(statPlayer.getUuid(), statPlayer.snapshot());
            statPlayer.flushChanges(stats);
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                dataManger.saveStatChangesMultiple(data, stats);
            }
        }.runTaskAsynchronously(this.getPlugin());
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
        return uuidStatMap.get(uuid);
    }

    public Callback<StatPlayer> getStatPlayer(UUID uuid) {
        if (uuidCallbackMap.containsKey(uuid))
            return uuidCallbackMap.get(uuid);

        if (this.uuidStatMap.containsKey(uuid))
            return Callback.withResult(this.uuidStatMap.get(uuid));

        Callback<StatPlayer> callback = new Callback<>();
        this.uuidCallbackMap.put(uuid, callback);

        Bukkit.getScheduler().runTaskAsynchronously(this.getPlugin(), () -> {
            List<Stat> stats = getPlugin().getStatisticManager().getRegisteredStats();
            Pair<String, Map<Stat, Double>> statData = dataManger.getStatsFromUUID(uuid, stats);
            if (statData == null) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        callback.setResult(null);
                    }
                }.runTask(getPlugin());
                return;
            }
            StatPlayer statPlayer = new StatPlayer(uuid, statData.getFirst());
            Map<Stat, Double> toSave = new HashMap<>();
            for (Stat stat : stats) {
                if (statData.getSecond().get(stat) == null) {
                    statPlayer.addStatToMap(stat, stat.getDefaultValue());
                    if (stat.isPersistent())
                        toSave.put(stat, stat.getDefaultValue());
                    continue;
                }
                statPlayer.addStatToMap(stat, statData.getSecond().get(stat));
            }
            dataManger.forceSetStats(uuid, toSave);
            new BukkitRunnable() {
                @Override
                public void run() {
                    callback.setResult(statPlayer);
                }
            }.runTask(this.getPlugin());
        });
        callback.addResultListener(() -> {
            this.uuidCallbackMap.remove(uuid);

            if (callback.getResult() != null) {
                this.uuidStatMap.put(uuid, callback.getResult());
                if (callback.getResult().getName() == null || callback.getResult().getName().equals(""))
                    return;
                this.nameStatMap.put(callback.getResult().getName(), callback.getResult());
            }
        });
        return callback;
    }

    public Callback<StatPlayer> getStatPlayer(String name) {
        if (nameCallbackMap.containsKey(name))
            return nameCallbackMap.get(name);

        if (this.nameStatMap.containsKey(name))
            return Callback.withResult(this.nameStatMap.get(name));

        Callback<StatPlayer> callback = new Callback<>();
        this.nameCallbackMap.put(name, callback);

        Bukkit.getScheduler().runTaskAsynchronously(this.getPlugin(), () -> {
            List<Stat> stats = getPlugin().getStatisticManager().getRegisteredStats();
            Pair<UUID, Map<Stat, Double>> statData = dataManger.getStatsFromName(name, stats);
            if (statData == null) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        callback.setResult(null);
                    }
                }.runTask(getPlugin());
                return;
            }
            StatPlayer statPlayer = new StatPlayer(statData.getFirst(), name);
            Map<Stat, Double> toSave = new HashMap<>();
            for (Stat stat : stats) {
                if (statData.getSecond().get(stat) == null) {
                    statPlayer.addStatToMap(stat, stat.getDefaultValue());
                    if (stat.isPersistent())
                        toSave.put(stat, stat.getDefaultValue());
                    continue;
                }
                statPlayer.addStatToMap(stat, statData.getSecond().get(stat));
            }
            dataManger.forceSetStats(statData.getFirst(), toSave);
            new BukkitRunnable() {
                @Override
                public void run() {
                    callback.setResult(statPlayer);
                }
            }.runTask(this.getPlugin());
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
