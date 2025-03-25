package il.cshaifasweng.OCSFMediatorExample.entities;

import javax.persistence.*;

@Entity
@Table(name = "branch")
public class Branch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String location;
    private String openingHours;

    // Many branches belong to one restaurant chain (Aggregation)
    @ManyToOne
    @JoinColumn(name = "resturant_chain_id", nullable = true)
    private ResturantChain restaurantChain;

    public Branch() {}

    public Branch(String location, String openingHours, ResturantChain restaurantChain) {
        this.location = location;
        this.openingHours = openingHours;
        this.restaurantChain = restaurantChain;
    }

    // ✅ Getters and Setters
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

    public ResturantChain getRestaurantChain() {
        return restaurantChain;
    }

    public void setRestaurantChain(ResturantChain restaurantChain) {
        this.restaurantChain = restaurantChain;
    }
}