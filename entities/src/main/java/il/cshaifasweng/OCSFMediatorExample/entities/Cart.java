package il.cshaifasweng.OCSFMediatorExample.entities;

import java.util.HashMap;
import java.util.Map;

public class Cart {

    public static final double DELIVERY_FEE = 20.0;
    private static int cartCounter = 0;
    private int id;
    private Map<MenuItem, Integer> items;
    private Map<MenuItem, String> notes;
    private double totalCost;
    private boolean includeDeliveryFee = true;

    public Cart() {
        this.id = ++cartCounter;
        this.items = new HashMap<>();
        this.notes = new HashMap<>();
        this.totalCost = 0;
    }

    public void addItem(MenuItem item) {
        items.put(item, items.getOrDefault(item, 0) + 1);
        totalCost += item.getPrice();
    }

    public void removeItem(MenuItem item) {
        if (items.containsKey(item)) {
            int quantity = items.get(item);
            if (quantity > 1) {
                items.put(item, quantity - 1);
            } else {
                items.remove(item);
                notes.remove(item);
            }
            totalCost -= item.getPrice();
        }
    }

    // ✅ Alias for removeItem (1 quantity)
    public void decreaseItem(MenuItem item) {
        removeItem(item);
    }

    // ✅ Completely remove item and note
    public void removeCompletely(MenuItem item) {
        if (items.containsKey(item)) {
            totalCost -= item.getPrice() * items.get(item);
            items.remove(item);
            notes.remove(item);
        }
    }

    public void deleteItemCompletely(MenuItem item) {
        removeCompletely(item); // alias
    }

    public void clearCart() {
        items.clear();
        notes.clear();
        totalCost = 0;
    }

    public double getSubtotal() {
        return totalCost;
    }

    public double getDeliveryFee() {
        return !items.isEmpty() && includeDeliveryFee ? DELIVERY_FEE : 0;
    }

    public double calculateTotal() {
        return getSubtotal() + getDeliveryFee();
    }

    public void setIncludeDeliveryFee(boolean include) {
        this.includeDeliveryFee = include;
    }

    public boolean isIncludeDeliveryFee() {
        return includeDeliveryFee;
    }

    public int getId() {
        return id;
    }

    public Map<MenuItem, Integer> getItems() {
        return items;
    }

    public double getTotalCost() {
        return totalCost;
    }

    public void setItems(Map<MenuItem, Integer> items) {
        this.items = items;
        this.totalCost = items.entrySet().stream()
                .mapToDouble(entry -> entry.getKey().getPrice() * entry.getValue())
                .sum();
    }

    // ✅ Notes per item
    public String getNote(MenuItem item) {
        return notes.getOrDefault(item, "");
    }

    public void setNote(MenuItem item, String note) {
        notes.put(item, note);
    }
}
