package me.saif.betterstats.data;

import me.saif.betterstats.BetterStats;
import me.saif.betterstats.data.database.Database;
import me.saif.betterstats.data.database.MySQLDatabase;
import me.saif.betterstats.player.StatPlayerSnapshot;
import me.saif.betterstats.statistics.DependantStat;
import me.saif.betterstats.statistics.OfflineExternalStat;
import me.saif.betterstats.statistics.OnlineExternalStat;
import me.saif.betterstats.statistics.Stat;
import me.saif.betterstats.utils.Pair;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class MySQLDataManager extends DataManger {

    private Database database;

    public MySQLDataManager(BetterStats plugin, String server) {
        super(plugin, server);
        try {
            this.database = new MySQLDatabase(
                    plugin.getConfig().getString("sql.host"),
                    plugin.getConfig().getInt("sql.port"),
                    plugin.getConfig().getString("sql.username"),
                    plugin.getConfig().getString("sql.password"),
                    plugin.getConfig().getString("sql.database"));
        } catch (Exception e) {
            e.printStackTrace();
            plugin.getLogger().severe("Could not connect to the database properly. Please check the config and reload the plugin.");
            Bukkit.getPluginManager().disablePlugin(this.getPlugin());
        }
    }

    @Override
    public String getType() {
        return "MYSQL";
    }

    @Override
    public void registerStatistics(Stat... stats) {
        String getColumns = "SELECT COLUMN_NAME FROM information_schema.COLUMNS where TABLE_SCHEMA = '" + database.getDatabaseName() + "' AND TABLE_NAME = '" + getDataTableName() + "'";
        try (Connection connection = database.getConnection();
             Statement statement = connection.createStatement()) {

            String createColumns = "ALTER TABLE " + getDataTableName() + " ADD %column% REAL NOT NULL DEFAULT 0;";
            //getting the current columns in the table
            ResultSet set = statement.executeQuery(getColumns);
            Set<String> columns = new HashSet<>();
            while (set.next()) {
                columns.add(set.getString("COLUMN_NAME"));
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
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void createTables() {
        String createDataTable = "CREATE TABLE IF NOT EXISTS " + getDataTableName() + " (UUID VARCHAR(36) NOT NULL PRIMARY KEY);";
        String createNameUUIDTable = "CREATE TABLE IF NOT EXISTS " + getPlayersTableName() + " (UUID VARCHAR(36) NOT NULL UNIQUE, NAME VARCHAR(16) NOT NULL UNIQUE);";
        try (Connection connection = database.getConnection();
             Statement statement = connection.createStatement()) {

            statement.executeUpdate(createDataTable);
            statement.executeUpdate(createNameUUIDTable);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void saveNameAndUUID(String name, UUID uuid) {
        String sql = "REPLACE INTO " + getPlayersTableName() + " (UUID,NAME) VALUES (?, ?)";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, uuid.toString());
            statement.setString(2, name);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void finishUp() {
        this.database.close();
    }

    @Override
    public void forceSetStatsMultiple(Map<UUID, Map<Stat, Double>> statsMap) {
        if (statsMap.size() == 0) return;
        try (Connection connection = database.getConnection();
             Statement statement = connection.createStatement()) {

            for (UUID uuid : statsMap.keySet()) {
                Map<Stat, Double> statDoubleMap = statsMap.get(uuid);
                if (statDoubleMap.size() == 0) continue;
                StringBuilder columns = new StringBuilder("(UUID,");
                StringBuilder values = new StringBuilder("('" + uuid + "',");

                statDoubleMap.forEach((stat, aDouble) -> {
                    if (!stat.isPersistent()) return;
                    columns.append(stat.getInternalName());
                    values.append(aDouble);
                    columns.append(",");
                    values.append(",");
                });
                columns.deleteCharAt(columns.length() - 1);
                values.deleteCharAt(values.length() - 1);
                columns.append(')');
                values.append(')');
                statement.addBatch("REPLACE INTO " + getDataTableName() + " " + columns + " VALUES " + values + ";");
            }
            statement.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void saveStatChangesMultiple(Map<UUID, StatPlayerSnapshot> statsMap, List<Stat> stats) {
        stats = stats.stream().filter(Stat::isPersistent).collect(Collectors.toList());
        if (statsMap.size() == 0 || stats.size() == 0) return;
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(createSavingStatement(stats))) {

            for (UUID uuid : statsMap.keySet()) {
                for (int i = 1; i <= stats.size(); i++) {
                    Stat stat = stats.get(i - 1);
                    double val;
                    if (statsMap.get(uuid).getChanges(stat) == null) val = statsMap.get(uuid).get(stat);
                    else val = statsMap.get(uuid).getChanges(stat);
                    statement.setDouble(i, val);
                }
                statement.setString(stats.size() + 1, uuid.toString());
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String createSavingStatement(List<Stat> stats) {
        StringBuilder sql = new StringBuilder("");
        stats.forEach((stat) -> {
            if (!stat.isPersistent()) return;
            if (stat instanceof DependantStat || stat instanceof OfflineExternalStat || stat instanceof OnlineExternalStat)
                sql.append(stat.getInternalName()).append("=?");
            else sql.append(stat.getInternalName()).append("=").append(stat.getInternalName()).append("+?");
            sql.append(',');
        });
        if (sql.toString().equals("")) return "";
        sql.deleteCharAt(sql.length() - 1);

        return "UPDATE " + getDataTableName() + " SET " + sql + " WHERE UUID = ?;";
    }

    @Override
    public Map<UUID, Pair<String, Map<Stat, Double>>> getStatsFromUUIDMultiple(Collection<UUID> uuids, List<Stat> stats) {
        stats = stats.stream().filter(Stat::isPersistent).collect(Collectors.toList());
        StringBuilder sql = new StringBuilder("SELECT " + getColumns(stats) + "," + getPlayersTableName() + ".NAME FROM " + getDataTableName() + " LEFT JOIN " + getPlayersTableName() + " ON " + getDataTableName() + ".UUID=" + getPlayersTableName() + ".UUID WHERE ");
        Iterator<UUID> uuidIterator = uuids.iterator();

        while (uuidIterator.hasNext()) {
            UUID uuid = uuidIterator.next();

            sql.append(getDataTableName()).append(".UUID ='").append(uuid.toString()).append('\'');
            if (!uuidIterator.hasNext()) sql.append(";");
            else sql.append(" OR ");

        }

        Map<UUID, Pair<String, Map<Stat, Double>>> map = new HashMap<>();
        try (Connection connection = database.getConnection();
             Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(sql.toString());

            while (resultSet.next()) {
                Map<Stat, Double> statMap = new HashMap<>();
                UUID uuid = UUID.fromString(resultSet.getString("UUID"));
                String name = resultSet.getString("NAME");
                for (Stat stat : stats) {
                    statMap.put(stat, resultSet.getObject(stat.getInternalName()) != null ? resultSet.getDouble(stat.getInternalName()) : null);
                }
                map.put(uuid, new Pair<>(name, statMap));
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
            if (!nameIterator.hasNext()) sql.append(";");
            else sql.append(" OR ");

        }
        Map<UUID, Map<Stat, Double>> map = new HashMap<>();
        try (Connection connection = database.getConnection();
             Statement statement = connection.createStatement()) {

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
        StringBuilder columns = new StringBuilder(getDataTableName()).append(".UUID,");
        for (int i = 0; i < stats.size(); i++) {
            columns.append(stats.get(i).getInternalName());
            if (i == stats.size() - 1) return columns.toString();
            else columns.append(",");
        }
        return "*";
    }
}
