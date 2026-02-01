package com.joogiebear.hytalevault.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a player's complete vault storage.
 * Vault access is controlled by permissions, not unlock counts.
 */
public class PlayerVault {

    private final UUID playerUuid;
    private final Map<Integer, VaultPage> vaults;
    private final int slotsPerVault;
    private boolean dirty;

    /**
     * Create a new player vault.
     *
     * @param playerUuid    The player's UUID
     * @param slotsPerVault The number of slots per vault
     */
    public PlayerVault(UUID playerUuid, int slotsPerVault) {
        this.playerUuid = playerUuid;
        this.vaults = new HashMap<>();
        this.slotsPerVault = slotsPerVault;
        this.dirty = false;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public int getSlotsPerVault() {
        return slotsPerVault;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void markClean() {
        this.dirty = false;
    }

    public void markDirty() {
        this.dirty = true;
    }

    /**
     * Get or create a vault (permission-based access).
     * Creates the vault data on-demand if it doesn't exist.
     *
     * @param vaultNumber   The vault number (1-indexed)
     * @param slotsPerVault The number of slots if creating new vault
     * @return The vault page, or null if invalid number
     */
    public VaultPage getOrCreateVault(int vaultNumber, int slotsPerVault) {
        if (vaultNumber < 1) {
            return null;
        }
        VaultPage vault = vaults.get(vaultNumber);
        if (vault == null) {
            vault = new VaultPage(vaultNumber, slotsPerVault);
            vaults.put(vaultNumber, vault);
            markDirty();
        }
        return vault;
    }

    /**
     * Get a vault if it exists (for reading only).
     *
     * @param vaultNumber The vault number (1-indexed)
     * @return The vault page, or null if not created yet
     */
    public VaultPage getVault(int vaultNumber) {
        if (vaultNumber < 1) {
            return null;
        }
        return vaults.get(vaultNumber);
    }

    /**
     * Clear all items from all vaults.
     */
    public void clearAll() {
        for (VaultPage vault : vaults.values()) {
            vault.clear();
        }
        markDirty();
    }

    /**
     * Get the total number of items stored across all vaults.
     *
     * @return Total item count
     */
    public int getTotalItemCount() {
        return vaults.values().stream()
                .mapToInt(VaultPage::getItemCount)
                .sum();
    }

    /**
     * Serialize this player vault to JSON.
     *
     * @return The JSON representation
     */
    public JsonObject serialize() {
        JsonObject json = new JsonObject();
        json.addProperty("playerUuid", playerUuid.toString());
        json.addProperty("slotsPerVault", slotsPerVault);

        JsonArray vaultsArray = new JsonArray();
        for (VaultPage vault : vaults.values()) {
            if (!vault.isEmpty()) {
                vaultsArray.add(vault.serialize());
            }
        }
        json.add("vaults", vaultsArray);

        return json;
    }

    /**
     * Deserialize a player vault from JSON.
     * Supports legacy keys for backward compatibility.
     *
     * @param json          The JSON object
     * @param slotsPerVault Slots per vault (used if not in JSON)
     * @return The deserialized vault
     */
    public static PlayerVault deserialize(JsonObject json, int slotsPerVault) {
        UUID playerUuid = UUID.fromString(json.get("playerUuid").getAsString());

        int slots = json.has("slotsPerVault")
                ? json.get("slotsPerVault").getAsInt()
                : json.has("slotsPerPage")
                        ? json.get("slotsPerPage").getAsInt()
                        : slotsPerVault;

        PlayerVault playerVault = new PlayerVault(playerUuid, slots);

        // Support both "vaults" and legacy "pages" array keys
        String arrayKey = json.has("vaults") ? "vaults" : "pages";
        if (json.has(arrayKey)) {
            JsonArray vaultsArray = json.getAsJsonArray(arrayKey);
            for (JsonElement element : vaultsArray) {
                VaultPage vault = VaultPage.deserialize(element.getAsJsonObject());
                playerVault.vaults.put(vault.getVaultNumber(), vault);
            }
        }

        return playerVault;
    }
}
