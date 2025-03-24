package il.cshaifasweng.OCSFMediatorExample.entities;

import java.util.ArrayList;

public class Menu {//menuid-int menuditems-list<menuItem>
    int MenuID;
    ArrayList<MenuItem> MenuItems;

    public Menu(int menuID, ArrayList<MenuItem> menuItems) {
        MenuID = menuID;
        MenuItems = menuItems;
    }

    public int getMenuID() {
        return MenuID;
    }

    public void setMenuID(int menuID) {
        MenuID = menuID;
    }

    public ArrayList<MenuItem> getMenuItems() {
        return MenuItems;
    }

    public void setMenuItems(ArrayList<MenuItem> menuItems) {
        MenuItems = menuItems;
    }
}
