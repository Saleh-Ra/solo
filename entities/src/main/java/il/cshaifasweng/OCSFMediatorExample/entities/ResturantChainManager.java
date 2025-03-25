package il.cshaifasweng.OCSFMediatorExample.entities;

import javax.persistence.*;

@Entity
@Table(name = "restaurant_chain_manager")
public class ResturantChainManager {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int managerId;

    private String managerName;

    // Default constructor required by Hibernate
    public ResturantChainManager() {}

    public ResturantChainManager(int managerId, String managerName) {
        this.managerId = managerId;
        this.managerName = managerName;
    }

    // Getters and setters
    public int getManagerId() {
        return managerId;
    }

    public void setManagerId(int managerId) {
        this.managerId = managerId;
    }

    public String getManagerName() {
        return managerName;
    }

    public void setManagerName(String managerName) {
        this.managerName = managerName;
    }
}
