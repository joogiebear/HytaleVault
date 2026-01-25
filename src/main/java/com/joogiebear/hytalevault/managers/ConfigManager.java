package com.joogiebear.hytalevault.managers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.joogiebear.hytalevault.HytaleVaultPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages plugin configuration loading and access.
 */
public class ConfigManager {

    private static final Logger LOGGER = Logger.getLogger("HytaleVault");

    private final HytaleVaultPlugin plugin;
    private JsonObject config;

    // Cached config values
    private int defaultVaults;
    private int maxVaults;
    private int slotsPerVault;
    private String storageType;
    private String storageDirectory;
    private int saveIntervalSeconds;

    // Messages (raw strings)
    private String messagePrefix;
    private String messageVaultOpened;
    private String messageVaultUnlocked;
    private String messageNoPermission;
    private String messagePlayerNotFound;
    private String messageInvalidVault;
    private String messageVaultNotUnlocked;
    private String messageVaultCleared;
    private String messageVaultsGranted;
    private String messageConfigReloaded;

    public ConfigManager(HytaleVaultPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        Path configPath = plugin.getPluginDataPath().resolve("config.json");

        if (!Files.exists(configPath)) {
            saveDefaultConfig(configPath);
        }

        try {
            String content = Files.readString(configPath);
            config = JsonParser.parseString(content).getAsJsonObject();
            parseConfig();
            LOGGER.info("Configuration loaded successfully.");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load configuration", e);
            loadDefaults();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to parse configuration", e);
            loadDefaults();
        }
    }

    private void saveDefaultConfig(Path configPath) {
        try {
            Files.createDirectories(configPath.getParent());

            try (InputStream is = getClass().getResourceAsStream("/config.json")) {
                if (is != null) {
                    Files.copy(is, configPath);
                    return;
                }
            }

            String defaultConfig = """
                {
                  "vault": {
                    "defaultVaults": 1,
                    "maxVaults": 9,
                    "slotsPerVault": 54
                  },
                  "storage": {
                    "type": "json",
                    "directory": "playerdata",
                    "saveIntervalSeconds": 300
                  },
                  "messages": {
                    "prefix": "[HytaleVault] ",
                    "vaultOpened": "Vault #{vault} opened!",
                    "vaultUnlocked": "Vault #{vault} unlocked!",
                    "noPermission": "You don't have permission to do that.",
                    "playerNotFound": "Player not found.",
                    "invalidVault": "Invalid vault number.",
                    "vaultNotUnlocked": "You haven't unlocked that vault yet.",
                    "vaultCleared": "Vault cleared for {player}.",
                    "vaultsGranted": "Granted {vaults} vaults to {player}.",
                    "configReloaded": "Configuration reloaded."
                  }
                }
                """;
            Files.writeString(configPath, defaultConfig);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to save default configuration", e);
        }
    }

    private void parseConfig() {
        JsonObject vault = config.getAsJsonObject("vault");
        if (vault != null) {
            defaultVaults = getInt(vault, "defaultVaults", getInt(vault, "defaultPages", 1));
            maxVaults = getInt(vault, "maxVaults", getInt(vault, "maxPages", 9));
            slotsPerVault = getInt(vault, "slotsPerVault", getInt(vault, "slotsPerPage", 54));
        } else {
            defaultVaults = 1;
            maxVaults = 9;
            slotsPerVault = 54;
        }

        JsonObject storage = config.getAsJsonObject("storage");
        if (storage != null) {
            storageType = getString(storage, "type", "json");
            storageDirectory = getString(storage, "directory", "playerdata");
            saveIntervalSeconds = getInt(storage, "saveIntervalSeconds", 300);
        } else {
            storageType = "json";
            storageDirectory = "playerdata";
            saveIntervalSeconds = 300;
        }

        JsonObject messages = config.getAsJsonObject("messages");
        if (messages != null) {
            messagePrefix = getString(messages, "prefix", "[HytaleVault] ");
            messageVaultOpened = getString(messages, "vaultOpened", "Vault #{vault} opened!");
            messageVaultUnlocked = getString(messages, "vaultUnlocked", "Vault #{vault} unlocked!");
            messageNoPermission = getString(messages, "noPermission", "You don't have permission.");
            messagePlayerNotFound = getString(messages, "playerNotFound", "Player not found.");
            messageInvalidVault = getString(messages, "invalidVault", "Invalid vault number.");
            messageVaultNotUnlocked = getString(messages, "vaultNotUnlocked", "You haven't unlocked that vault yet.");
            messageVaultCleared = getString(messages, "vaultCleared", "Vault cleared for {player}.");
            messageVaultsGranted = getString(messages, "vaultsGranted", "Granted {vaults} vaults to {player}.");
            messageConfigReloaded = getString(messages, "configReloaded", "Configuration reloaded.");
        } else {
            loadDefaultMessages();
        }
    }

    private void loadDefaults() {
        defaultVaults = 1;
        maxVaults = 9;
        slotsPerVault = 54;
        storageType = "json";
        storageDirectory = "playerdata";
        saveIntervalSeconds = 300;
        loadDefaultMessages();
    }

    private void loadDefaultMessages() {
        messagePrefix = "[HytaleVault] ";
        messageVaultOpened = "Vault #{vault} opened!";
        messageVaultUnlocked = "Vault #{vault} unlocked!";
        messageNoPermission = "You don't have permission.";
        messagePlayerNotFound = "Player not found.";
        messageInvalidVault = "Invalid vault number.";
        messageVaultNotUnlocked = "You haven't unlocked that vault yet.";
        messageVaultCleared = "Vault cleared for {player}.";
        messageVaultsGranted = "Granted {vaults} vaults to {player}.";
        messageConfigReloaded = "Configuration reloaded.";
    }

    private int getInt(JsonObject obj, String key, int defaultValue) {
        return obj.has(key) ? obj.get(key).getAsInt() : defaultValue;
    }

    private String getString(JsonObject obj, String key, String defaultValue) {
        return obj.has(key) ? obj.get(key).getAsString() : defaultValue;
    }

    // Getters for config values
    public int getDefaultVaults() { return defaultVaults; }
    public int getMaxVaults() { return maxVaults; }
    public int getSlotsPerVault() { return slotsPerVault; }
    public String getStorageType() { return storageType; }
    public String getStorageDirectory() { return storageDirectory; }
    public int getSaveIntervalSeconds() { return saveIntervalSeconds; }

    // Raw message getters (without prefix, for use with MessageUtil)
    public String getMessageNoPermissionRaw() { return messageNoPermission; }
    public String getMessagePlayerNotFoundRaw() { return messagePlayerNotFound; }
    public String getMessageInvalidVaultRaw() { return messageInvalidVault; }
    public String getMessageVaultNotUnlockedRaw() { return messageVaultNotUnlocked; }
    public String getMessageVaultOpenedRaw() { return messageVaultOpened; }
    public String getMessageVaultUnlockedRaw() { return messageVaultUnlocked; }
    public String getMessageVaultClearedRaw() { return messageVaultCleared; }
    public String getMessageVaultsGrantedRaw() { return messageVaultsGranted; }
    public String getMessageConfigReloadedRaw() { return messageConfigReloaded; }

    public String formatMessage(String message, String... replacements) {
        String result = message;
        for (int i = 0; i < replacements.length - 1; i += 2) {
            result = result.replace("{" + replacements[i] + "}", replacements[i + 1]);
        }
        return result;
    }
}
