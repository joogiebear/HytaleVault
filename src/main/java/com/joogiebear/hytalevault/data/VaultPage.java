package com.joogiebear.hytalevault.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import org.bson.BsonDocument;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a single vault's item storage.
 * Each vault contains a fixed number of item slots.
 */
public class VaultPage {

    private final int vaultNumber;
    private final int slots;
    private final Map<Integer, ItemStack> items;

    /**
     * Create a new vault storage.
     *
     * @param vaultNumber The vault number (1-indexed)
     * @param slots       The number of slots in this vault
     */
    public VaultPage(int vaultNumber, int slots) {
        this.vaultNumber = vaultNumber;
        this.slots = slots;
        this.items = new HashMap<>();
    }

    public int getVaultNumber() {
        return vaultNumber;
    }

    public int getSlots() {
        return slots;
    }

    public ItemStack getItem(int slot) {
        if (slot < 0 || slot >= slots) {
            return null;
        }
        return items.get(slot);
    }

    public void setItem(int slot, ItemStack item) {
        if (slot < 0 || slot >= slots) {
            return;
        }
        if (item == null || item.isEmpty()) {
            items.remove(slot);
        } else {
            items.put(slot, item);
        }
    }

    public void clearSlot(int slot) {
        setItem(slot, null);
    }

    public void clear() {
        items.clear();
    }

    public boolean isEmpty(int slot) {
        ItemStack item = items.get(slot);
        return item == null || item.isEmpty();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public Map<Integer, ItemStack> getItems() {
        return new HashMap<>(items);
    }

    public int getItemCount() {
        return items.size();
    }

    /**
     * Serialize this vault to JSON.
     */
    public JsonObject serialize() {
        JsonObject json = new JsonObject();
        json.addProperty("vaultNumber", vaultNumber);
        json.addProperty("slots", slots);

        JsonArray itemsArray = new JsonArray();
        for (Map.Entry<Integer, ItemStack> entry : items.entrySet()) {
            ItemStack item = entry.getValue();
            if (item == null || item.isEmpty()) continue;

            JsonObject itemJson = new JsonObject();
            itemJson.addProperty("slot", entry.getKey());
            itemJson.addProperty("itemId", item.getItemId());
            itemJson.addProperty("quantity", item.getQuantity());

            if (item.getDurability() != 0) {
                itemJson.addProperty("durability", item.getDurability());
            }
            if (item.getMaxDurability() != 0) {
                itemJson.addProperty("maxDurability", item.getMaxDurability());
            }
            if (item.getMetadata() != null) {
                itemJson.addProperty("metadata", item.getMetadata().toJson());
            }

            itemsArray.add(itemJson);
        }
        json.add("items", itemsArray);

        return json;
    }

    /**
     * Deserialize a vault from JSON.
     * Supports both new "vaultNumber" and legacy "pageNumber" keys.
     */
    public static VaultPage deserialize(JsonObject json) {
        int vaultNumber = json.has("vaultNumber")
                ? json.get("vaultNumber").getAsInt()
                : json.get("pageNumber").getAsInt();
        int slots = json.get("slots").getAsInt();

        VaultPage vault = new VaultPage(vaultNumber, slots);

        if (json.has("items")) {
            JsonArray itemsArray = json.getAsJsonArray("items");
            for (JsonElement element : itemsArray) {
                JsonObject itemJson = element.getAsJsonObject();
                int slot = itemJson.get("slot").getAsInt();

                String itemId = itemJson.has("itemId") ? itemJson.get("itemId").getAsString() : null;
                if (itemId == null || itemId.isEmpty()) continue;

                int quantity = itemJson.has("quantity") ? itemJson.get("quantity").getAsInt() : 1;

                ItemStack item;
                if (itemJson.has("durability") || itemJson.has("maxDurability") || itemJson.has("metadata")) {
                    double durability = itemJson.has("durability") ? itemJson.get("durability").getAsDouble() : 0;
                    double maxDurability = itemJson.has("maxDurability") ? itemJson.get("maxDurability").getAsDouble() : 0;
                    BsonDocument metadata = null;
                    if (itemJson.has("metadata")) {
                        metadata = BsonDocument.parse(itemJson.get("metadata").getAsString());
                    }
                    item = new ItemStack(itemId, quantity, durability, maxDurability, metadata);
                } else {
                    item = new ItemStack(itemId, quantity);
                }

                if (!item.isEmpty()) {
                    vault.items.put(slot, item);
                }
            }
        }

        return vault;
    }
}
