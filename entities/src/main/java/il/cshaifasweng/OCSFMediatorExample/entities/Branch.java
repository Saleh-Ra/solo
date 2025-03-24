package il.cshaifasweng.OCSFMediatorExample.entities;

import java.util.ArrayList;

public class Branch {
    int BranchID;
    String Location;
    Menu menu;
    ArrayList<ResturantTable> tables;
    String OpeningHours;

    public Branch(int branchID, String location, Menu menu, ArrayList<ResturantTable> tables, String OpeningHours) {
        BranchID = branchID;
        Location = location;
        this.menu = menu;
        this.tables = tables;
        this.OpeningHours = OpeningHours;
    }

    public int getBranchID() {
        return BranchID;
    }

    public void setBranchID(int branchID) {
        BranchID = branchID;
    }

    public String getLocation() {
        return Location;
    }

    public void setLocation(String location) {
        Location = location;
    }

    public Menu getMenu() {
        return menu;
    }

    public void setMenu(Menu menu) {
        this.menu = menu;
    }

    public ArrayList<ResturantTable> getTables() {
        return tables;
    }

    public void setTables(ArrayList<ResturantTable> tables) {
        this.tables = tables;
    }

    public String getOpeningHours() {
        return OpeningHours;
    }

    public void setOpeningHours(String openingHours) {
        OpeningHours = openingHours;
    }
}
