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
import com.joogiebear.hytalevault.data.PlayerVault;
import com.joogiebear.hytalevault.util.MessageUtil;

import javax.annotation.Nonnull;

/**
 * Custom UI page that displays a vault selector grid.
 * Shows unlocked vaults as interactive blue buttons and locked vaults as grayed out.
 * Clicking an unlocked vault opens that vault's container window.
 */
public class VaultSelectorPage extends InteractiveCustomUIPage<VaultSelectorPage.VaultSelectData> {

    private final PlayerVault playerVault;
    private final VaultUI vaultUI;
    private final int maxVaults;

    /**
     * Event data received when a button is clicked.
     * Contains the action string: "1"-"9" for vault buttons, "Close" for close button.
     */
    public static class VaultSelectData {
        public String action;

        public static final BuilderCodec<VaultSelectData> CODEC = BuilderCodec
                .builder(VaultSelectData.class, VaultSelectData::new)
                .append(
                        new KeyedCodec<>("Action", Codec.STRING),
                        (VaultSelectData o, String v) -> o.action = v,
                        (VaultSelectData o) -> o.action
                )
                .add()
                .build();
    }

    public VaultSelectorPage(@Nonnull PlayerRef playerRef, PlayerVault playerVault,
                             VaultUI vaultUI, int maxVaults) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, VaultSelectData.CODEC);
        this.playerVault = playerVault;
        this.vaultUI = vaultUI;
        this.maxVaults = maxVaults;
    }

    @Override
    public void build(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull UICommandBuilder cmd,
            @Nonnull UIEventBuilder evt,
            @Nonnull Store<EntityStore> store
    ) {
        cmd.append("Pages/VaultSelector.ui");

        // Set button text and bind events for each vault slot
        for (int i = 1; i <= 9; i++) {
            String buttonId = "#Vault" + i;

            if (i <= maxVaults && playerVault.isVaultUnlocked(i)) {
                cmd.set(buttonId + ".Text", "Vault " + i);
            } else {
                cmd.set(buttonId + ".Text", "Locked");
            }

            // Bind click event for all buttons (locked ones show error in handleDataEvent)
            evt.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    buttonId,
                    new EventData().append("Action", String.valueOf(i))
            );
        }

        // Bind close button
        evt.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#CloseButton",
                new EventData().append("Action", "Close")
        );
    }

    @Override
    public void handleDataEvent(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull VaultSelectData data
    ) {
        Player player = store.getComponent(ref, Player.getComponentType());

        if ("Close".equals(data.action)) {
            player.getPageManager().setPage(ref, store, Page.None);
            return;
        }

        // Parse vault number
        int vaultNumber;
        try {
            vaultNumber = Integer.parseInt(data.action);
        } catch (NumberFormatException e) {
            player.getPageManager().setPage(ref, store, Page.None);
            return;
        }

        // Close the selector first
        player.getPageManager().setPage(ref, store, Page.None);

        // Open the vault (openVault handles unlock validation and error messages)
        vaultUI.openVault(player, ref, store, playerRef, playerVault, vaultNumber);
    }
}
