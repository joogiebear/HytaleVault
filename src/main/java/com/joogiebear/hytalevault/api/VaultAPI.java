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

    public CompletableFuture<Integer> getUnlockedVaults(UUID playerUuid) {
        return getVault(playerUuid).thenApply(PlayerVault::getUnlockedVaults);
    }

    public CompletableFuture<Integer> grantVaults(UUID playerUuid, int count) {
        return HytaleVaultPlugin.getInstance().getVaultManager().grantVaults(playerUuid, count);
    }

    public CompletableFuture<ItemStack> getItem(UUID playerUuid, int vaultNumber, int slot) {
        return getVault(playerUuid).thenApply(vault -> {
            VaultPage vaultData = vault.getVault(vaultNumber);
            return vaultData != null ? vaultData.getItem(slot) : null;
        });
    }

    public CompletableFuture<Boolean> setItem(UUID playerUuid, int vaultNumber, int slot, ItemStack item) {
        return getVault(playerUuid).thenApply(vault -> {
            VaultPage vaultData = vault.getVault(vaultNumber);
            if (vaultData == null) return false;
            vaultData.setItem(slot, item);
            vault.markDirty();
            return true;
        });
    }

    public CompletableFuture<Boolean> isVaultUnlocked(UUID playerUuid, int vaultNumber) {
        return getVault(playerUuid).thenApply(vault -> vault.isVaultUnlocked(vaultNumber));
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

    public int getSlotsPerVault() {
        return HytaleVaultPlugin.getInstance().getConfigManager().getSlotsPerVault();
    }
}
