package il.cshaifasweng.OCSFMediatorExample.entities;

import javax.persistence.*;

@Entity
@Table(name = "user_accounts")
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String phoneNumber;

    // true = manager, false = regular client
    private boolean isManager;

    private String username;

    private String password;

    public UserAccount() {}

    public UserAccount( String phoneNumber, boolean isManager) {
        this.phoneNumber = phoneNumber;
        this.isManager = isManager;
    }

    public UserAccount( String phoneNumber, boolean isManager, String username, String password) {
        this.phoneNumber = phoneNumber;
        this.isManager = isManager;
        this.username = username;
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

    public boolean isManager() {
        return isManager;
    }

    public void setManager(boolean manager) {
        isManager = manager;
    }
}
