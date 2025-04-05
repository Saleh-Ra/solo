package il.cshaifasweng.OCSFMediatorExample.entities;

import javax.persistence.*;
import java.util.List;
import java.io.Serializable;

@Entity
@Table(name = "Restaurant_chain")
public class RestaurantChain implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false, unique = true)
    private String name;

    // Aggregation: A RestaurantChain can have multiple Branches, but Branches exist independently
    @OneToMany(mappedBy = "restaurantChain", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    private List<Branch> branches;

    public RestaurantChain() {}

    public RestaurantChain(String name, List<Branch> branches) {
        this.name = name;
        this.branches = branches;
    }

    // ✅ Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Branch> getBranches() {
        return branches;
    }

    public void setBranches(List<Branch> branches) {
        this.branches = branches;
    }
}
