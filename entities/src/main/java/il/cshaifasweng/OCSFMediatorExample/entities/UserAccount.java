package il.cshaifasweng.OCSFMediatorExample.entities;

import javax.persistence.*;

@Entity
@Table(name = "user_accounts")
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false, unique = true)
    private String username;

    private String password;
    private String phoneNumber;

    // true = manager, false = regular client
    private boolean isManager;

    public UserAccount() {
    }

    public UserAccount(String username, String password, String phoneNumber, boolean isManager) {
        this.username = username;
        this.password = password;
        this.phoneNumber = phoneNumber;
        this.isManager = isManager;
    }

    // Getters and setters
    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public boolean isManager() {
        return isManager;
    }

    public void setManager(boolean manager) {
        isManager = manager;
    }
}
