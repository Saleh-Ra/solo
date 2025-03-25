package il.cshaifasweng.OCSFMediatorExample.entities;

import javax.persistence.*;

@Entity
@Table(name = "order_table")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private int branchId;
    private double totalCost;
    private String status;

    // ✅ Many-to-One Association: Many Orders belong to one Client.
    @ManyToOne
    @JoinColumn(name = "client_id", nullable = true)
    private Client client;

    public Order() {}

    public Order(int branchid,double totalCost, String status, Client client) {
        this.branchId = branchid;
        this.totalCost = totalCost;
        this.status = status;
        this.client = client;
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

    public void setBranchId(int id) {
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
}