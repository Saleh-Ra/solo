package il.cshaifasweng.OCSFMediatorExample.entities;

import java.util.ArrayList;

public class Cart {

    private static int cartCounter = 0;
    private int id;
    private ArrayList<MenuItem> items;
    private int sum;
    public Cart() {
        int id=++cartCounter;
        this.items = new ArrayList<>();
        this.sum = 0;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public ArrayList<MenuItem> getItems() {
        return items;
    }

    public void setItems(ArrayList<MenuItem> items) {
        this.items = items;
    }

    public int getSum() {
        return sum;
    }

    public void setSum(int sum) {
        this.sum = sum;
    }
}
