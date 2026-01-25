package com.joogiebear.hytalevault.gui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.joogiebear.hytalevault.HytaleVaultPlugin;
import com.joogiebear.hytalevault.util.MessageUtil;

import javax.annotation.Nonnull;
import java.util.logging.Logger;

/**
 * Admin panel UI with form inputs for managing player vaults.
 * Provides Give, Set, Clear, and Reload actions via interactive buttons.
 */
public class AdminPanelPage extends InteractiveCustomUIPage<AdminPanelPage.AdminEventData> {

    private static final Logger LOGGER = Logger.getLogger("HytaleVault");
    private final HytaleVaultPlugin plugin;

    /**
     * Event data from the admin panel form.
     * Captures the action, player name input, and vault count input.
     */
    public static class AdminEventData {
        public String action;
        public String playerName;
        public int vaultCount;

        public static final BuilderCodec<AdminEventData> CODEC = BuilderCodec
                .builder(AdminEventData.class, AdminEventData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING),
                        (AdminEventData o, String v) -> o.action = v,
                        (AdminEventData o) -> o.action)
                .add()
                .append(new KeyedCodec<>("@PlayerName", Codec.STRING),
                        (AdminEventData o, String v) -> o.playerName = v,
                        (AdminEventData o) -> o.playerName)
                .add()
                .append(new KeyedCodec<>("@VaultCount", Codec.INTEGER),
                        (AdminEventData o, Integer v) -> o.vaultCount = v != null ? v : 1,
                        (AdminEventData o) -> o.vaultCount)
                .add()
                .build();
    }

    public AdminPanelPage(@Nonnull PlayerRef playerRef, HytaleVaultPlugin plugin) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, AdminEventData.CODEC);
        this.plugin = plugin;
    }

    @Override
    public void build(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull UICommandBuilder cmd,
            @Nonnull UIEventBuilder evt,
            @Nonnull Store<EntityStore> store
    ) {
        cmd.append("Pages/AdminPanel.ui");

        // Bind action buttons with form input values
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#GiveButton",
                new EventData().append("Action", "Give")
                        .append("@PlayerName", "#PlayerInput.Value")
                        .append("@VaultCount", "#VaultsInput.Value"));

        evt.addEventBinding(CustomUIEventBindingType.Activating, "#SetButton",
                new EventData().append("Action", "Set")
                        .append("@PlayerName", "#PlayerInput.Value")
                        .append("@VaultCount", "#VaultsInput.Value"));

        evt.addEventBinding(CustomUIEventBindingType.Activating, "#ClearButton",
                new EventData().append("Action", "Clear")
                        .append("@PlayerName", "#PlayerInput.Value"));

        evt.addEventBinding(CustomUIEventBindingType.Activating, "#ReloadButton",
                new EventData().append("Action", "Reload"));

        evt.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton",
                new EventData().append("Action", "Close"));
    }

    @Override
    public void handleDataEvent(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull AdminEventData data
    ) {
        Player player = store.getComponent(ref, Player.getComponentType());

        switch (data.action) {
            case "Close" -> {
                player.getPageManager().setPage(ref, store, Page.None);
            }
            case "Reload" -> {
                plugin.reload();
                playerRef.sendMessage(MessageUtil.success("Configuration reloaded."));
                player.getPageManager().setPage(ref, store, Page.None);
            }
            case "Give" -> {
                handleGive(player, ref, store, data);
            }
            case "Set" -> {
                handleSet(player, ref, store, data);
            }
            case "Clear" -> {
                handleClear(player, ref, store, data);
            }
        }
    }

    private void handleGive(Player player, Ref<EntityStore> ref, Store<EntityStore> store, AdminEventData data) {
        if (data.playerName == null || data.playerName.isBlank()) {
            playerRef.sendMessage(MessageUtil.error("Enter a player name."));
            return;
        }

        if (data.vaultCount < 1) {
            playerRef.sendMessage(MessageUtil.error("Vault count must be at least 1."));
            return;
        }

        PlayerRef targetRef = findPlayer(player, data.playerName);
        if (targetRef == null) {
            playerRef.sendMessage(MessageUtil.error("Player not found: " + data.playerName));
            return;
        }

        player.getPageManager().setPage(ref, store, Page.None);
        plugin.getVaultManager().grantVaults(targetRef.getUuid(), data.vaultCount).thenAccept(newTotal -> {
            playerRef.sendMessage(MessageUtil.success("Granted " + data.vaultCount + " vaults to " + targetRef.getUsername()));
        });
    }

    private void handleSet(Player player, Ref<EntityStore> ref, Store<EntityStore> store, AdminEventData data) {
        if (data.playerName == null || data.playerName.isBlank()) {
            playerRef.sendMessage(MessageUtil.error("Enter a player name."));
            return;
        }

        if (data.vaultCount < 1 || data.vaultCount > plugin.getConfigManager().getMaxVaults()) {
            playerRef.sendMessage(MessageUtil.error("Vaults must be between 1 and " + plugin.getConfigManager().getMaxVaults()));
            return;
        }

        PlayerRef targetRef = findPlayer(player, data.playerName);
        if (targetRef == null) {
            playerRef.sendMessage(MessageUtil.error("Player not found: " + data.playerName));
            return;
        }

        player.getPageManager().setPage(ref, store, Page.None);
        plugin.getVaultManager().setVaults(targetRef.getUuid(), data.vaultCount).thenRun(() -> {
            playerRef.sendMessage(MessageUtil.success("Set " + targetRef.getUsername() + "'s vaults to " + data.vaultCount));
        });
    }

    private void handleClear(Player player, Ref<EntityStore> ref, Store<EntityStore> store, AdminEventData data) {
        if (data.playerName == null || data.playerName.isBlank()) {
            playerRef.sendMessage(MessageUtil.error("Enter a player name."));
            return;
        }

        PlayerRef targetRef = findPlayer(player, data.playerName);
        if (targetRef == null) {
            playerRef.sendMessage(MessageUtil.error("Player not found: " + data.playerName));
            return;
        }

        // Open confirmation dialog instead of clearing immediately
        player.getPageManager().setPage(ref, store, Page.None);
        ConfirmClearPage confirmPage = new ConfirmClearPage(
                playerRef, plugin, targetRef.getUsername(), targetRef.getUuid()
        );
        player.getPageManager().openCustomPage(ref, store, confirmPage);
    }

    private PlayerRef findPlayer(Player adminPlayer, String name) {
        var world = adminPlayer.getWorld();
        if (world == null) return null;

        for (PlayerRef ref : world.getPlayerRefs()) {
            if (ref.getUsername().equalsIgnoreCase(name)) {
                return ref;
            }
        }
        return null;
    }
}
