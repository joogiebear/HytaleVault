package com.joogiebear.hytalevault.managers;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.joogiebear.hytalevault.HytaleVaultPlugin;
import com.joogiebear.hytalevault.data.PlayerVault;
import com.joogiebear.hytalevault.data.storage.StorageBackend;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Manages player vault operations including loading, saving, and caching.
 * Vault access is controlled by permissions, not unlock counts.
 */
public class VaultManager {

    private static final Logger LOGGER = Logger.getLogger("HytaleVault");

    private final HytaleVaultPlugin plugin;
    private final StorageBackend storage;
    private final Map<UUID, PlayerVault> vaultCache;

    public VaultManager(HytaleVaultPlugin plugin, StorageBackend storage) {
        this.plugin = plugin;
        this.storage = storage;
        this.vaultCache = new ConcurrentHashMap<>();
    }

    public CompletableFuture<PlayerVault> getVault(Player player) {
        return getVault(player.getUuid());
    }

    public CompletableFuture<PlayerVault> getVault(UUID playerUuid) {
        PlayerVault cached = vaultCache.get(playerUuid);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        ConfigManager config = plugin.getConfigManager();
        return storage.loadVault(playerUuid, config.getSlotsPerVault())
                .thenApply(vault -> {
                    vaultCache.put(playerUuid, vault);
                    return vault;
                });
    }

    public PlayerVault getCachedVault(UUID playerUuid) {
        return vaultCache.get(playerUuid);
    }

    public boolean isVaultCached(UUID playerUuid) {
        return vaultCache.containsKey(playerUuid);
    }

    public CompletableFuture<Void> saveVault(UUID playerUuid) {
        PlayerVault vault = vaultCache.get(playerUuid);
        if (vault == null || !vault.isDirty()) {
            return CompletableFuture.completedFuture(null);
        }
        return storage.saveVault(vault);
    }

    public CompletableFuture<Void> saveVault(PlayerVault vault) {
        if (vault == null || !vault.isDirty()) {
            return CompletableFuture.completedFuture(null);
        }
        return storage.saveVault(vault);
    }

    public CompletableFuture<Void> unloadVault(UUID playerUuid) {
        PlayerVault vault = vaultCache.get(playerUuid);
        if (vault == null) {
            return CompletableFuture.completedFuture(null);
        }
        return saveVault(vault).thenRun(() -> vaultCache.remove(playerUuid));
    }

    public void saveAll() {
        LOGGER.info("Saving all vaults...");
        for (PlayerVault vault : vaultCache.values()) {
            if (vault.isDirty()) {
                storage.saveVault(vault).join();
            }
        }
        LOGGER.info("All vaults saved.");
    }

    public CompletableFuture<Void> clearVault(UUID playerUuid) {
        return getVault(playerUuid).thenCompose(vault -> {
            vault.clearAll();
            return storage.saveVault(vault);
        });
    }

    public boolean hasVaultPermission(Player player, int vaultNumber) {
        // Vault 1 is accessible by default
        if (vaultNumber == 1) {
            return player.hasPermission("hytalevault.vault.1", true);
        }

        // Check wildcard first
        if (player.hasPermission("hytalevault.vault.*")) {
            return true;
        }

        // Tiered permissions: vault.5 grants access to vaults 1-5
        // So for vault N, check if player has permission for any vault >= N
        ConfigManager config = plugin.getConfigManager();
        for (int i = vaultNumber; i <= config.getMaxVaults(); i++) {
            if (player.hasPermission("hytalevault.vault." + i)) {
                return true;
            }
        }

        return false;
    }

    public int getMaxAccessibleVault(Player player) {
        ConfigManager config = plugin.getConfigManager();

        if (player.hasPermission("hytalevault.vault.*")) {
            return config.getMaxVaults();
        }

        // Find the highest vault permission (tiered system)
        for (int i = config.getMaxVaults(); i >= 2; i--) {
            if (player.hasPermission("hytalevault.vault." + i)) {
                return i;
            }
        }
        return 1;
    }

    public void shutdown() {
        saveAll();
        vaultCache.clear();
    }
}
