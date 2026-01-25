package com.joogiebear.hytalevault.listeners;

import com.hypixel.hytale.server.core.entity.entities.Player;
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

    public void register() {
        // TODO: Register event listeners using Hytale's event system
        LOGGER.info("Registering player event listeners");
    }

    public void onPlayerJoin(Player player) {
        plugin.getVaultManager().getVault(player)
                .thenAccept(vault -> {
                    LOGGER.fine("Loaded vault for " + player.getLegacyDisplayName() +
                            " with " + vault.getUnlockedVaults() + " vaults");
                })
                .exceptionally(e -> {
                    LOGGER.warning("Failed to load vault for " + player.getLegacyDisplayName() + ": " + e.getMessage());
                    return null;
                });
    }

    public void onPlayerQuit(Player player) {
        // Close vault window if open (syncs container to vault data)
        VaultUI vaultUI = plugin.getVaultUI();
        if (vaultUI != null && vaultUI.isVaultOpen(player.getUuid())) {
            vaultUI.closeVault(player);
        }

        plugin.getVaultManager().unloadVault(player.getUuid())
                .thenRun(() -> {
                    LOGGER.fine("Saved and unloaded vault for " + player.getLegacyDisplayName());
                })
                .exceptionally(e -> {
                    LOGGER.warning("Failed to save vault for " + player.getLegacyDisplayName() + ": " + e.getMessage());
                    return null;
                });
    }
}
