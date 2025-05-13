package il.cshaifasweng.OCSFMediatorExample.entities;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.io.Serializable;

@Entity
@Table(name = "reservation")
public class Reservation implements Serializable {
    public static final int DEFAULT_DURATION_MINUTES = 90; // 1.5 hours
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false)
    private LocalDateTime reservationTime; // Start time
    
    @Column(nullable = false)
    private LocalDateTime endTime; // End time (1.5 hours after start by default)

    private int numberOfGuests;
    private String phoneNumber;
    
    // Specific table ID for this reservation
    private int tableId;

    // ✅ Many-to-One Association: Many reservations belong to one branch.
    @ManyToOne
    @JoinColumn(name = "branch_id")
    private Branch branch;

    // ✅ Many-to-One Association: Many reservations can belong to one client.
    @ManyToOne
    @JoinColumn(name = "client_id")
    private Client client;

    // ✅ Many-to-Many Association: Many reservations can include many tables.
    @ManyToMany
    @JoinTable(name = "reservation_tables",
            joinColumns = @JoinColumn(name = "reservation_id"),
            inverseJoinColumns = @JoinColumn(name = "table_id"))
    private List<RestaurantTable> tables;

    public Reservation() {
    }

    public Reservation(LocalDateTime reservationTime, int numberOfGuests, String phoneNumber, Branch branch, List<RestaurantTable> tables) {
        this.reservationTime = reservationTime;
        this.endTime = reservationTime.plusMinutes(DEFAULT_DURATION_MINUTES);
        this.numberOfGuests = numberOfGuests;
        this.phoneNumber = phoneNumber;
        this.branch = branch;
        this.tables = tables;
        
        // If we have a specific table assigned, store its ID
        if (tables != null && !tables.isEmpty()) {
            this.tableId = tables.get(0).getid();
        }
    }
    
    public Reservation(LocalDateTime reservationTime, LocalDateTime endTime, int numberOfGuests, 
                      String phoneNumber, Branch branch, int tableId) {
        this.reservationTime = reservationTime;
        this.endTime = endTime;
        this.numberOfGuests = numberOfGuests;
        this.phoneNumber = phoneNumber;
        this.branch = branch;
        this.tableId = tableId;
    }

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
    
    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public int getNumberOfGuests() {
        return numberOfGuests;
    }

    public void setNumberOfGuests(int numberOfGuests) {
        this.numberOfGuests = numberOfGuests;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Branch getBranch() {
        return branch;
    }

    public void setBranch(Branch branch) {
        this.branch = branch;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public List<RestaurantTable> getTables() {
        return tables;
    }

    public void setTables(List<RestaurantTable> tables) {
        this.tables = tables;
        
        // If we have a specific table assigned, store its ID
        if (tables != null && !tables.isEmpty()) {
            this.tableId = tables.get(0).getid();
        }
    }
    
    public int getTableId() {
        return tableId;
    }
    
    public void setTableId(int tableId) {
        this.tableId = tableId;
    }
    
    /**
     * Checks if this reservation overlaps with the given time window
     * @param startTime The start time to check
     * @param endTime The end time to check
     * @return true if there is an overlap, false otherwise
     */
    public boolean overlaps(LocalDateTime startTime, LocalDateTime endTime) {
        // Reservations overlap if one starts before the other ends and ends after the other starts
        return this.reservationTime.isBefore(endTime) && this.endTime.isAfter(startTime);
    }
}