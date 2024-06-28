package net.worldmc.trainman;

import com.google.inject.Inject;
import com.moandjiezana.toml.Toml;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import net.worldmc.trainman.database.DatabaseManager;
import net.worldmc.trainman.listeners.PreLoginListener;
import org.slf4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

@Plugin(
        id = "trainman",
        name = "trainman",
        version = BuildConstants.VERSION,
        authors = "Ezekiel Pari"
)
public class Trainman {
    private final ProxyServer proxyServer;
    private final Logger logger;
    private final Path dataDirectory;
    private Toml config;
    private DatabaseManager databaseManager;

    @Inject
    public Trainman(ProxyServer proxyServer, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxyServer = proxyServer;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    private static final MinecraftChannelIdentifier TUTORIAL_CHANNEL = MinecraftChannelIdentifier.from("clives:tutorial");

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        loadConfig();
        proxyServer.getChannelRegistrar().register(TUTORIAL_CHANNEL);
        databaseManager = new DatabaseManager(logger, this);
        databaseManager.connectToDatabase();
        registerListeners();
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!(event.getSource() instanceof ServerConnection)) return;
        if (!event.getIdentifier().equals(TUTORIAL_CHANNEL)) return;

        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(event.getData()));
            String message = in.readUTF();

            if ("tutorial_done".equals(message) && event.getTarget() instanceof Player player) {
                databaseManager.addPlayer(player.getUniqueId());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadConfig() {
        if (!Files.exists(dataDirectory)) {
            try {
                Files.createDirectories(dataDirectory);
            } catch (IOException e) {
                logger.error("Failed to create data directory", e);
                return;
            }
        }

        File configFile = new File(dataDirectory.toFile(), "config.toml");

        if (!configFile.exists()) {
            try (InputStream in = getClass().getResourceAsStream("/config.toml")) {
                if (in == null) {
                    return;
                }
                Files.copy(in, configFile.toPath());
            } catch (IOException e) {
                logger.error("Failed to create default config file", e);
                return;
            }
        }

        config = new Toml().read(configFile);
    }

    private void registerListeners() {
        proxyServer.getEventManager().register(this, new PreLoginListener(logger, databaseManager, this));
    }

    public ProxyServer getProxyServer() {
        return proxyServer;
    }

    public String getConfigString(String key) {
        return config.getString(key);
    }

    public int getConfigInt(String key) {
        return config.getLong(key).intValue();
    }
}