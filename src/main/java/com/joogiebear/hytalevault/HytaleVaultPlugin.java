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

    public HytaleVaultPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    @Override
    protected void setup() {
        LOGGER.info("HytaleVault is setting up...");

        // Initialize configuration
        configManager = new ConfigManager(this);
        configManager.loadConfig();

        // Initialize storage backend
        Path dataPath = getPluginDataPath().resolve(configManager.getStorageDirectory());
        storageBackend = new JsonStorage(dataPath);
        storageBackend.initialize();

        // Initialize vault manager
        vaultManager = new VaultManager(this, storageBackend);
    }

    @Override
    protected void start() {
        LOGGER.info("HytaleVault is starting...");

        // Register event listeners
        new PlayerListener(this).register();

        // Register commands using the CommandRegistry
        vaultCommand = new VaultCommand(this);
        getCommandRegistry().registerCommand(vaultCommand);
        LOGGER.info("Registered /vault command");

        getCommandRegistry().registerCommand(new VaultAdminCommand(this));
        LOGGER.info("Registered /vaultadmin command");

        getCommandRegistry().registerCommand(new VaultInfoCommand(this));
        LOGGER.info("Registered /vaultinfo command");

        LOGGER.info("HytaleVault has been enabled!");
    }

    @Override
    protected void shutdown() {
        LOGGER.info("HytaleVault is shutting down...");

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
