package com.joogiebear.hytalevault.data.storage;

import com.joogiebear.hytalevault.data.PlayerVault;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for vault data storage backends.
 * Implementations handle persistence of player vault data.
 */
public interface StorageBackend {

    /**
     * Load a player's vault from storage.
     *
     * @param playerUuid    The player's UUID
     * @param defaultVaults Default number of vaults for new players
     * @param slotsPerVault Slots per vault
     * @return A future containing the loaded vault, or a new vault if none exists
     */
    CompletableFuture<PlayerVault> loadVault(UUID playerUuid, int defaultVaults, int slotsPerVault);

    /**
     * Save a player's vault to storage.
     *
     * @param vault The vault to save
     * @return A future that completes when the save is done
     */
    CompletableFuture<Void> saveVault(PlayerVault vault);

    /**
     * Delete a player's vault from storage.
     *
     * @param playerUuid The player's UUID
     * @return A future that completes when the deletion is done
     */
    CompletableFuture<Void> deleteVault(UUID playerUuid);

    /**
     * Check if a player has an existing vault in storage.
     *
     * @param playerUuid The player's UUID
     * @return A future containing true if the vault exists
     */
    CompletableFuture<Boolean> vaultExists(UUID playerUuid);

    /**
     * Initialize the storage backend.
     * Called when the plugin enables.
     */
    void initialize();

    /**
     * Shutdown the storage backend.
     * Called when the plugin disables.
     */
    void shutdown();
}
