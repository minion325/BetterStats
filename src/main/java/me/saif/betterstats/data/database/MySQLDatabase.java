package me.saif.betterstats.data.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.saif.betterstats.utils.Verify;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;

public class MySQLDatabase extends Database {

    private final HikariDataSource source;
    private final String database;

    public MySQLDatabase(String host, int port, String user, String password, String database) {
        if (!Verify.isNotNull(host, password, user,password, database))
            throw new NullPointerException("Check database info in config.yml. Something is not set properly");
        Properties props = new Properties();
        props.setProperty("dataSource.serverName", host);
        props.setProperty("dataSource.portNumber", String.valueOf(port));
        props.setProperty("dataSource.user", user);
        props.setProperty("dataSource.password", password);
        props.setProperty("dataSource.databaseName", this.database = database);

        HikariConfig config = new HikariConfig(props);
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);

        this.source = new HikariDataSource(config);
        Logger.getGlobal().info("Connection to database established: " + this.source.getPoolName());
    }

    @Override
    public Connection getConnection() throws SQLException {
        return this.source.getConnection();
    }

    @Override
    public void close() {
        this.source.close();
    }

    @Override
    public String getDatabaseName() {
        return this.database;
    }
}
