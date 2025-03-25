package il.cshaifasweng.OCSFMediatorExample.entities;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "menu")
public class Menu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int menuID;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "menu_id") // Creates a foreign key in MenuItem pointing to this menu
    private List<MenuItem> menuItems;

    // Default constructor required by Hibernate
    public Menu() {}

    public Menu(List<MenuItem> menuItems) {
        this.menuItems = menuItems;
    }

    // Getters and setters
    public int getMenuID() {
        return menuID;
    }

    public void setMenuID(int menuID) {
        this.menuID = menuID;
    }

    public List<MenuItem> getMenuItems() {
        return menuItems;
    }

    public void setMenuItems(List<MenuItem> menuItems) {
        this.menuItems = menuItems;
    }
}
