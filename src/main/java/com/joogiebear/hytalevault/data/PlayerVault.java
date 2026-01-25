package com.joogiebear.hytalevault.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a player's complete vault storage.
 * Contains multiple numbered vaults that can be unlocked progressively.
 */
public class PlayerVault {

    private final UUID playerUuid;
    private final Map<Integer, VaultPage> vaults;
    private int unlockedVaults;
    private final int slotsPerVault;
    private boolean dirty;

    /**
     * Create a new player vault.
     *
     * @param playerUuid    The player's UUID
     * @param defaultVaults The default number of unlocked vaults
     * @param slotsPerVault The number of slots per vault
     */
    public PlayerVault(UUID playerUuid, int defaultVaults, int slotsPerVault) {
        this.playerUuid = playerUuid;
        this.vaults = new HashMap<>();
        this.unlockedVaults = defaultVaults;
        this.slotsPerVault = slotsPerVault;
        this.dirty = false;

        // Initialize default vaults
        for (int i = 1; i <= defaultVaults; i++) {
            vaults.put(i, new VaultPage(i, slotsPerVault));
        }
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public int getUnlockedVaults() {
        return unlockedVaults;
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
     * Get a specific vault.
     *
     * @param vaultNumber The vault number (1-indexed)
     * @return The vault, or null if not unlocked
     */
    public VaultPage getVault(int vaultNumber) {
        if (vaultNumber < 1 || vaultNumber > unlockedVaults) {
            return null;
        }
        return vaults.computeIfAbsent(vaultNumber, num -> new VaultPage(num, slotsPerVault));
    }

    /**
     * Check if a vault is unlocked.
     *
     * @param vaultNumber The vault number (1-indexed)
     * @return True if the vault is unlocked
     */
    public boolean isVaultUnlocked(int vaultNumber) {
        return vaultNumber >= 1 && vaultNumber <= unlockedVaults;
    }

    /**
     * Unlock additional vaults.
     *
     * @param count     The number of vaults to unlock
     * @param maxVaults The maximum allowed vaults
     * @return The new total number of unlocked vaults
     */
    public int unlockVaults(int count, int maxVaults) {
        int newTotal = Math.min(unlockedVaults + count, maxVaults);
        if (newTotal > unlockedVaults) {
            for (int i = unlockedVaults + 1; i <= newTotal; i++) {
                vaults.put(i, new VaultPage(i, slotsPerVault));
            }
            unlockedVaults = newTotal;
            markDirty();
        }
        return unlockedVaults;
    }

    /**
     * Set the number of unlocked vaults directly.
     *
     * @param count     The number of vaults
     * @param maxVaults The maximum allowed vaults
     */
    public void setUnlockedVaults(int count, int maxVaults) {
        this.unlockedVaults = Math.max(1, Math.min(count, maxVaults));
        markDirty();
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
        json.addProperty("unlockedVaults", unlockedVaults);
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
     * Supports both new keys and legacy "pages"/"unlockedPages" keys for backward compatibility.
     *
     * @param json          The JSON object
     * @param defaultVaults Default vaults if not specified
     * @param slotsPerVault Slots per vault
     * @return The deserialized vault
     */
    public static PlayerVault deserialize(JsonObject json, int defaultVaults, int slotsPerVault) {
        UUID playerUuid = UUID.fromString(json.get("playerUuid").getAsString());

        int savedUnlocked = json.has("unlockedVaults")
                ? json.get("unlockedVaults").getAsInt()
                : json.has("unlockedPages")
                        ? json.get("unlockedPages").getAsInt()
                        : defaultVaults;
        // Ensure existing players always have at least the configured defaultVaults
        int unlockedVaults = Math.max(savedUnlocked, defaultVaults);

        int slots = json.has("slotsPerVault")
                ? json.get("slotsPerVault").getAsInt()
                : json.has("slotsPerPage")
                        ? json.get("slotsPerPage").getAsInt()
                        : slotsPerVault;

        PlayerVault playerVault = new PlayerVault(playerUuid, unlockedVaults, slots);
        playerVault.unlockedVaults = unlockedVaults;

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
