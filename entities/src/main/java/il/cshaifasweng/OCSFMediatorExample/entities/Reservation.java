package il.cshaifasweng.OCSFMediatorExample.entities;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.io.Serializable;

@Entity
@javax.persistence.Table(name = "reservation")
public class Reservation implements Serializable {
    public static final int DEFAULT_DURATION_MINUTES = 90;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private LocalDateTime reservationTime;
    private int numberOfGuests;
    private String phoneNumber;

    // ✅ One-to-One Association: A Client can have only ONE active Reservation at a time.
    @ManyToOne
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    // ✅ Many Reservations belong to ONE Branch.
    @ManyToOne
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    // ✅ Many Reservations can include multiple Tables.
    @ManyToMany
    @JoinTable(
            name = "reservation_table",
            joinColumns = @JoinColumn(name = "reservation_id"),
            inverseJoinColumns = @JoinColumn(name = "table_id")
    )
    private List<RestaurantTable> tables;

    public Reservation() {}

    public Reservation(LocalDateTime reservationDate, int numberOfGuests, Client client, Branch branch, List<RestaurantTable> tables) {
        this.reservationTime = reservationDate;
        this.numberOfGuests = numberOfGuests;
        this.client = client;
        this.branch = branch;
        this.tables = tables;
        this.phoneNumber = client.getAccount().getPhoneNumber();
    }

    // Alternative constructor that takes a phone number instead of a client
    public Reservation(LocalDateTime reservationDate, int numberOfGuests, String phoneNumber, Branch branch, List<RestaurantTable> tables) {
        this.reservationTime = reservationDate;
        this.numberOfGuests = numberOfGuests;
        this.branch = branch;
        this.tables = tables;
        this.phoneNumber = phoneNumber;
        // client will be set separately
    }

    // ✅ Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public LocalDateTime getReservationTime() {
        return reservationTime;
    }

    public void setReservationTime(LocalDateTime reservationTime) {
        this.reservationTime = reservationTime;
    }

    public int getNumberOfGuests() {
        return numberOfGuests;
    }

    public void setNumberOfGuests(int numberOfGuests) {
        this.numberOfGuests = numberOfGuests;
    }
    
    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public Branch getBranch() {
        return branch;
    }

    public void setBranch(Branch branch) {
        this.branch = branch;
    }

    public List<RestaurantTable> getTables() {
        return tables;
    }

    public void setTables(List<RestaurantTable> tables) {
        this.tables = tables;
    }

    public int getDurationMinutes() {
        return DEFAULT_DURATION_MINUTES;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}