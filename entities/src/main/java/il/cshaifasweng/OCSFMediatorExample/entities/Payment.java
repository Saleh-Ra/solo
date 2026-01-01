package il.cshaifasweng.OCSFMediatorExample.entities;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "payment")
public class Payment implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private double amount;
    private String paymentMethod;
    private String status;

    // ✅ Many-to-One Association: Many Payments belong to one Client.
    @ManyToOne
    @JoinColumn(name = "client_id", nullable = true)
    private Client client;

    // ✅ One-to-One Association: One Payment is for one Order.
    @OneToOne
    @JoinColumn(name = "order_id", unique = true, nullable = true)
    private Order order;

    public Payment() {}

    public Payment(double amount, String paymentMethod, String status, Client client) {
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.client = client;
    }

    public Payment(double amount, String paymentMethod, String status, Client client, Order order) {
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.client = client;
        this.order = order;
    }

    // ✅ Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
        if (order != null && order.getPayment() != this) {
            order.setPayment(this);
        }
    }
}