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
    // true = manager, false = regular client
    private boolean isManager;
    private String password;

    public UserAccount() {}

    //we don't ask if the user is a client.
    //the isManager will be true only in the initialized data, other than that, it will be a manager
    public UserAccount( String name,String phoneNumber, boolean isManager, String password) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.isManager = isManager;
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
    public String getPassword() {return password;}
    public void setPassword(String password) {this.password = password;}
    public String getName() {return name;}
    public void setName(String name) {this.name = name;}
}
