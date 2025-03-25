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

    @Column(nullable = false, unique = true)
    private String contactDetails;

    // ✅ One-to-Many Association: A Client can have multiple Orders, but Orders remain if the Client is deleted.
    @OneToMany(mappedBy = "client", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    private List<Order> orders;

    // ✅ One-to-Many Association: A Client can have multiple Payments, but Payments remain if the Client is deleted.
    @OneToMany(mappedBy = "client", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    private List<Payment> payments;

    // ✅ One-to-One Association: A Client can have ONLY ONE active Reservation at a time.
    @OneToOne(mappedBy = "client", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    private Reservation reservation;

    // ✅ One-to-One Association: A Client can have ONLY ONE active Complaint at a time.
    @OneToOne(mappedBy = "client", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    private Complaint complaint;

    public Client() {}

    public Client(String name, String contactDetails) {
        this.name = name;
        this.contactDetails = contactDetails;
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

    public String getContactDetails() {
        return contactDetails;
    }

    public void setContactDetails(String contactDetails) {
        this.contactDetails = contactDetails;
    }

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

    public Reservation getReservation() {
        return reservation;
    }

    public void setReservation(Reservation reservation) {
        this.reservation = reservation;
    }

    public Complaint getComplaint() {
        return complaint;
    }

    public void setComplaint(Complaint complaint) {
        this.complaint = complaint;
    }
}