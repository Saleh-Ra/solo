package il.cshaifasweng.OCSFMediatorExample.entities;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "reports")
public class Report implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private ReportType type;

    @Column(nullable = false)
    private LocalDateTime generatedAt;

    @Column(columnDefinition = "TEXT")
    private String content;

    @ManyToOne
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Column(name = "manager_phone", nullable = false)
    private String managerPhone;

    @Column(nullable = false)
    private LocalDateTime startDate;

    @Column(nullable = false)
    private LocalDateTime endDate;

    public static enum ReportType {
        SALES,
        ORDERS,
        RESERVATIONS,
        COMPLAINTS,
        PERFORMANCE,
        INVENTORY
    }

    // Default constructor required by JPA
    public Report() {
        this.generatedAt = LocalDateTime.now();
    }

    public Report(ReportType type, Branch branch, String managerPhone, LocalDateTime startDate, LocalDateTime endDate) {
        this.type = type;
        this.branch = branch;
        this.managerPhone = managerPhone;
        this.startDate = startDate;
        this.endDate = endDate;
        this.generatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public ReportType getType() {
        return type;
    }

    public void setType(ReportType type) {
        this.type = type;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Branch getBranch() {
        return branch;
    }

    public void setBranch(Branch branch) {
        this.branch = branch;
    }

    public String getManagerPhone() {
        return managerPhone;
    }

    public void setManagerPhone(String managerPhone) {
        this.managerPhone = managerPhone;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    @Override
    public String toString() {
        return String.format("Report{id=%d, type=%s, branch=%s, manager=%s, generatedAt=%s}", 
            id, type, branch != null ? branch.getLocation() : "N/A", managerPhone, generatedAt);
    }
} 