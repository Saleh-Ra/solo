
package il.cshaifasweng.OCSFMediatorExample.entities;
import javax.persistence.*;
import java.util.Date;


@Entity
@Table(name = "delivery")
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    // One-to-One with Order (each delivery is for one order)
    @OneToOne
    @JoinColumn(name = "order_id")
    private Order order;

    // Many-to-One with Client (a client can have multiple deliveries)
    @ManyToOne
    @JoinColumn(name = "client_id")
    private Client client;

    private String deliveryAddress;

    @Temporal(TemporalType.TIMESTAMP)
    private Date deliveryDate;

    private String status; // e.g., "Pending", "In Transit", "Delivered"

    private String notes; // Optional: to hold custom instructions

    // Default constructor
    public Delivery() {}

    // Constructor using objects
    public Delivery(Order order, Client client, String deliveryAddress, Date deliveryDate, String status, String notes) {
        this.order = order;
        this.client = client;
        this.deliveryAddress = deliveryAddress;
        this.deliveryDate = deliveryDate;
        this.status = status;
        this.notes = notes;
    }

    // Additional constructor accepting int IDs (converts to objects)
    public Delivery(int clientId, int orderId, String deliveryAddress, Date deliveryDate, String status) {
        // Use your DataManager (or another lookup mechanism) to retrieve the objects:

        this.deliveryAddress = deliveryAddress;
        this.deliveryDate = deliveryDate;
        this.status = status;
        this.notes = "";
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public Date getDeliveryDate() {
        return deliveryDate;
    }

    public void setDeliveryDate(Date deliveryDate) {
        this.deliveryDate = deliveryDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
