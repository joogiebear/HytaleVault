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
import com.joogiebear.hytalevault.managers.ConfigManager;
import com.joogiebear.hytalevault.util.MessageUtil;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Admin command for vault management.
 * Usage: /vaultadmin <subcommand> [args]
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
        addSubCommand(new GiveSubCommand(plugin));
        addSubCommand(new SetSubCommand(plugin));
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
        ctx.sendMessage(MessageUtil.of("/vaultadmin give <player> <vaults> - Grant vaults"));
        ctx.sendMessage(MessageUtil.of("/vaultadmin set <player> <vaults> - Set vault count"));
        ctx.sendMessage(MessageUtil.of("/vaultadmin clear <player> - Clear a player's vaults"));
        ctx.sendMessage(MessageUtil.of("/vaultadmin reload - Reload configuration"));
        return CompletableFuture.completedFuture(null);
    }

    // Subcommand: give
    private static class GiveSubCommand extends AbstractCommand {
        private final HytaleVaultPlugin plugin;
        private final RequiredArg<PlayerRef> playerArg;
        private final RequiredArg<Integer> vaultsArg;

        public GiveSubCommand(HytaleVaultPlugin plugin) {
            super("give", "Grant vaults to a player");
            this.plugin = plugin;
            this.playerArg = withRequiredArg("player", "Player to give vaults to", ArgTypes.PLAYER_REF);
            this.vaultsArg = withRequiredArg("vaults", "Number of vaults to grant", ArgTypes.INTEGER);
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            PlayerRef targetRef = ctx.get(playerArg);
            int vaults = ctx.get(vaultsArg);

            if (targetRef == null) {
                ctx.sendMessage(MessageUtil.error(plugin.getConfigManager().getMessagePlayerNotFoundRaw()));
                return CompletableFuture.completedFuture(null);
            }

            if (vaults < 1) {
                ctx.sendMessage(MessageUtil.error("Vaults must be at least 1."));
                return CompletableFuture.completedFuture(null);
            }

            return plugin.getVaultManager().grantVaults(targetRef.getUuid(), vaults).thenAccept(newTotal -> {
                ctx.sendMessage(MessageUtil.success("Granted " + vaults + " vaults to " + targetRef.getUsername()));
                targetRef.sendMessage(MessageUtil.success("You now have " + newTotal + " vaults!"));
            });
        }
    }

    // Subcommand: set
    private static class SetSubCommand extends AbstractCommand {
        private final HytaleVaultPlugin plugin;
        private final RequiredArg<PlayerRef> playerArg;
        private final RequiredArg<Integer> vaultsArg;

        public SetSubCommand(HytaleVaultPlugin plugin) {
            super("set", "Set a player's vault count");
            this.plugin = plugin;
            this.playerArg = withRequiredArg("player", "Player to modify", ArgTypes.PLAYER_REF);
            this.vaultsArg = withRequiredArg("vaults", "Number of vaults to set", ArgTypes.INTEGER);
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            ConfigManager config = plugin.getConfigManager();
            PlayerRef targetRef = ctx.get(playerArg);
            int vaults = ctx.get(vaultsArg);

            if (targetRef == null) {
                ctx.sendMessage(MessageUtil.error(config.getMessagePlayerNotFoundRaw()));
                return CompletableFuture.completedFuture(null);
            }

            if (vaults < 1 || vaults > config.getMaxVaults()) {
                ctx.sendMessage(MessageUtil.error("Vaults must be between 1 and " + config.getMaxVaults()));
                return CompletableFuture.completedFuture(null);
            }

            return plugin.getVaultManager().setVaults(targetRef.getUuid(), vaults).thenRun(() -> {
                ctx.sendMessage(MessageUtil.success("Set " + targetRef.getUsername() + "'s vaults to " + vaults + "."));
            });
        }
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
