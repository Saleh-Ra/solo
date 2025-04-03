package il.cshaifasweng.OCSFMediatorExample.entities;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "branch")
public class Branch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String location;
    private String openingHours;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")  // This will add a foreign key in RestaurantTable
    private List<RestaurantTable> tables;

    // Many branches belong to one restaurant chain (Aggregation)
    @ManyToOne
    @JoinColumn(name = "Restaurant_chain_id", nullable = true)
    private RestaurantChain restaurantChain;

    public Branch() {}

    public Branch(String location, String openingHours, /*RestaurantChain restaurantChain*/List<RestaurantTable> tables) {
        this.location = location;
        this.openingHours = openingHours;
        //this.restaurantChain = restaurantChain;
        this.tables = tables;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getOpeningHours() {
        return openingHours;
    }

    public void setOpeningHours(String openingHours) {
        this.openingHours = openingHours;
    }

    /*public RestaurantChain getRestaurantChain() {
        return restaurantChain;
    }

    public void setRestaurantChain(RestaurantChain restaurantChain) {
        this.restaurantChain = restaurantChain;
    }*/
    public List<RestaurantTable> getTables() {
        return tables;
    }
    public void setTables(List<RestaurantTable> tables) {}
}