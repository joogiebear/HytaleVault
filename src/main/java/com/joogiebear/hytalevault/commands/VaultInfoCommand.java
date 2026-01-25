package com.joogiebear.hytalevault.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.joogiebear.hytalevault.HytaleVaultPlugin;
import com.joogiebear.hytalevault.gui.VaultInfoPage;
import com.joogiebear.hytalevault.managers.ConfigManager;
import com.joogiebear.hytalevault.util.MessageUtil;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Command to open the vault info panel showing player's vault statistics.
 * Usage: /vaultinfo
 */
public class VaultInfoCommand extends AbstractCommand {

    private static final Logger LOGGER = Logger.getLogger("HytaleVault");
    private final HytaleVaultPlugin plugin;

    public VaultInfoCommand(HytaleVaultPlugin plugin) {
        super("vaultinfo", "View your vault statistics");
        this.plugin = plugin;
    }

    @Override
    public boolean hasPermission(CommandSender sender) {
        return true;
    }

    @Override
    protected CompletableFuture<Void> execute(CommandContext ctx) {
        if (!(ctx.sender() instanceof Player player)) {
            ctx.sendMessage(MessageUtil.error("This command can only be used by players."));
            return CompletableFuture.completedFuture(null);
        }

        World world = player.getWorld();
        if (world == null) {
            ctx.sendMessage(MessageUtil.error("Failed to open vault info. Please try again."));
            return CompletableFuture.completedFuture(null);
        }

        ConfigManager config = plugin.getConfigManager();

        return plugin.getVaultManager().getVault(player).thenAcceptAsync(vault -> {
            Ref<EntityStore> ref = player.getReference();
            Store<EntityStore> store = ref.getStore();
            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

            if (playerRef == null) return;

            VaultInfoPage infoPage = new VaultInfoPage(
                    playerRef, vault, config.getMaxVaults(), config.getSlotsPerVault()
            );
            player.getPageManager().openCustomPage(ref, store, infoPage);
        }, world).exceptionally(e -> {
            ctx.sendMessage(MessageUtil.error("Failed to open vault info. Please try again."));
            LOGGER.warning("Failed to open vault info for " + player.getDisplayName() + ": " + e.getMessage());
            return null;
        });
    }
}
