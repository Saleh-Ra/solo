package il.cshaifasweng.OCSFMediatorExample.entities;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "user_accounts")
public class UserAccount implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String name;

    @Column(nullable = false, unique = true)
    private String phoneNumber;
    // Changed from boolean isManager to String role
    @Column(name = "role", nullable = false)
    private String role;
    private String password;
    
    // Added branch information
    private Integer branchId;
    private String branchName;

    public UserAccount() {}

    //when a user creates an account it should default to client role with null branch
    public UserAccount(String name, String phoneNumber, String role, String password) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.role = role;
        this.password = password;
        // For clients, branch fields remain null by default
    }
    
    //constructor with branch info for managers
    public UserAccount(String name, String phoneNumber, String role, String password, Integer branchId, String branchName) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.role = role;
        this.password = password;
        this.branchId = branchId;
        this.branchName = branchName;
    }

    // Getters and setters
    public int getId() {
        return id;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isManager() {
        if (role == null) {return false;}
        return "manager".equalsIgnoreCase(role);
    }

    public String getPassword() {return password;}
    public void setPassword(String password) {this.password = password;}
    public String getName() {return name;}
    public void setName(String name) {this.name = name;}
    
    public Integer getBranchId() {
        return branchId;
    }
    
    public void setBranchId(Integer branchId) {
        this.branchId = branchId;
    }
    
    public String getBranchName() {
        return branchName;
    }
    
    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }
}
