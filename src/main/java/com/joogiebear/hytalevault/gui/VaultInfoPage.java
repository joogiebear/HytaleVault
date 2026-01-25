package com.joogiebear.hytalevault.gui;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.joogiebear.hytalevault.data.PlayerVault;

import javax.annotation.Nonnull;

/**
 * Displays vault statistics for the player in a styled info panel.
 */
public class VaultInfoPage extends InteractiveCustomUIPage<VaultInfoPage.InfoCloseData> {

    private final PlayerVault playerVault;
    private final int maxVaults;
    private final int slotsPerVault;

    public static class InfoCloseData {
        public static final BuilderCodec<InfoCloseData> CODEC =
                BuilderCodec.builder(InfoCloseData.class, InfoCloseData::new).build();
    }

    public VaultInfoPage(@Nonnull PlayerRef playerRef, PlayerVault playerVault,
                         int maxVaults, int slotsPerVault) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, InfoCloseData.CODEC);
        this.playerVault = playerVault;
        this.maxVaults = maxVaults;
        this.slotsPerVault = slotsPerVault;
    }

    @Override
    public void build(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull UICommandBuilder cmd,
            @Nonnull UIEventBuilder evt,
            @Nonnull Store<EntityStore> store
    ) {
        cmd.append("Pages/VaultInfo.ui");

        int unlocked = playerVault.getUnlockedVaults();
        int totalItems = playerVault.getTotalItemCount();
        int totalSlots = unlocked * slotsPerVault;

        cmd.set("#UnlockedValue.Text", String.valueOf(unlocked));
        cmd.set("#TotalItemsValue.Text", String.valueOf(totalItems));
        cmd.set("#MaxVaultsValue.Text", String.valueOf(maxVaults));
        cmd.set("#SlotsInfo.Text", totalItems + " / " + totalSlots + " total slots used");

        evt.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton");
    }

    @Override
    public void handleDataEvent(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull InfoCloseData data
    ) {
        Player player = store.getComponent(ref, Player.getComponentType());
        player.getPageManager().setPage(ref, store, Page.None);
    }
}
