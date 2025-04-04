package il.cshaifasweng.OCSFMediatorExample.entities;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "client")
public class Client {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false)
    private String name;

    /*@Column(nullable = false, unique = true)
    private String contactDetails;*/

    @OneToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "user_account_id", nullable = false)
    private UserAccount account;

    // ✅ One-to-Many Association: A Client can have multiple Orders, but Orders remain if the Client is deleted.
    @OneToMany(mappedBy = "client", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    private List<Order> orders;

    // ✅ One-to-Many Association: A Client can have multiple Payments, but Payments remain if the Client is deleted.
    @OneToMany(mappedBy = "client", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    private List<Payment> payments;

    // ✅ One-to-One Association: A Client can have ONLY ONE active Reservation at a time.
    @OneToMany(mappedBy = "client", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    private List<Reservation> reservations;

    // ✅ One-to-One Association: A Client can have ONLY ONE active Complaint at a time.
    @OneToOne(mappedBy = "client", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    private Complaint complaint;

    public Client() {}

    public Client(String name, UserAccount account) {
        this.name = name;
        //this.contactDetails = contactDetails;
        this.account = account;
    }

    // ✅ Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /*public String getContactDetails() {
        return contactDetails;
    }

    public void setContactDetails(String contactDetails) {
        this.contactDetails = contactDetails;
    }*/
    public UserAccount getAccount() {return account;}
    public void setAccount(UserAccount account) {this.account = account;}

    public List<Order> getOrders() {
        return orders;
    }

    public void setOrders(List<Order> orders) {
        this.orders = orders;
    }

    public List<Payment> getPayments() {
        return payments;
    }

    public void setPayments(List<Payment> payments) {
        this.payments = payments;
    }

    public List<Reservation> getReservations() {
        return reservations;
    }

    public void setReservation(List<Reservation> reservations) {
        this.reservations = reservations;
    }

    public Complaint getComplaint() {
        return complaint;
    }

    public void setComplaint(Complaint complaint) {
        this.complaint = complaint;
    }
}