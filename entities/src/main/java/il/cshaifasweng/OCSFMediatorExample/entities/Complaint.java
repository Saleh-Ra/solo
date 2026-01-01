package il.cshaifasweng.OCSFMediatorExample.entities;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "complaint")
public class Complaint implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false, length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    // ✅ One-to-One Association: A Client can have only ONE active Complaint at a time.
    @OneToOne
    @JoinColumn(name = "client_id", unique = true, nullable = false)
    private Client client;

    // ✅ Many Complaints belong to ONE Branch.
    @ManyToOne
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    // ✅ Many Complaints can be assigned to ONE CustomerService rep.
    // Nullable because complaints start unassigned
    @ManyToOne
    @JoinColumn(name = "customer_service_id", nullable = true)
    private CustomerService assignedCustomerService;

    // Optional field for manager/CS rep response/notes
    @Column(length = 1000)
    private String managerNotes;

    public Complaint() {}

    public Complaint(String description, Status status, Client client, Branch branch) {
        this.description = description;
        this.status = status;
        this.client = client;
        this.branch = branch;
    }

    public Complaint(String description, Status status, Client client, Branch branch, CustomerService assignedCustomerService) {
        this.description = description;
        this.status = status;
        this.client = client;
        this.branch = branch;
        this.assignedCustomerService = assignedCustomerService;
    }

    // ✅ Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
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

    public CustomerService getAssignedCustomerService() {
        return assignedCustomerService;
    }

    public void setAssignedCustomerService(CustomerService assignedCustomerService) {
        this.assignedCustomerService = assignedCustomerService;
    }

    public String getManagerNotes() {
        return managerNotes;
    }

    public void setManagerNotes(String managerNotes) {
        this.managerNotes = managerNotes;
    }

    // ✅ Enum for Complaint Status
    public enum Status {
        PENDING, RESOLVED, REJECTED
    }
}