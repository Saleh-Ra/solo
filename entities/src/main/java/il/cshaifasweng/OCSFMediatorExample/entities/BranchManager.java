package il.cshaifasweng.OCSFMediatorExample.entities;

import javax.persistence.*;

@Entity
@Table(name = "branch_manager")
public class BranchManager {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int managerId;

    private String managerName;

    @OneToOne
    private Branch branch;

    @OneToOne
    @JoinColumn(name = "user_account_id", nullable = false)
    private UserAccount manager;

    // Default constructor required by Hibernate
    public BranchManager() {}

    public BranchManager(String managerName, Branch branch, UserAccount manager) {
        this.managerName = managerName;
        this.branch = branch;
        this.manager = manager;
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
    public Branch getBranch() {return branch;}
    public void setBranch(Branch branch) {this.branch = branch;}
    public UserAccount getManager() {return manager;}
    public void setManager(UserAccount manager) {this.manager = manager;}
}
