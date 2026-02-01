package com.joogiebear.hytalevault;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.joogiebear.hytalevault.commands.VaultAdminCommand;
import com.joogiebear.hytalevault.commands.VaultCommand;
import com.joogiebear.hytalevault.commands.VaultInfoCommand;
import com.joogiebear.hytalevault.data.storage.JsonStorage;
import com.joogiebear.hytalevault.data.storage.StorageBackend;
import com.joogiebear.hytalevault.listeners.PlayerListener;
import com.joogiebear.hytalevault.managers.ConfigManager;
import com.joogiebear.hytalevault.managers.VaultManager;
import com.joogiebear.hytalevault.gui.VaultUI;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * HytaleVault - Personal storage plugin for Hytale servers.
 * Provides players with numbered vaults for personal storage.
 */
public class HytaleVaultPlugin extends JavaPlugin {

    private static final Logger LOGGER = Logger.getLogger("HytaleVault");
    private static HytaleVaultPlugin instance;

    private ConfigManager configManager;
    private VaultManager vaultManager;
    private StorageBackend storageBackend;
    private VaultCommand vaultCommand;
    private PlayerListener playerListener;
    private ScheduledExecutorService autoSaveScheduler;
    private ScheduledFuture<?> autoSaveTask;

    public HytaleVaultPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    @Override
    protected void setup() {
        LOGGER.info("HytaleVault is setting up...");

        // Load config early - required for command registration (subcommands depend on maxVaults)
        // This is lightweight config loading, not heavy data loading
        configManager = new ConfigManager(this);
        configManager.loadConfig();

        // Register event listeners (must be in setup per Hytale docs)
        playerListener = new PlayerListener(this);
        playerListener.register();

        // Register commands using the CommandRegistry (must be in setup per Hytale docs)
        vaultCommand = new VaultCommand(this);
        getCommandRegistry().registerCommand(vaultCommand);
        LOGGER.info("Registered /vault command");

        getCommandRegistry().registerCommand(new VaultAdminCommand(this));
        LOGGER.info("Registered /vaultadmin command");

        getCommandRegistry().registerCommand(new VaultInfoCommand(this));
        LOGGER.info("Registered /vaultinfo command");
    }

    @Override
    protected void start() {
        LOGGER.info("HytaleVault is starting...");

        // Initialize storage backend
        Path dataPath = getPluginDataPath().resolve(configManager.getStorageDirectory());
        storageBackend = new JsonStorage(dataPath);
        storageBackend.initialize();

        // Initialize vault manager
        vaultManager = new VaultManager(this, storageBackend);

        // Start auto-save task
        int saveInterval = configManager.getSaveIntervalSeconds();
        if (saveInterval > 0) {
            autoSaveScheduler = Executors.newSingleThreadScheduledExecutor();
            autoSaveTask = autoSaveScheduler.scheduleAtFixedRate(
                    this::autoSave,
                    saveInterval,
                    saveInterval,
                    TimeUnit.SECONDS
            );
            LOGGER.info("Auto-save scheduled every " + saveInterval + " seconds");
        }

        LOGGER.info("HytaleVault has been enabled!");
    }

    /**
     * Periodic auto-save of all loaded vaults.
     */
    private void autoSave() {
        if (vaultManager != null) {
            vaultManager.saveAll();
            LOGGER.fine("Auto-save completed");
        }
    }

    @Override
    protected void shutdown() {
        LOGGER.info("HytaleVault is shutting down...");

        // Stop auto-save task first
        if (autoSaveTask != null) {
            autoSaveTask.cancel(false);
        }

        // Shutdown the scheduler properly
        if (autoSaveScheduler != null) {
            autoSaveScheduler.shutdown();
            try {
                if (!autoSaveScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    autoSaveScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                autoSaveScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // Close all open vault windows (syncs data)
        VaultUI vaultUI = getVaultUI();
        if (vaultUI != null) {
            vaultUI.closeAll();
        }

        // Save all loaded vaults
        if (vaultManager != null) {
            vaultManager.saveAll();
        }

        if (storageBackend != null) {
            storageBackend.shutdown();
        }

        LOGGER.info("HytaleVault has been disabled!");
    }

    /**
     * Get the plugin instance.
     */
    public static HytaleVaultPlugin getInstance() {
        return instance;
    }

    /**
     * Get the configuration manager.
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }

    /**
     * Get the vault manager.
     */
    public VaultManager getVaultManager() {
        return vaultManager;
    }

    /**
     * Get the storage backend.
     */
    public StorageBackend getStorageBackend() {
        return storageBackend;
    }

    /**
     * Get the vault command.
     */
    public VaultCommand getVaultCommand() {
        return vaultCommand;
    }

    /**
     * Get the vault UI manager.
     */
    public VaultUI getVaultUI() {
        return vaultCommand != null ? vaultCommand.getVaultUI() : null;
    }

    /**
     * Get the plugin's data directory path.
     * Uses mods/HytaleVault instead of Hytale's default Group_Name convention.
     */
    public Path getPluginDataPath() {
        return Path.of("mods", "HytaleVault");
    }

    /**
     * Reload the plugin configuration.
     */
    public void reload() {
        configManager.loadConfig();
        LOGGER.info("Configuration reloaded.");
    }
}
