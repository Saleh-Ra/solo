package il.cshaifasweng.OCSFMediatorExample.entities;

import java.util.ArrayList;

public class Cart {

    private static int cartCounter = 0;
    private int id;
    private ArrayList<MenuItem> items;
    private double totalCost;

    public Cart() {
        this.id = ++cartCounter; // ✅ Fixed: `this.id`, not a local variable
        this.items = new ArrayList<>();
        this.totalCost = 0;
    }

    public void addItem(MenuItem item) {
        items.add(item);
        totalCost += item.getPrice();
    }

    public void removeItem(MenuItem item) {
        if (items.remove(item)) {
            totalCost -= item.getPrice();
        }
    }

    public void clearCart() {
        items.clear();
        totalCost = 0;
    }

    public double calculateTotal() {
        return totalCost;
    }

    // Getters and setters
    public int getId() {
        return id;
    }

    public ArrayList<MenuItem> getItems() {
        return items;
    }

    public double getTotalCost() {
        return totalCost;
    }

    public void setItems(ArrayList<MenuItem> items) {
        this.items = items;
        // Recalculate total cost if setting a new list
        this.totalCost = items.stream().mapToDouble(MenuItem::getPrice).sum();
    }
}
