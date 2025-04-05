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

    public UserAccount() {}

    //when a user creates an account it should default to client role
    public UserAccount(String name, String phoneNumber, String role, String password) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.role = role;
        this.password = password;
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
        return "manager".equalsIgnoreCase(role);
    }

    public String getPassword() {return password;}
    public void setPassword(String password) {this.password = password;}
    public String getName() {return name;}
    public void setName(String name) {this.name = name;}
}
