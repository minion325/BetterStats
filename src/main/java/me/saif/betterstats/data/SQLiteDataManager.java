package me.saif.betterstats.data;

import me.saif.betterstats.BetterStats;
import me.saif.betterstats.data.database.Database;
import me.saif.betterstats.data.database.SQLiteDatabase;
import me.saif.betterstats.statistics.DependantStat;
import me.saif.betterstats.statistics.OfflineExternalStat;
import me.saif.betterstats.statistics.OnlineExternalStat;
import me.saif.betterstats.statistics.Stat;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class SQLiteDataManager extends DataManger {

    private Database database;

    public SQLiteDataManager(BetterStats plugin, String server) {
        super(plugin, server);
        try {
            this.database = new SQLiteDatabase(new File(plugin.getDataFolder(), "data.db"));
        } catch (IOException e) {
            e.printStackTrace();
            plugin.getLogger().severe("Could not load the database properly. Shutting down...");
            Bukkit.getPluginManager().disablePlugin(this.getPlugin());
        }
    }

    @Override
    public String getType() {
        return "SQLite";
    }

    @Override
    public void registerStatistics(Stat... stats) {
        String getColumns = "PRAGMA table_info(" + getDataTableName() + ")";
        try {
            Connection connection = database.getConnection();
            Statement statement = connection.createStatement();

            String createColumns = "ALTER TABLE " + getDataTableName() + " ADD %column% REAL;";
            //getting the current columns in the table
            ResultSet set = statement.executeQuery(getColumns);
            Set<String> columns = new HashSet<>();
            while (set.next()) {
                columns.add(set.getString("NAME"));
            }

            //figuring which columns need to be created
            Set<String> toCreateColumns = new HashSet<>();
            for (Stat stat : stats) {
                if (!stat.isPersistent())
                    continue;

                if (columns.contains(stat.getInternalName()))
                    continue;

                toCreateColumns.add(stat.getInternalName());
            }

            //creating those columns
            for (String column : toCreateColumns) {
                statement.addBatch(StringUtils.replace(createColumns, "%column%", column));
            }

            if (toCreateColumns.size() != 0)
                statement.executeBatch();

            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void createTables() {
        String createDataTable = "CREATE TABLE IF NOT EXISTS " + getDataTableName() + " (UUID VARCHAR(36) NOT NULL PRIMARY KEY);";
        String createNameUUIDTable = "CREATE TABLE IF NOT EXISTS " + getPlayersTableName() + " (UUID VARCHAR(36) NOT NULL UNIQUE, NAME VARCHAR(16) NOT NULL UNIQUE);";
        try {
            Connection connection = database.getConnection();
            Statement statement = connection.createStatement();
            statement.executeUpdate(createDataTable);
            statement.executeUpdate(createNameUUIDTable);
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void saveNameAndUUID(String name, UUID uuid) {
        String sql = "REPLACE INTO " + getPlayersTableName() + " (UUID,NAME) VALUES (?, ?)";
        try {
            Connection connection = database.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, uuid.toString());
            statement.setString(2, name);
            statement.executeUpdate();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void finishUp() {
        this.database.close();
    }

    @Override
    public void saveStatsMultiple(Map<UUID, Map<Stat, Double>> statsMap) {
        if (statsMap.size() == 0)
            return;
        try {
            Connection connection = database.getConnection();
            Statement statement = connection.createStatement();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void forceSetStatsMultiple(Map<UUID, Map<Stat, Double>> statsMap) {

    }

    @Override
    public Map<UUID, Map<Stat, Double>> getStatsFromUUIDMultiple(Collection<UUID> uuids, List<Stat> stats) {
        return null;
    }

    @Override
    public Map<UUID, Map<Stat, Double>> getStatsFromNameMultiple(Collection<String> names, List<Stat> stats) {
        return null;
    }

    /*private String getSaveStatsQuery(List<Stat> stats) {
        StringBuilder updateString = new StringBuilder();
        for (int i = 0; i < stats.size(); i++) {
            Stat stat = stats.get(i);
            if (stat instanceof DependantStat || stat instanceof OfflineExternalStat || stat instanceof OnlineExternalStat)
                updateString.append(stat.getInternalName()).append("=? ");
            else
                updateString.append(stat.getInternalName()).append("=").append(stat.getInternalName()).append("+? ");
            if (i != stats.size() - 1)
                updateString.append(",");
        }
        if (updateString.toString().equals(""))
            return null;
        return "UPDATE " + getDataTableName() + " SET " + updateString.toString() + " WHERE UUID = ?";
    }*/
}
