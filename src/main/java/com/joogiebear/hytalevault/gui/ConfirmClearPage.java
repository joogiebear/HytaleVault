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
import java.util.UUID;

/**
 * Confirmation dialog shown before clearing a player's vaults.
 */
public class ConfirmClearPage extends InteractiveCustomUIPage<ConfirmClearPage.ConfirmData> {

    private final HytaleVaultPlugin plugin;
    private final String targetName;
    private final UUID targetUuid;

    public static class ConfirmData {
        public String action;

        public static final BuilderCodec<ConfirmData> CODEC = BuilderCodec
                .builder(ConfirmData.class, ConfirmData::new)
                .append(
                        new KeyedCodec<>("Action", Codec.STRING),
                        (ConfirmData o, String v) -> o.action = v,
                        (ConfirmData o) -> o.action
                )
                .add()
                .build();
    }

    public ConfirmClearPage(@Nonnull PlayerRef playerRef, HytaleVaultPlugin plugin,
                            String targetName, UUID targetUuid) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, ConfirmData.CODEC);
        this.plugin = plugin;
        this.targetName = targetName;
        this.targetUuid = targetUuid;
    }

    @Override
    public void build(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull UICommandBuilder cmd,
            @Nonnull UIEventBuilder evt,
            @Nonnull Store<EntityStore> store
    ) {
        cmd.append("Pages/ConfirmClear.ui");

        cmd.set("#Message.Text", "Clear ALL vault items for " + targetName + "? This cannot be undone.");

        evt.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#ConfirmButton",
                new EventData().append("Action", "Confirm")
        );
        evt.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#CancelButton",
                new EventData().append("Action", "Cancel")
        );
    }

    @Override
    public void handleDataEvent(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull ConfirmData data
    ) {
        Player player = store.getComponent(ref, Player.getComponentType());
        player.getPageManager().setPage(ref, store, Page.None);

        if ("Confirm".equals(data.action)) {
            plugin.getVaultManager().clearVault(targetUuid).thenRun(() -> {
                playerRef.sendMessage(MessageUtil.success("Vaults cleared for " + targetName));
            });
        }
    }
}
