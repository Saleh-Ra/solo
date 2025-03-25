package il.cshaifasweng.OCSFMediatorExample.entities;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "reservation")
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private LocalDateTime reservationDate;
    private int numberOfGuests;

    // ✅ One-to-One Association: A Client can have only ONE active Reservation at a time.
    @OneToOne
    @JoinColumn(name = "client_id", unique = true, nullable = false)
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
    private List<ResturantTable> tables;

    public Reservation() {}

    public Reservation(LocalDateTime reservationDate, int numberOfGuests, Client client, Branch branch, List<ResturantTable> tables) {
        this.reservationDate = reservationDate;
        this.numberOfGuests = numberOfGuests;
        this.client = client;
        this.branch = branch;
        this.tables = tables;
    }

    // ✅ Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public LocalDateTime getReservationDate() {
        return reservationDate;
    }

    public void setReservationDate(LocalDateTime reservationDate) {
        this.reservationDate = reservationDate;
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

    public List<ResturantTable> getTables() {
        return tables;
    }

    public void setTables(List<ResturantTable> tables) {
        this.tables = tables;
    }
}