package com.joogiebear.hytalevault.listeners;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.joogiebear.hytalevault.HytaleVaultPlugin;
import com.joogiebear.hytalevault.gui.VaultUI;

import java.util.logging.Logger;

/**
 * Listener for player connection events.
 */
public class PlayerListener {

    private static final Logger LOGGER = Logger.getLogger("HytaleVault");

    private final HytaleVaultPlugin plugin;

    public PlayerListener(HytaleVaultPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Register event listeners with the Hytale event system.
     * Must be called during plugin setup() phase.
     */
    public void register() {
        // PlayerReadyEvent uses String key type - use registerGlobal to receive all events
        plugin.getEventRegistry().registerGlobal(PlayerReadyEvent.class, this::onPlayerReady);

        // PlayerDisconnectEvent uses Void key type - use register
        plugin.getEventRegistry().register(PlayerDisconnectEvent.class, this::onPlayerDisconnect);

        LOGGER.info("Registered player event listeners");
    }

    /**
     * Handle player ready event.
     * Called when a player has finished loading and is ready for gameplay.
     */
    private void onPlayerReady(PlayerReadyEvent event) {
        Player player = event.getPlayer();

        plugin.getVaultManager().getVault(player)
                .thenAccept(vault -> {
                    LOGGER.fine("Loaded vault for " + player.getLegacyDisplayName());
                })
                .exceptionally(e -> {
                    LOGGER.warning("Failed to load vault for " + player.getLegacyDisplayName() + ": " + e.getMessage());
                    return null;
                });
    }

    /**
     * Handle player disconnect event.
     * Called when a player disconnects from the server.
     * Note: PlayerDisconnectEvent provides PlayerRef, not Player directly.
     */
    private void onPlayerDisconnect(PlayerDisconnectEvent event) {
        PlayerRef playerRef = event.getPlayerRef();

        // Close vault window if open (syncs container to vault data)
        VaultUI vaultUI = plugin.getVaultUI();
        if (vaultUI != null && vaultUI.isVaultOpen(playerRef.getUuid())) {
            vaultUI.closeVaultByUuid(playerRef.getUuid());
        }

        plugin.getVaultManager().unloadVault(playerRef.getUuid())
                .thenRun(() -> {
                    LOGGER.fine("Saved and unloaded vault for " + playerRef.getUsername());
                })
                .exceptionally(e -> {
                    LOGGER.warning("Failed to save vault for " + playerRef.getUsername() + ": " + e.getMessage());
                    return null;
                });
    }
}
