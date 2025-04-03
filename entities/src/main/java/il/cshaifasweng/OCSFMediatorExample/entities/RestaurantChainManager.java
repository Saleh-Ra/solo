package il.cshaifasweng.OCSFMediatorExample.entities;

import javax.persistence.*;

@Entity
@Table(name = "restaurant_chain_manager")
public class RestaurantChainManager {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int managerId;

    private String managerName;

    @OneToOne
    @JoinColumn(name = "user_account_id", nullable = false)
    private UserAccount account;


    // Default constructor required by Hibernate
    public RestaurantChainManager() {}

    public RestaurantChainManager(String managerName, UserAccount account) {
        this.managerName = managerName;
        this.account = account;
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

    public UserAccount getAccount() {return this.account;}

    public void setAccount(UserAccount account) {this.account = account;}
}
