package com.joogiebear.hytalevault.data.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.joogiebear.hytalevault.data.PlayerVault;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JSON file-based storage backend.
 * Stores each player's vault in a separate JSON file.
 */
public class JsonStorage implements StorageBackend {

    private static final Logger LOGGER = Logger.getLogger("HytaleVault");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path dataDirectory;

    /**
     * Create a new JSON storage backend.
     *
     * @param dataDirectory The directory to store player data files
     */
    public JsonStorage(Path dataDirectory) {
        this.dataDirectory = dataDirectory;
    }

    @Override
    public void initialize() {
        try {
            if (!Files.exists(dataDirectory)) {
                Files.createDirectories(dataDirectory);
                LOGGER.info("Created data directory: " + dataDirectory);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to create data directory", e);
        }
    }

    @Override
    public void shutdown() {
        // No cleanup needed for file-based storage
    }

    @Override
    public CompletableFuture<PlayerVault> loadVault(UUID playerUuid, int slotsPerVault) {
        return CompletableFuture.supplyAsync(() -> {
            Path filePath = getPlayerFile(playerUuid);

            if (!Files.exists(filePath)) {
                // Create a new vault for this player
                return new PlayerVault(playerUuid, slotsPerVault);
            }

            try {
                String content = Files.readString(filePath);
                JsonObject json = JsonParser.parseString(content).getAsJsonObject();
                return PlayerVault.deserialize(json, slotsPerVault);
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to load vault for " + playerUuid, e);
                return new PlayerVault(playerUuid, slotsPerVault);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to parse vault data for " + playerUuid, e);
                return new PlayerVault(playerUuid, slotsPerVault);
            }
        });
    }

    @Override
    public CompletableFuture<Void> saveVault(PlayerVault vault) {
        return CompletableFuture.runAsync(() -> {
            Path filePath = getPlayerFile(vault.getPlayerUuid());

            try {
                JsonObject json = vault.serialize();
                String content = GSON.toJson(json);
                Files.writeString(filePath, content);
                vault.markClean();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to save vault for " + vault.getPlayerUuid(), e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> deleteVault(UUID playerUuid) {
        return CompletableFuture.runAsync(() -> {
            Path filePath = getPlayerFile(playerUuid);

            try {
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                }
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to delete vault for " + playerUuid, e);
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> vaultExists(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            Path filePath = getPlayerFile(playerUuid);
            return Files.exists(filePath);
        });
    }

    /**
     * Get the file path for a player's vault data.
     *
     * @param playerUuid The player's UUID
     * @return The file path
     */
    private Path getPlayerFile(UUID playerUuid) {
        return dataDirectory.resolve(playerUuid.toString() + ".json");
    }
}
