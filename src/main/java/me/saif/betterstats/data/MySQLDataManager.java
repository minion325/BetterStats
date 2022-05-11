package me.saif.betterstats.data;

import me.saif.betterstats.BetterStats;
import me.saif.betterstats.data.database.Database;
import me.saif.betterstats.data.database.MySQLDatabase;
import me.saif.betterstats.player.StatPlayer;
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
    public void registerStatistics(Stat... stats) {
        String getColumns = "SELECT COLUMN_NAME FROM information_schema.COLUMNS where TABLE_SCHEMA = '" + database.getDatabaseName() + "' AND TABLE_NAME = '" + getDataTableName() + "'";
        try (Connection connection = database.getConnection();
             Statement statement = connection.createStatement()) {

            String createColumns = "ALTER TABLE " + getDataTableName() + " ADD %column% REAL;";
            String setDefaults = "ALTER TABLE " + getDataTableName() + " ALTER COLUMN %column% SET DEFAULT %def%;";
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
            statement.clearBatch();

            //setting the default value of all registered stats in the database just incase
            for (Stat stat : stats) {
                if (!stat.isPersistent())
                    continue;
                String stmt = StringUtils.replace(StringUtils.replace(setDefaults, "%column%", stat.getInternalName()), "%def%", String.valueOf(stat.getDefaultValue()));
                statement.addBatch(stmt);
            }
            statement.executeBatch();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void createTables() {
        String createDataTable = "CREATE TABLE IF NOT EXISTS " + getDataTableName() + " (UUID VARCHAR(36) NOT NULL PRIMARY KEY);";
        String createNameUUIDTable = "CREATE TABLE IF NOT EXISTS " + getPlayersTableName() + " (UUID VARCHAR(36) NOT NULL UNIQUE, NAME VARCHAR(16) NOT NULL UNIQUE);";
        try (Connection connection = database.getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate(createDataTable);
            statement.executeUpdate(createNameUUIDTable);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void saveNameAndUUID(String name, UUID uuid) {
        String sql = "REPLACE INTO " + getPlayersTableName() + " (UUID,NAME) VALUES (?, ?)";
        try (Connection connection = database.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
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
    public void saveStatistics(StatPlayer statPlayer, List<Stat> stats) {
        this.saveStatistics(Collections.singleton(statPlayer), stats);
    }

    @Override
    public void saveStatistics(Set<StatPlayer> statPlayers, List<Stat> stats) {
        stats = stats.stream().filter(Stat::isPersistent).collect(Collectors.toList());

        if (statPlayers.size() == 0 || stats.size() == 0)
            return;

        String sql = getSaveSQLStatement(stats);

        try (Connection connection = this.database.getConnection(); PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            for (StatPlayer statPlayer : statPlayers) {
                preparedStatement.setString(1, statPlayer.getUuid().toString());
                int i = 2;
                while (i < stats.size() + 2) {
                    preparedStatement.setDouble(i, statPlayer.getStat(stats.get(i - 2)));
                    i++;
                }
                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Map<UUID, Map<Stat, Double>> getStatPlayersDataByUUIDs(Set<UUID> uuids, List<Stat> stats) {
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
        try (Connection connection = database.getConnection(); Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(sql.toString());

            while (resultSet.next()) {
                Map<Stat, Double> statMap = new HashMap<>();
                UUID uuid = UUID.fromString(resultSet.getString("UUID"));
                for (Stat stat : stats) {
                    statMap.put(stat, resultSet.getDouble(stat.getInternalName()));
                }
                map.put(uuid, statMap);
            }

            return map;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Map<Stat, Double> getStatPlayerDataByUUID(UUID uuid, List<Stat> stats) {
        Map<UUID, Map<Stat, Double>> dataMap = getStatPlayersDataByUUIDs(Collections.singleton(uuid), stats);
        if (dataMap == null)
            return null;
        return dataMap.get(uuid);
    }

    @Override
    public Map<UUID, Map<Stat, Double>> getStatPlayersDataByNames(Set<String> names, List<Stat> stats) {
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
        try (Connection connection = database.getConnection(); Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(sql.toString());

            while (resultSet.next()) {
                Map<Stat, Double> statMap = new HashMap<>();
                UUID uuid = UUID.fromString(resultSet.getString("UUID"));
                for (Stat stat : stats) {
                    statMap.put(stat, resultSet.getDouble(stat.getInternalName()));
                }
                map.put(uuid, statMap);
            }

            return map;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Pair<UUID, Map<Stat, Double>> getStatPlayerDataByName(String name, List<Stat> stats) {
        Map<UUID, Map<Stat, Double>> dataMap = getStatPlayersDataByNames(Collections.singleton(name), stats);
        if (dataMap == null || dataMap.size() <= 0)
            return null;

        for (UUID uuid : dataMap.keySet()) {
            return new Pair<>(uuid, dataMap.get(uuid));
        }

        return null;
    }

    private String getSaveSQLStatement(List<Stat> stats) {
        stats = stats.stream().filter(Stat::isPersistent).collect(Collectors.toList());
        if (stats.size() == 0)
            return "";
        StringBuilder columns = new StringBuilder("(UUID,");
        StringBuilder values = new StringBuilder("(?,");
        for (int i = 0; i < stats.size(); i++) {
            columns.append(stats.get(i).getInternalName());
            values.append("?");
            if (i == stats.size() - 1) {
                columns.append(")");
                values.append(")");
            } else {
                columns.append(",");
                values.append(",");
            }
        }
        return "REPLACE INTO " + getDataTableName() + " " + columns + " VALUES " + values + ";";
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
}
