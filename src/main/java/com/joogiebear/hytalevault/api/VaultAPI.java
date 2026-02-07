package com.joogiebear.hytalevault.api;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.joogiebear.hytalevault.HytaleVaultPlugin;
import com.joogiebear.hytalevault.data.PlayerVault;
import com.joogiebear.hytalevault.data.VaultPage;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Public API for HytaleVault.
 *
 * Vault access is controlled by permissions (LuckPerms).
 * Use hasVaultPermission() to check if a player can access a vault.
 */
public class VaultAPI {

    private static VaultAPI instance;

    private VaultAPI() {}

    public static VaultAPI getInstance() {
        if (instance == null) {
            if (HytaleVaultPlugin.getInstance() == null) {
                throw new IllegalStateException("HytaleVault is not loaded!");
            }
            instance = new VaultAPI();
        }
        return instance;
    }

    public CompletableFuture<PlayerVault> getVault(Player player) {
        return HytaleVaultPlugin.getInstance().getVaultManager().getVault(player);
    }

    public CompletableFuture<PlayerVault> getVault(UUID playerUuid) {
        return HytaleVaultPlugin.getInstance().getVaultManager().getVault(playerUuid);
    }

    /**
     * Check if a player has permission to access a specific vault.
     * Permissions are tiered: hytalevault.vault.3 grants access to vaults 1-3.
     *
     * @param player      The player to check
     * @param vaultNumber The vault number (1-indexed)
     * @return true if the player has permission
     */
    public boolean hasVaultPermission(Player player, int vaultNumber) {
        return HytaleVaultPlugin.getInstance().getVaultManager().hasVaultPermission(player, vaultNumber);
    }

    /**
     * Get the maximum vault a player has access to based on permissions.
     *
     * @param player The player to check
     * @return The highest vault number the player can access
     */
    public int getMaxAccessibleVault(Player player) {
        return HytaleVaultPlugin.getInstance().getVaultManager().getMaxAccessibleVault(player);
    }

    public CompletableFuture<ItemStack> getItem(UUID playerUuid, int vaultNumber, int slot) {
        int slotsPerVault = getSlotsPerVault();
        return getVault(playerUuid).thenApply(vault -> {
            VaultPage vaultData = vault.getOrCreateVault(vaultNumber, slotsPerVault);
            return vaultData != null ? vaultData.getItem(slot) : null;
        });
    }

    public CompletableFuture<Boolean> setItem(UUID playerUuid, int vaultNumber, int slot, ItemStack item) {
        int slotsPerVault = getSlotsPerVault();
        return getVault(playerUuid).thenApply(vault -> {
            VaultPage vaultData = vault.getOrCreateVault(vaultNumber, slotsPerVault);
            if (vaultData == null) return false;
            vaultData.setItem(slot, item);
            vault.markDirty();
            return true;
        });
    }

    public CompletableFuture<Integer> getTotalItems(UUID playerUuid) {
        return getVault(playerUuid).thenApply(PlayerVault::getTotalItemCount);
    }

    public CompletableFuture<Void> saveVault(UUID playerUuid) {
        return HytaleVaultPlugin.getInstance().getVaultManager().saveVault(playerUuid);
    }

    public int getMaxVaults() {
        return HytaleVaultPlugin.getInstance().getConfigManager().getMaxVaults();
    }

    /**
     * Get the global maximum slots per vault (config value).
     * For the actual slots a specific player gets, use getSlotsForPlayer().
     */
    public int getSlotsPerVault() {
        return HytaleVaultPlugin.getInstance().getConfigManager().getSlotsPerVault();
    }

    /**
     * Get the number of slots a player has based on their slot tier permissions.
     * Returns the global slotsPerVault if no slot tiers are configured.
     */
    public int getSlotsForPlayer(Player player) {
        return HytaleVaultPlugin.getInstance().getVaultManager().getSlotsForPlayer(player);
    }

    /**
     * Check if an item is blacklisted from being stored in vaults.
     */
    public boolean isItemBlacklisted(String itemId) {
        return HytaleVaultPlugin.getInstance().getConfigManager().isBlacklisted(itemId);
    }
}
