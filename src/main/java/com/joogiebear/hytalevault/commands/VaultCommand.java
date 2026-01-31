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
import com.joogiebear.hytalevault.gui.VaultSelectorPage;
import com.joogiebear.hytalevault.gui.VaultUI;
import com.joogiebear.hytalevault.managers.ConfigManager;
import com.joogiebear.hytalevault.util.MessageUtil;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Main vault command for players to access their vault.
 * Usage: /vault [number]
 */
public class VaultCommand extends AbstractCommand {

    private static final Logger LOGGER = Logger.getLogger("HytaleVault");

    private final HytaleVaultPlugin plugin;
    private final VaultUI vaultUI;

    public VaultCommand(HytaleVaultPlugin plugin) {
        super("vault", "Open a personal vault");
        this.plugin = plugin;
        this.vaultUI = new VaultUI(plugin);

        // Add alias
        addAliases("v");

        // Register numbered subcommands for /vault 1, /vault 2, etc.
        ConfigManager config = plugin.getConfigManager();
        for (int i = 1; i <= config.getMaxVaults(); i++) {
            addSubCommand(new VaultNumberSubCommand(plugin, vaultUI, i));
        }
    }

    @Override
    public boolean hasPermission(CommandSender sender) {
        return true;
    }

    @Override
    protected CompletableFuture<Void> execute(CommandContext ctx) {
        // No argument provided - open vault selector UI
        if (!(ctx.sender() instanceof Player player)) {
            ctx.sendMessage(MessageUtil.error("This command can only be used by players."));
            return CompletableFuture.completedFuture(null);
        }

        World world = player.getWorld();
        if (world == null) {
            ctx.sendMessage(MessageUtil.error("Failed to open vault selector. Please try again."));
            return CompletableFuture.completedFuture(null);
        }

        ConfigManager config = plugin.getConfigManager();

        // Get the max vault this player has permission to access
        int maxAccessibleVault = plugin.getVaultManager().getMaxAccessibleVault(player);

        // Load vault async, then access ECS store on the world thread
        return plugin.getVaultManager().getVault(player).thenAcceptAsync(vault -> {
            Ref<EntityStore> ref = player.getReference();
            Store<EntityStore> store = ref.getStore();
            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

            if (playerRef == null) return;

            VaultSelectorPage selectorPage = new VaultSelectorPage(
                    playerRef, vault, vaultUI, maxAccessibleVault, plugin.getVaultManager(), player
            );
            player.getPageManager().openCustomPage(ref, store, selectorPage);
        }, world).exceptionally(e -> {
            ctx.sendMessage(MessageUtil.error("Failed to open vault selector. Please try again."));
            LOGGER.warning("Failed to open vault selector for " + player.getDisplayName() + ": " + e.getMessage());
            return null;
        });
    }

    public VaultUI getVaultUI() {
        return vaultUI;
    }

    /**
     * Subcommand for opening a specific numbered vault.
     * Registered as "1", "2", ..., "9" so /vault 1, /vault 2, etc. work.
     */
    private static class VaultNumberSubCommand extends AbstractCommand {
        private static final Logger LOG = Logger.getLogger("HytaleVault");
        private final HytaleVaultPlugin plugin;
        private final VaultUI vaultUI;
        private final int vaultNumber;

        public VaultNumberSubCommand(HytaleVaultPlugin plugin, VaultUI vaultUI, int vaultNumber) {
            super(String.valueOf(vaultNumber), "Open vault #" + vaultNumber);
            this.plugin = plugin;
            this.vaultUI = vaultUI;
            this.vaultNumber = vaultNumber;
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

            ConfigManager config = plugin.getConfigManager();

            if (!plugin.getVaultManager().hasVaultPermission(player, vaultNumber)) {
                ctx.sendMessage(MessageUtil.error(config.getMessageNoPermissionRaw()));
                return CompletableFuture.completedFuture(null);
            }

            World world = player.getWorld();
            if (world == null) {
                ctx.sendMessage(MessageUtil.error("Failed to load vault. Please try again."));
                return CompletableFuture.completedFuture(null);
            }

            // Load vault async, then access ECS store on the world thread
            return plugin.getVaultManager().getVault(player).thenAcceptAsync(vault -> {
                Ref<EntityStore> ref = player.getReference();
                Store<EntityStore> store = ref.getStore();
                PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

                if (playerRef == null) return;

                if (!vault.isVaultUnlocked(vaultNumber)) {
                    playerRef.sendMessage(MessageUtil.error(config.getMessageVaultNotUnlockedRaw()));
                    return;
                }
                vaultUI.openVault(player, ref, store, playerRef, vault, vaultNumber);
            }, world).exceptionally(e -> {
                ctx.sendMessage(MessageUtil.error("Failed to load vault. Please try again."));
                LOG.warning("Failed to load vault for " + player.getDisplayName() + ": " + e.getMessage());
                return null;
            });
        }
    }
}
