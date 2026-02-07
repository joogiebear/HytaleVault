package com.joogiebear.hytalevault.gui;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.PageManager;
import com.hypixel.hytale.server.core.entity.entities.player.windows.ContainerWindow;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.joogiebear.hytalevault.HytaleVaultPlugin;
import com.joogiebear.hytalevault.data.PlayerVault;
import com.joogiebear.hytalevault.data.VaultPage;
import com.joogiebear.hytalevault.managers.ConfigManager;
import com.joogiebear.hytalevault.util.MessageUtil;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Manages the vault GUI for players using Hytale's Windows System.
 * Uses real-time syncing to ensure items are saved even if the menu is closed quickly.
 */
public class VaultUI {

    private static final Logger LOGGER = Logger.getLogger("HytaleVault");
    private static final Map<UUID, VaultSession> openSessions = new ConcurrentHashMap<>();

    private final HytaleVaultPlugin plugin;

    public VaultUI(HytaleVaultPlugin plugin) {
        this.plugin = plugin;
    }

    public void openVault(Player player, Ref<EntityStore> ref, Store<EntityStore> store,
                          PlayerRef playerRef, PlayerVault vault, int vaultNumber) {
        ConfigManager config = plugin.getConfigManager();

        // Permission-only check
        if (!plugin.getVaultManager().hasVaultPermission(player, vaultNumber)) {
            playerRef.sendMessage(MessageUtil.error(config.getMessageNoPermissionRaw()));
            return;
        }

        if (vaultNumber < 1 || vaultNumber > config.getMaxVaults()) {
            playerRef.sendMessage(MessageUtil.error(config.getMessageInvalidVaultRaw()));
            return;
        }

        // Get player's slot count based on permissions (or global default if no tiers)
        int slotsPerVault = plugin.getVaultManager().getSlotsForPlayer(player);
        // Always create/store vault with global max so items aren't lost on downgrades
        VaultPage vaultData = vault.getOrCreateVault(vaultNumber, config.getSlotsPerVault());
        if (vaultData == null) {
            playerRef.sendMessage(MessageUtil.error(config.getMessageInvalidVaultRaw()));
            return;
        }

        // Close any existing vault window first (sync items before opening new one)
        if (openSessions.containsKey(playerRef.getUuid())) {
            syncAndRemoveSession(playerRef.getUuid());
        }

        // Create container with player's permitted slot count
        VaultContainer container = new VaultContainer((short) slotsPerVault);
        container.setBlacklist(config.getBlacklistedItems());

        // Load items from vault into container (only within permitted slot range)
        Map<Integer, ItemStack> items = vaultData.getItems();
        for (Map.Entry<Integer, ItemStack> entry : items.entrySet()) {
            int slot = entry.getKey();
            ItemStack item = entry.getValue();
            if (slot >= 0 && slot < slotsPerVault && item != null && !item.isEmpty()) {
                container.internal_setSlot((short) slot, item);
            }
        }

        // Set up real-time sync listener AFTER loading initial items
        // Saves immediately to disk on every change to prevent data loss
        container.setChangeListener((slot, item) -> {
            // Sync this slot change directly to vault data
            if (item == null || item.isEmpty()) {
                vaultData.clearSlot(slot);
            } else {
                vaultData.setItem(slot, item);
            }
            vault.markDirty();
            // Save to disk immediately (async)
            plugin.getVaultManager().saveVault(vault);
        });

        // Wrap in ContainerWindow and open via PageManager
        ContainerWindow containerWindow = new ContainerWindow(container);

        VaultSession session = new VaultSession(playerRef.getUuid(), vault, vaultNumber, container, containerWindow);
        openSessions.put(playerRef.getUuid(), session);

        PageManager pageManager = player.getPageManager();
        pageManager.setPageWithWindows(ref, store, Page.Bench, true, containerWindow);

        String message = config.formatMessage(config.getMessageVaultOpenedRaw(), "vault", String.valueOf(vaultNumber));
        playerRef.sendMessage(MessageUtil.success(message));
        LOGGER.info("Opened vault #" + vaultNumber + " for " + player.getDisplayName());
    }

    public void closeVault(Player player) {
        Ref<EntityStore> ref = player.getReference();
        Store<EntityStore> store = ref.getStore();
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

        if (playerRef != null) {
            VaultSession session = openSessions.remove(playerRef.getUuid());
            if (session != null) {
                // Real-time sync already happened, just save to disk
                plugin.getVaultManager().saveVault(session.vault).join();
                LOGGER.info("Saved vault for " + playerRef.getUuid());
            }
        }
    }

    /**
     * Sync current session, save to disk, and remove it.
     */
    private void syncAndRemoveSession(UUID playerUuid) {
        VaultSession session = openSessions.remove(playerUuid);
        if (session != null) {
            // Real-time sync already happened, just save to disk
            plugin.getVaultManager().saveVault(session.vault).join();
        }
    }

    /**
     * Close a vault by player UUID (used when Player object is not available).
     */
    public void closeVaultByUuid(UUID playerUuid) {
        syncAndRemoveSession(playerUuid);
    }

    public boolean isVaultOpen(UUID playerUuid) {
        return openSessions.containsKey(playerUuid);
    }

    public VaultSession getSession(UUID playerUuid) {
        return openSessions.get(playerUuid);
    }

    public void closeAll() {
        // Save all open vaults before clearing
        for (VaultSession session : openSessions.values()) {
            plugin.getVaultManager().saveVault(session.vault).join();
        }
        openSessions.clear();
    }

    public static class VaultSession {
        public final UUID playerUuid;
        public final PlayerVault vault;
        public int currentVault;
        public final VaultContainer container;
        public final ContainerWindow window;

        public VaultSession(UUID playerUuid, PlayerVault vault, int currentVault,
                           VaultContainer container, ContainerWindow window) {
            this.playerUuid = playerUuid;
            this.vault = vault;
            this.currentVault = currentVault;
            this.container = container;
            this.window = window;
        }
    }
}
