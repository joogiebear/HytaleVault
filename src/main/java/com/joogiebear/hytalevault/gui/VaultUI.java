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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Manages the vault GUI for players using Hytale's Windows System.
 */
public class VaultUI {

    private static final Logger LOGGER = Logger.getLogger("HytaleVault");
    private static final Map<UUID, VaultSession> openSessions = new HashMap<>();

    private final HytaleVaultPlugin plugin;

    public VaultUI(HytaleVaultPlugin plugin) {
        this.plugin = plugin;
    }

    public void openVault(Player player, Ref<EntityStore> ref, Store<EntityStore> store,
                          PlayerRef playerRef, PlayerVault vault, int vaultNumber) {
        ConfigManager config = plugin.getConfigManager();

        // Check permission first
        if (!plugin.getVaultManager().hasVaultPermission(player, vaultNumber)) {
            playerRef.sendMessage(MessageUtil.error(config.getMessageNoPermissionRaw()));
            return;
        }

        if (vaultNumber < 1 || !vault.isVaultUnlocked(vaultNumber)) {
            playerRef.sendMessage(MessageUtil.error(config.getMessageVaultNotUnlockedRaw()));
            return;
        }

        VaultPage vaultData = vault.getVault(vaultNumber);
        if (vaultData == null) {
            playerRef.sendMessage(MessageUtil.error(config.getMessageInvalidVaultRaw()));
            return;
        }

        // Close any existing vault window first (sync items before opening new one)
        if (openSessions.containsKey(playerRef.getUuid())) {
            syncAndRemoveSession(playerRef.getUuid());
        }

        // Create container with all slots as storage
        int slotsPerVault = config.getSlotsPerVault();
        VaultContainer container = new VaultContainer((short) slotsPerVault);

        // Load items from vault into container
        Map<Integer, ItemStack> items = vaultData.getItems();
        for (Map.Entry<Integer, ItemStack> entry : items.entrySet()) {
            int slot = entry.getKey();
            ItemStack item = entry.getValue();
            if (slot >= 0 && slot < slotsPerVault && item != null && !item.isEmpty()) {
                container.internal_setSlot((short) slot, item);
            }
        }

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
                syncContainerToVault(session);
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
            syncContainerToVault(session);
            plugin.getVaultManager().saveVault(session.vault).join();
        }
    }

    /**
     * Sync the container slots back to the vault data.
     */
    private void syncContainerToVault(VaultSession session) {
        if (session.container == null || session.vault == null) return;

        VaultPage vaultData = session.vault.getVault(session.currentVault);
        if (vaultData == null) return;

        vaultData.clear();

        short capacity = session.container.getCapacity();
        for (short i = 0; i < capacity; i++) {
            ItemStack item = session.container.internal_getSlot(i);
            if (item != null && !item.isEmpty()) {
                vaultData.setItem(i, item);
            }
        }

        session.vault.markDirty();
        LOGGER.info("Synced container to vault #" + session.currentVault);
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
            syncContainerToVault(session);
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
