package me.saif.betterstats.data;

import me.saif.betterstats.BetterStats;
import me.saif.betterstats.statistics.Stat;
import me.saif.betterstats.utils.Pair;

import java.util.*;

public abstract class DataManger {

    private final BetterStats plugin;
    private final String server;
    private final String dataTableName;
    private final String playerTableName = "betterstats_players";

    public DataManger(BetterStats plugin, String server) {
        this.plugin = plugin;
        this.server = server;
        dataTableName = server + "_stats";
    }

    public abstract String getType();

    public String getDataTableName() {
        return dataTableName;
    }

    public String getPlayersTableName() {
        return playerTableName;
    }

    public String getServer() {
        return server;
    }

    public BetterStats getPlugin() {
        return plugin;
    }

    public abstract void registerStatistics(Stat... stats);

    public abstract void createTables();

    public abstract void saveNameAndUUID(String name, UUID uuid);

    public abstract void finishUp();

    public void saveStatChanges(UUID uuid, Map<Stat, Double> stats) {
        Map<UUID, Map<Stat, Double>> statsMap = new HashMap<>();
        statsMap.put(uuid, stats);
        saveStatChangesMultiple(statsMap);
    }

    public abstract void saveStatChangesMultiple(Map<UUID, Map<Stat, Double>> statsMap);

    public void forceSetStats(UUID uuid, Map<Stat, Double> stats) {
        Map<UUID, Map<Stat, Double>> statsMap = new HashMap<>();
        statsMap.put(uuid, stats);
        forceSetStatsMultiple(statsMap);
    }

    public abstract void forceSetStatsMultiple(Map<UUID, Map<Stat, Double>> statsMap);

    public Map<Stat, Double> getStatsFromUUID(UUID uuid, List<Stat> stats) {
        Map<UUID, Map<Stat, Double>> map = getStatsFromUUIDMultiple(Collections.singleton(uuid), stats);
        if (map.containsKey(uuid))
            return map.get(uuid);
        else return new HashMap<>();
    }

    public abstract Map<UUID, Map<Stat, Double>> getStatsFromUUIDMultiple(Collection<UUID> uuids, List<Stat> stats);

    public Pair<UUID, Map<Stat, Double>> getStatsFromName(String name, List<Stat> stats) {
        return Pair.fromMap(getStatsFromNameMultiple(Collections.singleton(name), stats));
    }

    public abstract Map<UUID, Map<Stat, Double>> getStatsFromNameMultiple(Collection<String> names, List<Stat> stats);
}
