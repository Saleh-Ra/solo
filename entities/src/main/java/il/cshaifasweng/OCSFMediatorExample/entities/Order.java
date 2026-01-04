package il.cshaifasweng.OCSFMediatorExample.entities;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.io.Serializable;

@Entity
@Table(name = "order_table")
public class Order implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private int branchId;
    private double totalCost;
    @Column(nullable = false)
    private LocalDateTime orderTime;
    private String status;

    // Customer details
    private String customerName;
    private String phoneNumber;
    private String deliveryDate;
    private String deliveryTime;
    private String deliveryLocation;
    private String paymentMethod;
    
    // Order items and customizations (stored as formatted string)
    // Format: "itemName1:quantity1:note1|itemName2:quantity2:note2|..."
    @Column(length = 2000)
    private String orderItems;

    // ✅ Many-to-One Association: Many Orders belong to one Client.
    @ManyToOne
    @JoinColumn(name = "client_id", nullable = true)
    private Client client;

    // ✅ One-to-One Association: One Order has one Payment.
    // Payment owns the relationship via its `order` field.
    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Payment payment;

    // ✅ One-to-One Association: One Order has one Delivery.
    // Delivery owns the relationship via its `order` field.
    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Delivery delivery;

    public Order() {}

    public Order(int branchid, double totalCost, LocalDateTime time, Client client) {
        this.branchId = branchid;
        this.totalCost = totalCost;
        this.orderTime = time;
        this.client = client;
        this.status = "Pending";
    }

    // ✅ Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getBranchId() {
        return branchId;
    }

    public void setBranchId(int branchId) {
        this.branchId = branchId;
    }

    public double getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(double totalCost) {
        this.totalCost = totalCost;
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

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
        if (payment != null && payment.getOrder() != this) {
            payment.setOrder(this);
        }
    }

    public Delivery getDelivery() {
        return delivery;
    }

    public void setDelivery(Delivery delivery) {
        this.delivery = delivery;
        if (delivery != null && delivery.getOrder() != this) {
            delivery.setOrder(this);
        }
    }

    public LocalDateTime getOrderTime() {return this.orderTime;}

    public void setOrderTime(LocalDateTime orderTime) {this.orderTime=orderTime;}

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getDeliveryDate() {
        return deliveryDate;
    }

    public void setDeliveryDate(String deliveryDate) {
        this.deliveryDate = deliveryDate;
    }

    public String getDeliveryTime() {
        return deliveryTime;
    }

    public void setDeliveryTime(String deliveryTime) {
        this.deliveryTime = deliveryTime;
    }

    public String getDeliveryLocation() {
        return deliveryLocation;
    }

    public void setDeliveryLocation(String deliveryLocation) {
        this.deliveryLocation = deliveryLocation;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
    
    public String getOrderItems() {
        return orderItems;
    }
    
    public void setOrderItems(String orderItems) {
        this.orderItems = orderItems;
    }
}