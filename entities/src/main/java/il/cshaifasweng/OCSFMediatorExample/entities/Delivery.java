package il.cshaifasweng.OCSFMediatorExample.entities;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "delivery")
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int deliveryID;

    private int clientID;  // Optional: You can later replace with @ManyToOne to a Client entity if needed
    private int orderID;   // Optional: You can later replace with @ManyToOne to an Order entity if needed

    private String deliveryAddress;

    @Temporal(TemporalType.TIMESTAMP)
    private Date deliveryDate;

    private String status;

    // Default constructor (required by Hibernate)
    public Delivery() {}

    public Delivery(int clientID, int orderID, String deliveryAddress, Date deliveryDate, String status) {
        this.clientID = clientID;
        this.orderID = orderID;
        this.deliveryAddress = deliveryAddress;
        this.deliveryDate = deliveryDate;
        this.status = status;
    }

    // Getters and Setters
    public int getDeliveryID() {
        return deliveryID;
    }

    public void setDeliveryID(int deliveryID) {
        this.deliveryID = deliveryID;
    }

    public int getClientID() {
        return clientID;
    }

    public void setClientID(int clientID) {
        this.clientID = clientID;
    }

    public int getOrderID() {
        return orderID;
    }

    public void setOrderID(int orderID) {
        this.orderID = orderID;
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
}
