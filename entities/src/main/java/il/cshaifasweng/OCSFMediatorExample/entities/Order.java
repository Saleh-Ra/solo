package il.cshaifasweng.OCSFMediatorExample.entities;

import java.util.ArrayList;

public class Order {// totalcost  status-string
    int OrderID;
    int ClientID;
    int BranchID;
    ArrayList<MenuItem> MenuItems;
    int TotalCost;
    String Status;

    public Order(int orderID, int clientID, int branchID, ArrayList<MenuItem> menuItems, int totalCost, String status) {
        OrderID = orderID;
        ClientID = clientID;
        BranchID = branchID;
        MenuItems = menuItems;
        TotalCost = totalCost;
        Status = status;
    }

    public int getOrderID() {
        return OrderID;
    }

    public void setOrderID(int orderID) {
        OrderID = orderID;
    }

    public int getClientID() {
        return ClientID;
    }

    public void setClientID(int clientID) {
        ClientID = clientID;
    }

    public int getBranchID() {
        return BranchID;
    }

    public void setBranchID(int branchID) {
        BranchID = branchID;
    }

    public ArrayList<MenuItem> getMenuItems() {
        return MenuItems;
    }

    public void setMenuItems(ArrayList<MenuItem> menuItems) {
        this.MenuItems = menuItems;
    }

    public int getTotalCost() {
        return TotalCost;
    }

    public void setTotalCost(int totalCost) {
        TotalCost = totalCost;
    }

    public String getStatus() {
        return Status;
    }

    public void setStatus(String status) {
        Status = status;
    }
}
