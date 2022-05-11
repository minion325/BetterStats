package me.saif.betterstats.data;

import me.saif.betterstats.BetterStats;
import me.saif.betterstats.player.StatPlayer;
import me.saif.betterstats.statistics.Stat;
import me.saif.betterstats.utils.Pair;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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

    public abstract void saveStatistics(StatPlayer statPlayer, List<Stat> stats);

    public abstract void saveStatistics(Set<StatPlayer> statPlayers, List<Stat> stats);

    public abstract Map<UUID, Map<Stat, Double>> getStatPlayersDataByUUIDs(Set<UUID> uuids, List<Stat> stats);

    public abstract Map<UUID, Map<Stat, Double>> getStatPlayersDataByNames(Set<String> names, List<Stat> stats);

    public abstract Map<Stat, Double> getStatPlayerDataByUUID(UUID uuid, List<Stat> stats);

    public abstract Pair<UUID, Map<Stat, Double>> getStatPlayerDataByName(String name, List<Stat> stats);

    public abstract void saveNameAndUUID(String name, UUID uuid);

    public abstract void finishUp();
}
