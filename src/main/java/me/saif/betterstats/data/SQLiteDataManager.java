package me.saif.betterstats.data;

import me.saif.betterstats.BetterStats;
import me.saif.betterstats.data.database.Database;
import me.saif.betterstats.data.database.SQLiteDatabase;
import me.saif.betterstats.statistics.*;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

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
    public void saveStatChangesMultiple(Map<UUID, Map<Stat, Double>> statsMap) {
        if (statsMap.size() == 0)
            return;
        try {
            Connection connection = database.getConnection();
            Statement statement = connection.createStatement();
            for (UUID uuid : statsMap.keySet()) {
                Map<Stat, Double> statDoubleMap = statsMap.get(uuid);
                if (statDoubleMap.size() == 0)
                    continue;
                StringBuilder columns = new StringBuilder("(UUID,");
                StringBuilder values = new StringBuilder("(?,");

                statDoubleMap.forEach((stat, aDouble) -> {
                    if (!stat.isPersistent())
                        return;
                    columns.append(stat.getInternalName());
                    values.append(aDouble);
                    columns.append(",");
                    values.append(",");
                });

                statement.addBatch("REPLACE INTO " + getDataTableName() + " " + columns + " VALUES " + values + ";");
            }
            statement.executeBatch();
        } catch (
                SQLException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void forceSetStatsMultiple(Map<UUID, Map<Stat, Double>> statsMap) {
        if (statsMap.size() == 0)
            return;
        try {
            Connection connection = database.getConnection();
            Statement statement = connection.createStatement();
            for (UUID uuid : statsMap.keySet()) {
                Map<Stat, Double> statDoubleMap = statsMap.get(uuid);
                StringBuilder sql = new StringBuilder("");
                statDoubleMap.forEach((stat, aDouble) -> {
                    if (!stat.isPersistent())
                        return;
                    if (stat instanceof DependantStat || stat instanceof OfflineExternalStat || stat instanceof OnlineExternalStat)
                        sql.append(stat.getInternalName()).append("=").append(aDouble);
                    else
                        sql.append(stat.getInternalName()).append("=").append(stat.getInternalName()).append("+").append(aDouble);
                    sql.append(',');
                });
                if (sql.toString().equals(""))
                    continue;
                sql.deleteCharAt(sql.length() - 1);
                statement.addBatch("UPDATE " + getDataTableName() + " SET " + sql +
                        " WHERE UUID = '" + uuid + "';");
            }
            statement.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Map<UUID, Map<Stat, Double>> getStatsFromUUIDMultiple(Collection<UUID> uuids, List<Stat> stats) {
        stats = stats.stream().filter(Stat::isPersistent).collect(Collectors.toList());
        StringBuilder sql = new StringBuilder("SELECT " + getColumns(stats) + " FROM " + getDataTableName() + " WHERE ");
        Iterator<UUID> uuidIterator = uuids.iterator();

        while (uuidIterator.hasNext()) {
            UUID uuid = uuidIterator.next();

            sql.append("UUID ='").append(uuid.toString()).append('\'');
            if (!uuidIterator.hasNext())
                sql.append(";");
            else
                sql.append(" OR ");

        }

        Map<UUID, Map<Stat, Double>> map = new HashMap<>();
        try {
            Connection connection = database.getConnection();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(sql.toString());

            while (resultSet.next()) {
                Map<Stat, Double> statMap = new HashMap<>();
                UUID uuid = UUID.fromString(resultSet.getString("UUID"));
                for (Stat stat : stats) {
                    statMap.put(stat, resultSet.getObject(stat.getInternalName()) != null ? resultSet.getDouble(stat.getInternalName()) : null);
                }
                map.put(uuid, statMap);
            }

            statement.close();
            return map;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Map<UUID, Map<Stat, Double>> getStatsFromNameMultiple(Collection<String> names, List<Stat> stats) {
        stats = stats.stream().filter(Stat::isPersistent).collect(Collectors.toList());
        StringBuilder sql = new StringBuilder("SELECT " + getColumns(stats) + " FROM " + getDataTableName() + " WHERE ");
        Iterator<String> nameIterator = names.iterator();

        while (nameIterator.hasNext()) {
            String name = nameIterator.next();

            sql.append("UUID =").append("(SELECT UUID FROM ").append(getPlayersTableName()).append(" WHERE NAME ='").append(name).append("' LIMIT 1)");
            if (!nameIterator.hasNext())
                sql.append(";");
            else
                sql.append(" OR ");

        }
        Map<UUID, Map<Stat, Double>> map = new HashMap<>();
        try {
            Connection connection = database.getConnection();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(sql.toString());

            while (resultSet.next()) {
                Map<Stat, Double> statMap = new HashMap<>();
                UUID uuid = UUID.fromString(resultSet.getString("UUID"));
                for (Stat stat : stats) {
                    statMap.put(stat, resultSet.getObject(stat.getInternalName()) != null ? resultSet.getDouble(stat.getInternalName()) : null);
                }
                map.put(uuid, statMap);
            }
            statement.close();
            return map;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getColumns(List<Stat> stats) {
        StringBuilder columns = new StringBuilder("UUID,");
        for (int i = 0; i < stats.size(); i++) {
            columns.append(stats.get(i).getInternalName());
            if (i == stats.size() - 1)
                return columns.toString();
            else
                columns.append(",");
        }
        return "*";
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
