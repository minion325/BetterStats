package me.saif.betterstats.data;

import me.saif.betterstats.BetterStats;
import me.saif.betterstats.player.StatPlayerSnapshot;
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

    public void saveStatChanges(UUID uuid, StatPlayerSnapshot snapshot, List<Stat> stats) {
        Map<UUID, StatPlayerSnapshot> statsMap = new HashMap<>();
        statsMap.put(uuid, snapshot);
        saveStatChangesMultiple(statsMap, stats);
    }

    /**
     *
     * @param statsMap Map with statplayersnapshots to be saved to the database. For regular stats, changes are saved
     *                 Other stat types, the actual values are saved.
     * @param stats The stats to be saved.
     */
    public abstract void saveStatChangesMultiple(Map<UUID, StatPlayerSnapshot> statsMap, List<Stat> stats);

    public void forceSetStats(UUID uuid, Map<Stat, Double> stats) {
        Map<UUID, Map<Stat, Double>> statsMap = new HashMap<>();
        statsMap.put(uuid, stats);
        forceSetStatsMultiple(statsMap);
    }

    /**
     *
     * @param statsMap Map with values with will be pushed to the database
     */
    public abstract void forceSetStatsMultiple(Map<UUID, Map<Stat, Double>> statsMap);

    public Pair<String, Map<Stat, Double>> getStatsFromUUID(UUID uuid, List<Stat> stats) {
        Map<UUID, Pair<String, Map<Stat, Double>>> map = getStatsFromUUIDMultiple(Collections.singleton(uuid), stats);
        return map.getOrDefault(uuid, null);
    }

    public abstract Map<UUID, Pair<String, Map<Stat, Double>>> getStatsFromUUIDMultiple(Collection<UUID> uuids, List<Stat> stats);

    public Pair<UUID, Map<Stat, Double>> getStatsFromName(String name, List<Stat> stats) {
        return Pair.fromMap(getStatsFromNameMultiple(Collections.singleton(name), stats));
    }

    public abstract Map<UUID, Map<Stat, Double>> getStatsFromNameMultiple(Collection<String> names, List<Stat> stats);
}
