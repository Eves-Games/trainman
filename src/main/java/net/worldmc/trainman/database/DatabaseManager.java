package net.worldmc.trainman.database;

import net.worldmc.trainman.Trainman;
import org.slf4j.Logger;

import java.sql.*;
import java.util.UUID;

public class DatabaseManager {
    private final Logger logger;
    private final Trainman plugin;
    private Connection dbConnection;

    public DatabaseManager(Logger logger, Trainman plugin) {
        this.logger = logger;
        this.plugin = plugin;
    }

    public void connectToDatabase() {
        String host = plugin.getConfigString("database.host");
        int port = plugin.getConfigInt("database.port");
        String dbName = plugin.getConfigString("database.name");
        String username = plugin.getConfigString("database.username");
        String password = plugin.getConfigString("database.password");

        String url = String.format("jdbc:mysql://%s:%d/%s?useSSL=false", host, port, dbName);

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            dbConnection = DriverManager.getConnection(url, username, password);
            logger.info("Successfully connected to the database.");

            createPlayersTable();
        } catch (SQLException e) {
            logger.error("Failed to connect to the database", e);
        } catch (ClassNotFoundException e) {
            logger.error("MySQL JDBC Driver not found", e);
        }
    }

    public void createPlayersTable() {
        String sql = "CREATE TABLE IF NOT EXISTS players ("
                + "uuid VARCHAR(36) PRIMARY KEY)";
        try (Statement stmt = dbConnection.createStatement()) {
            stmt.execute(sql);
            logger.info("Players table created or already exists.");
        } catch (SQLException e) {
            logger.error("Error creating players table", e);
        }
    }

    public boolean getPlayer(UUID playerUUID) {
        String sql = "SELECT uuid FROM players WHERE uuid = ?";
        try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
            stmt.setString(1, playerUUID.toString());
            ResultSet rs = stmt.executeQuery();

            return rs.next();
        } catch (SQLException e) {
            logger.error("Error checking if player exists", e);
            return false;
        }
    }

    public void addPlayer(UUID playerUUID) {
        String sql = "INSERT IGNORE INTO players (uuid) VALUES (?)";
        try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
            stmt.setString(1, playerUUID.toString());
            stmt.executeUpdate();
            logger.info("Added new player " + playerUUID + " to the database");
        } catch (SQLException e) {
            logger.error("Error adding new player to database", e);
        }
    }
}