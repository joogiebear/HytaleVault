package com.joogiebear.hytalevault.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.joogiebear.hytalevault.HytaleVaultPlugin;
import com.joogiebear.hytalevault.gui.AdminPanelPage;
import com.joogiebear.hytalevault.util.MessageUtil;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Admin command for vault management.
 * Usage: /vaultadmin <subcommand> [args]
 *
 * Note: Vault access is controlled by permissions (LuckPerms).
 * Use LuckPerms to grant vault access: lp user <player> permission set hytalevault.vault.<number>
 */
public class VaultAdminCommand extends AbstractCommand {

    private static final Logger LOGGER = Logger.getLogger("HytaleVault");

    private final HytaleVaultPlugin plugin;

    public VaultAdminCommand(HytaleVaultPlugin plugin) {
        super("vaultadmin", "Admin commands for vault management");
        this.plugin = plugin;

        // Set permission
        requirePermission("hytalevault.admin");

        // Add subcommands
        addSubCommand(new ClearSubCommand(plugin));
        addSubCommand(new ReloadSubCommand(plugin));
    }

    @Override
    protected CompletableFuture<Void> execute(CommandContext ctx) {
        // Open admin panel UI for player senders
        if (ctx.sender() instanceof Player player) {
            World world = player.getWorld();
            if (world == null) {
                ctx.sendMessage(MessageUtil.error("Failed to open admin panel."));
                return CompletableFuture.completedFuture(null);
            }

            return CompletableFuture.runAsync(() -> {
                Ref<EntityStore> ref = player.getReference();
                Store<EntityStore> store = ref.getStore();
                PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

                if (playerRef == null) return;

                AdminPanelPage adminPage = new AdminPanelPage(playerRef, plugin);
                player.getPageManager().openCustomPage(ref, store, adminPage);
            }, world);
        }

        // Console fallback: show text help
        ctx.sendMessage(MessageUtil.info("HytaleVault Admin Commands:"));
        ctx.sendMessage(MessageUtil.of("/vaultadmin clear <player> - Clear a player's vaults"));
        ctx.sendMessage(MessageUtil.of("/vaultadmin reload - Reload configuration"));
        ctx.sendMessage(MessageUtil.of(""));
        ctx.sendMessage(MessageUtil.info("Vault access is controlled by permissions:"));
        ctx.sendMessage(MessageUtil.of("  lp user <player> permission set hytalevault.vault.<number>"));
        ctx.sendMessage(MessageUtil.of("  Example: hytalevault.vault.3 grants vaults 1-3"));
        return CompletableFuture.completedFuture(null);
    }

    // Subcommand: clear
    private static class ClearSubCommand extends AbstractCommand {
        private final HytaleVaultPlugin plugin;
        private final RequiredArg<PlayerRef> playerArg;

        public ClearSubCommand(HytaleVaultPlugin plugin) {
            super("clear", "Clear a player's vaults");
            this.plugin = plugin;
            this.playerArg = withRequiredArg("player", "Player whose vaults to clear", ArgTypes.PLAYER_REF);
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            PlayerRef targetRef = ctx.get(playerArg);

            if (targetRef == null) {
                ctx.sendMessage(MessageUtil.error(plugin.getConfigManager().getMessagePlayerNotFoundRaw()));
                return CompletableFuture.completedFuture(null);
            }

            return plugin.getVaultManager().clearVault(targetRef.getUuid()).thenRun(() -> {
                ctx.sendMessage(MessageUtil.success("Vaults cleared for " + targetRef.getUsername()));
            });
        }
    }

    // Subcommand: reload
    private static class ReloadSubCommand extends AbstractCommand {
        private final HytaleVaultPlugin plugin;

        public ReloadSubCommand(HytaleVaultPlugin plugin) {
            super("reload", "Reload configuration");
            this.plugin = plugin;
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            plugin.reload();
            ctx.sendMessage(MessageUtil.success("Configuration reloaded."));
            return CompletableFuture.completedFuture(null);
        }
    }
}
