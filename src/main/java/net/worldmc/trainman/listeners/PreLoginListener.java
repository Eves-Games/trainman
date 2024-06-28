package net.worldmc.trainman.listeners;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.worldmc.trainman.Trainman;
import net.worldmc.trainman.database.DatabaseManager;
import org.slf4j.Logger;

import java.util.Optional;

public class PreLoginListener {
    private final Logger logger;
    private final DatabaseManager databaseManager;
    private final Trainman plugin;

    public PreLoginListener(Logger logger, DatabaseManager databaseManager, Trainman plugin) {
        this.logger = logger;
        this.databaseManager = databaseManager;
        this.plugin = plugin;
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onServerPreConnect(ServerPreConnectEvent event) {
        Player player = event.getPlayer();
        boolean tutorialStatus = databaseManager.getPlayer(player.getUniqueId());

        if (tutorialStatus) return;

        Optional<RegisteredServer> tutorialServer = plugin.getProxyServer().getServer("Tutorial");
        if (tutorialServer.isPresent()) {
            event.setResult(ServerPreConnectEvent.ServerResult.allowed(tutorialServer.get()));
            logger.info("Player " + player.getUsername() + " redirected to tutorial server");
        } else {
            logger.warn("Tutorial server not found. Player " + player.getUsername() + " not redirected");
        }
    }
}