package il.cshaifasweng.OCSFMediatorExample.server.Handlers;

import il.cshaifasweng.OCSFMediatorExample.entities.*;
import il.cshaifasweng.OCSFMediatorExample.server.dataManagers.DataManager;
import il.cshaifasweng.OCSFMediatorExample.server.ocsf.ConnectionToClient;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ReportHandler {

    /**
     * Handle GET_REPORTS request - return existing reports for the user's branch
     */
    public static void handleGetReports(ConnectionToClient client) {
        try {
            // Get user info to determine branch
            Object clientInfo = client.getInfo("user");
            if (clientInfo instanceof UserAccount) {
                UserAccount user = (UserAccount) clientInfo;
                
                if ("manager".equalsIgnoreCase(user.getRole())) {
                    if (user.getBranchId() != null) {
                        // Branch manager: get reports for their branch
                        int branchId = user.getBranchId();
                        String branchName = user.getBranchName();
                        
                        // Generate current branch report
                        String report = generateBranchReport(branchId, branchName);
                        
                        // Send the report as a simple string
                        client.sendToClient("REPORTS_DATA;" + report);
                        System.out.println("Sent branch report to manager: " + user.getName());
                    } else {
                        // Chain manager (like Saleh): get chain-wide report
                        String report = generateChainReport("SUMMARY", 
                            LocalDateTime.now().minusDays(30).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")),
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS")));
                        
                        // Send the report as a simple string
                        client.sendToClient("REPORTS_DATA;" + report);
                        System.out.println("Sent chain report to chain manager: " + user.getName());
                    }
                } else if ("chain_manager".equalsIgnoreCase(user.getRole())) {
                    // Chain manager: get chain-wide report
                    String report = generateChainReport("SUMMARY", 
                        LocalDateTime.now().minusDays(30).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")),
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS")));
                    
                    // Send the report as a simple string
                    client.sendToClient("REPORTS_DATA;" + report);
                    System.out.println("Sent chain report to chain manager: " + user.getName());
                } else {
                    // Regular user: no reports available
                    client.sendToClient("REPORTS_DATA;No reports available for regular users");
                }
            } else {
                client.sendToClient("REPORTS_DATA;User information not available");
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                client.sendToClient("REPORTS_ERROR;Failed to generate reports: " + e.getMessage());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Handle GENERATE_REPORT request - generate a new report
     */
    public static void handleGenerateReport(String msgString, ConnectionToClient client) {
        try {
            // Format: GENERATE_REPORT;reportType;startDate;endDate
            String[] parts = msgString.split(";");
            if (parts.length < 4) {
                client.sendToClient("REPORT_ERROR;Invalid format for report generation");
                return;
            }

            String reportType = parts[1];
            String startDateStr = parts[2];
            String endDateStr = parts[3];

            // Get user info to determine branch
            Object clientInfo = client.getInfo("user");
            if (clientInfo instanceof UserAccount) {
                UserAccount user = (UserAccount) clientInfo;
                
                if ("manager".equalsIgnoreCase(user.getRole())) {
                    if (user.getBranchId() != null) {
                        // Branch manager: generate branch-specific report
                        String report = generateCustomReport(user.getBranchId(), user.getBranchName(), 
                                                          reportType, startDateStr, endDateStr);
                        
                        client.sendToClient("REPORT_GENERATED;" + report);
                        System.out.println("Generated custom report for branch manager: " + user.getName());
                    } else {
                        // Chain manager (like Saleh): generate chain-wide report
                        String report = generateChainReport(reportType, startDateStr, endDateStr);
                        
                        client.sendToClient("REPORT_GENERATED;" + report);
                        System.out.println("Generated chain report for chain manager: " + user.getName());
                    }
                } else if ("chain_manager".equalsIgnoreCase(user.getRole())) {
                    // Chain manager: generate chain-wide report
                    String report = generateChainReport(reportType, startDateStr, endDateStr);
                    
                    client.sendToClient("REPORT_GENERATED;" + report);
                    System.out.println("Generated chain report for chain manager: " + user.getName());
                } else {
                    client.sendToClient("REPORT_ERROR;Only managers and chain managers can generate reports");
                }
            } else {
                client.sendToClient("REPORT_ERROR;User information not available");
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                client.sendToClient("REPORT_ERROR;Failed to generate report: " + e.getMessage());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Handle EXPORT_REPORT request
     */
    public static void handleExportReport(String msgString, ConnectionToClient client) {
        try {
            // For now, just send a success message
            client.sendToClient("REPORT_EXPORTED;Report export functionality coming soon");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Generate a simple branch report with current statistics
     */
    private static String generateBranchReport(int branchId, String branchName) {
        try {
            // Get all orders for this branch
            List<Order> allOrders = DataManager.fetchAll(Order.class);
            List<Order> branchOrders = new ArrayList<>();
            
            for (Order order : allOrders) {
                if (order.getBranchId() == branchId) {
                    branchOrders.add(order);
                }
            }

            // Calculate statistics
            int totalOrders = branchOrders.size();
            double totalRevenue = 0.0;
            int pendingOrders = 0;
            
            for (Order order : branchOrders) {
                totalRevenue += order.getTotalCost();
                if ("Pending".equals(order.getStatus())) {
                    pendingOrders++;
                }
            }

            // Get visitor count (from branch)
            Branch branch = DataManager.fetchByField(Branch.class, "id", branchId).get(0);
            int visitorCount = branch.getMonthlyVisitCount();

            // Format the report
            String report = String.format(
                "🏪 %s Branch Report\n" +
                "📊 Current Statistics:\n" +
                "   • Total Orders: %d\n" +
                "   • Pending Orders: %d\n" +
                "   • Total Revenue: $%.2f\n" +
                "   • Monthly Visitors: %d\n" +
                "   • Generated: %s",
                branchName,
                totalOrders,
                pendingOrders,
                totalRevenue,
                visitorCount,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
            );

            return report;
        } catch (Exception e) {
            return "Error generating report: " + e.getMessage();
        }
    }

    /**
     * Generate a custom report based on parameters
     * Shows actual list of orders/reservations/complaints based on report type
     */
    private static String generateCustomReport(int branchId, String branchName, 
                                            String reportType, String startDateStr, String endDateStr) {
        try {
            // Parse dates
            LocalDateTime startDate = LocalDateTime.parse(startDateStr);
            LocalDateTime endDate = LocalDateTime.parse(endDateStr);

            // Parse report type
            Report.ReportType type;
            try {
                type = Report.ReportType.valueOf(reportType.toUpperCase());
            } catch (IllegalArgumentException e) {
                return "Error: Invalid report type: " + reportType;
            }

            StringBuilder report = new StringBuilder();
            report.append(String.format("🏪 %s Branch Report\n", branchName));
            report.append(String.format("📅 Period: %s to %s\n", 
                startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                endDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))));
            report.append(String.format("📊 Report Type: %s\n\n", type));

            switch (type) {
                case ORDERS:
                    report.append(generateOrdersReport(branchId, startDate, endDate));
                    break;
                case RESERVATIONS:
                    report.append(generateReservationsReport(branchId, startDate, endDate));
                    break;
                case COMPLAINTS:
                    report.append(generateComplaintsReport(branchId, startDate, endDate));
                    break;
                default:
                    return "Error: Report type " + type + " is not supported for branch managers.";
            }

            report.append(String.format("\n📅 Generated: %s", 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));

            return report.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error generating custom report: " + e.getMessage();
        }
    }

    /**
     * Generate orders report with actual order list
     */
    private static String generateOrdersReport(int branchId, LocalDateTime startDate, LocalDateTime endDate) {
        StringBuilder report = new StringBuilder();
        
        // Get all orders for this branch in date range
        List<Order> allOrders = DataManager.fetchAll(Order.class);
        List<Order> branchOrders = new ArrayList<>();
        
        for (Order order : allOrders) {
            LocalDateTime orderTime = order.getOrderTime();
            if (order.getBranchId() == branchId && 
                !orderTime.isBefore(startDate) && 
                !orderTime.isAfter(endDate)) {
                branchOrders.add(order);
            }
        }

        // Sort by date (newest first)
        branchOrders.sort((o1, o2) -> o2.getOrderTime().compareTo(o1.getOrderTime()));

        // Calculate statistics
        int totalOrders = branchOrders.size();
        double totalRevenue = 0.0;
        int pendingOrders = 0;
        int completedOrders = 0;
        
        for (Order order : branchOrders) {
            totalRevenue += order.getTotalCost();
            if ("Pending".equals(order.getStatus())) {
                pendingOrders++;
            } else {
                completedOrders++;
            }
        }

        report.append("📦 Orders Report\n");
        report.append(String.format("   • Total Orders: %d\n", totalOrders));
        report.append(String.format("   • Pending: %d\n", pendingOrders));
        report.append(String.format("   • Completed: %d\n", completedOrders));
        report.append(String.format("   • Total Revenue: $%.2f\n\n", totalRevenue));

        if (branchOrders.isEmpty()) {
            report.append("No orders found in this period.\n");
        } else {
            report.append("📋 Order Details:\n");
            for (Order order : branchOrders) {
                String orderTime = order.getOrderTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                report.append(String.format("   • Order #%d - %s - $%.2f - %s - %s\n",
                    order.getId(),
                    order.getCustomerName(),
                    order.getTotalCost(),
                    order.getStatus(),
                    orderTime));
            }
        }

        return report.toString();
    }

    /**
     * Generate reservations report with actual reservation list
     */
    private static String generateReservationsReport(int branchId, LocalDateTime startDate, LocalDateTime endDate) {
        StringBuilder report = new StringBuilder();

        // Get all reservations for this branch in date range
        List<Reservation> allReservations = DataManager.fetchAll(Reservation.class);
        List<Reservation> branchReservations = new ArrayList<>();
        
        for (Reservation reservation : allReservations) {
            if (reservation.getBranch() != null && reservation.getBranch().getId() == branchId) {
                LocalDateTime resTime = reservation.getReservationTime();
                if (!resTime.isBefore(startDate) && !resTime.isAfter(endDate)) {
                    branchReservations.add(reservation);
                }
            }
        }

        // Sort by date (newest first)
        branchReservations.sort((r1, r2) -> r2.getReservationTime().compareTo(r1.getReservationTime()));

        // Calculate statistics
        int totalReservations = branchReservations.size();
        int activeReservations = 0;
        int pastReservations = 0;
        LocalDateTime now = LocalDateTime.now();
        
        for (Reservation reservation : branchReservations) {
            if (reservation.getReservationTime().isAfter(now)) {
                activeReservations++;
            } else {
                pastReservations++;
            }
        }

        report.append("📅 Reservations Report\n");
        report.append(String.format("   • Total Reservations: %d\n", totalReservations));
        report.append(String.format("   • Active (Future): %d\n", activeReservations));
        report.append(String.format("   • Past: %d\n\n", pastReservations));

        if (branchReservations.isEmpty()) {
            report.append("No reservations found in this period.\n");
        } else {
            report.append("📋 Reservation Details:\n");
            for (Reservation reservation : branchReservations) {
                String resTime = reservation.getReservationTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                String clientName = reservation.getClient() != null ? reservation.getClient().getName() : "Unknown";
                report.append(String.format("   • Reservation #%d - %s - %d guests - %s\n",
                    reservation.getId(),
                    clientName,
                    reservation.getNumberOfGuests(),
                    resTime));
            }
        }

        return report.toString();
    }

    /**
     * Generate complaints report with actual complaint list
     */
    private static String generateComplaintsReport(int branchId, LocalDateTime startDate, LocalDateTime endDate) {
        StringBuilder report = new StringBuilder();
        
        // Get all complaints for this branch
        // Note: Complaints don't have a timestamp field, so we'll use all complaints for the branch
        // If you want date filtering, we'd need to add a timestamp to Complaint entity
        List<Complaint> allComplaints = DataManager.fetchAll(Complaint.class);
        List<Complaint> branchComplaints = new ArrayList<>();
        
        for (Complaint complaint : allComplaints) {
            if (complaint.getBranch() != null && complaint.getBranch().getId() == branchId) {
                branchComplaints.add(complaint);
            }
        }

        // Sort by ID (newest first, assuming higher ID = newer)
        branchComplaints.sort((c1, c2) -> Integer.compare(c2.getId(), c1.getId()));

        // Calculate statistics
        int totalComplaints = branchComplaints.size();
        int pendingComplaints = 0;
        int resolvedComplaints = 0;
        int rejectedComplaints = 0;
        
        for (Complaint complaint : branchComplaints) {
            switch (complaint.getStatus()) {
                case PENDING:
                    pendingComplaints++;
                    break;
                case RESOLVED:
                    resolvedComplaints++;
                    break;
                case REJECTED:
                    rejectedComplaints++;
                    break;
            }
        }

        report.append("📝 Complaints Report\n");
        report.append(String.format("   • Total Complaints: %d\n", totalComplaints));
        report.append(String.format("   • Pending: %d\n", pendingComplaints));
        report.append(String.format("   • Resolved: %d\n", resolvedComplaints));
        report.append(String.format("   • Rejected: %d\n\n", rejectedComplaints));

        if (branchComplaints.isEmpty()) {
            report.append("No complaints found for this branch.\n");
        } else {
            report.append("📋 Complaint Details:\n");
            for (Complaint complaint : branchComplaints) {
                String clientName = complaint.getClient() != null ? complaint.getClient().getName() : "Unknown";
                String description = complaint.getDescription();
                if (description.length() > 50) {
                    description = description.substring(0, 50) + "...";
                }
                report.append(String.format("   • Complaint #%d - %s - %s - %s\n",
                    complaint.getId(),
                    clientName,
                    complaint.getStatus(),
                    description));
                if (complaint.getManagerNotes() != null && !complaint.getManagerNotes().isEmpty()) {
                    report.append(String.format("      Response: %s\n", complaint.getManagerNotes()));
                }
            }
        }

        return report.toString();
    }

    /**
     * Generate a chain-wide report for chain managers
     * Shows actual list of orders/reservations/complaints across all branches
     */
    private static String generateChainReport(String reportType, String startDateStr, String endDateStr) {
        try {
            // Parse dates
            LocalDateTime startDate = LocalDateTime.parse(startDateStr);
            LocalDateTime endDate = LocalDateTime.parse(endDateStr);

            // Parse report type
            Report.ReportType type;
            try {
                type = Report.ReportType.valueOf(reportType.toUpperCase());
            } catch (IllegalArgumentException e) {
                return "Error: Invalid report type: " + reportType;
            }

            StringBuilder report = new StringBuilder();
            report.append("🏢 Restaurant Chain Report\n");
            report.append(String.format("📅 Period: %s to %s\n", 
                startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                endDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))));
            report.append(String.format("📊 Report Type: %s\n\n", type));

            switch (type) {
                case ORDERS:
                    report.append(generateChainOrdersReport(startDate, endDate));
                    break;
                case RESERVATIONS:
                    report.append(generateChainReservationsReport(startDate, endDate));
                    break;
                case COMPLAINTS:
                    report.append(generateChainComplaintsReport());
                    break;
                default:
                    return "Error: Report type " + type + " is not yet supported.";
            }

            report.append(String.format("\n📅 Generated: %s", 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));

            return report.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error generating chain report: " + e.getMessage();
        }
    }

    /**
     * Generate chain-wide orders report
     */
    private static String generateChainOrdersReport(LocalDateTime startDate, LocalDateTime endDate) {
        StringBuilder report = new StringBuilder();
        
        // Get all orders in date range across all branches
        List<Order> allOrders = DataManager.fetchAll(Order.class);
        List<Order> periodOrders = new ArrayList<>();
        
        for (Order order : allOrders) {
            LocalDateTime orderTime = order.getOrderTime();
            if (!orderTime.isBefore(startDate) && !orderTime.isAfter(endDate)) {
                periodOrders.add(order);
            }
        }

        // Sort by date (newest first)
        periodOrders.sort((o1, o2) -> o2.getOrderTime().compareTo(o1.getOrderTime()));

        // Calculate chain-wide statistics
        int totalOrders = periodOrders.size();
        double totalRevenue = 0.0;
        int pendingOrders = 0;
        int completedOrders = 0;
        
        // Group by branch
        Map<Integer, Integer> ordersByBranch = new HashMap<>();
        Map<Integer, Double> revenueByBranch = new HashMap<>();
        
        for (Order order : periodOrders) {
            totalRevenue += order.getTotalCost();
            if ("Pending".equals(order.getStatus())) {
                pendingOrders++;
            } else {
                completedOrders++;
            }
            
            int branchId = order.getBranchId();
            ordersByBranch.put(branchId, ordersByBranch.getOrDefault(branchId, 0) + 1);
            revenueByBranch.put(branchId, revenueByBranch.getOrDefault(branchId, 0.0) + order.getTotalCost());
        }

        // Get all branches
        List<Branch> allBranches = DataManager.fetchAll(Branch.class);
        int totalVisitors = 0;
        for (Branch branch : allBranches) {
            totalVisitors += branch.getMonthlyVisitCount();
        }

        report.append("📦 Chain-wide Orders Report\n");
        report.append(String.format("   • Total Orders: %d\n", totalOrders));
        report.append(String.format("   • Pending: %d\n", pendingOrders));
        report.append(String.format("   • Completed: %d\n", completedOrders));
        report.append(String.format("   • Total Revenue: $%.2f\n", totalRevenue));
        report.append(String.format("   • Total Monthly Visitors: %d\n", totalVisitors));
        report.append(String.format("   • Number of Branches: %d\n\n", allBranches.size()));

        // Show breakdown by branch
        report.append("📊 Orders by Branch:\n");
        for (Branch branch : allBranches) {
            int branchOrders = ordersByBranch.getOrDefault(branch.getId(), 0);
            double branchRevenue = revenueByBranch.getOrDefault(branch.getId(), 0.0);
            if (branchOrders > 0) {
                report.append(String.format("   • %s: %d orders, $%.2f revenue\n",
                    branch.getLocation(), branchOrders, branchRevenue));
            }
        }

        if (periodOrders.isEmpty()) {
            report.append("\nNo orders found in this period.\n");
        } else {
            report.append("\n📋 Order Details (showing first 50):\n");
            int count = 0;
            for (Order order : periodOrders) {
                if (count >= 50) {
                    report.append(String.format("   ... and %d more orders\n", periodOrders.size() - 50));
                    break;
                }
                String orderTime = order.getOrderTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                Branch branch = allBranches.stream()
                    .filter(b -> b.getId() == order.getBranchId())
                    .findFirst()
                    .orElse(null);
                String branchName = branch != null ? branch.getLocation() : "Unknown";
                report.append(String.format("   • Order #%d [%s] - %s - $%.2f - %s - %s\n",
                    order.getId(),
                    branchName,
                    order.getCustomerName(),
                    order.getTotalCost(),
                    order.getStatus(),
                    orderTime));
                count++;
            }
        }

        return report.toString();
    }

    /**
     * Generate chain-wide reservations report
     */
    private static String generateChainReservationsReport(LocalDateTime startDate, LocalDateTime endDate) {
        StringBuilder report = new StringBuilder();
        
        // Get all reservations in date range across all branches
        List<Reservation> allReservations = DataManager.fetchAll(Reservation.class);
        List<Reservation> periodReservations = new ArrayList<>();
        
        for (Reservation reservation : allReservations) {
            if (reservation.getBranch() != null) {
                LocalDateTime resTime = reservation.getReservationTime();
                if (!resTime.isBefore(startDate) && !resTime.isAfter(endDate)) {
                    periodReservations.add(reservation);
                }
            }
        }

        // Sort by date (newest first)
        periodReservations.sort((r1, r2) -> r2.getReservationTime().compareTo(r1.getReservationTime()));

        // Calculate statistics
        int totalReservations = periodReservations.size();
        int activeReservations = 0;
        int pastReservations = 0;
        LocalDateTime now = LocalDateTime.now();
        
        // Group by branch
        Map<Integer, Integer> reservationsByBranch = new HashMap<>();
        
        for (Reservation reservation : periodReservations) {
            if (reservation.getReservationTime().isAfter(now)) {
                activeReservations++;
            } else {
                pastReservations++;
            }
            
            int branchId = reservation.getBranch().getId();
            reservationsByBranch.put(branchId, reservationsByBranch.getOrDefault(branchId, 0) + 1);
        }

        report.append("📅 Chain-wide Reservations Report\n");
        report.append(String.format("   • Total Reservations: %d\n", totalReservations));
        report.append(String.format("   • Active (Future): %d\n", activeReservations));
        report.append(String.format("   • Past: %d\n\n", pastReservations));

        // Show breakdown by branch
        List<Branch> allBranches = DataManager.fetchAll(Branch.class);
        report.append("📊 Reservations by Branch:\n");
        for (Branch branch : allBranches) {
            int branchReservations = reservationsByBranch.getOrDefault(branch.getId(), 0);
            if (branchReservations > 0) {
                report.append(String.format("   • %s: %d reservations\n",
                    branch.getLocation(), branchReservations));
            }
        }

        if (periodReservations.isEmpty()) {
            report.append("\nNo reservations found in this period.\n");
        } else {
            report.append("\n📋 Reservation Details (showing first 50):\n");
            int count = 0;
            for (Reservation reservation : periodReservations) {
                if (count >= 50) {
                    report.append(String.format("   ... and %d more reservations\n", periodReservations.size() - 50));
                    break;
                }
                String resTime = reservation.getReservationTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                String clientName = reservation.getClient() != null ? reservation.getClient().getName() : "Unknown";
                String branchName = reservation.getBranch() != null ? reservation.getBranch().getLocation() : "Unknown";
                report.append(String.format("   • Reservation #%d [%s] - %s - %d guests - %s\n",
                    reservation.getId(),
                    branchName,
                    clientName,
                    reservation.getNumberOfGuests(),
                    resTime));
                count++;
            }
        }

        return report.toString();
    }

    /**
     * Generate chain-wide complaints report
     */
    private static String generateChainComplaintsReport() {
        StringBuilder report = new StringBuilder();
        
        // Get all complaints across all branches
        List<Complaint> allComplaints = DataManager.fetchAll(Complaint.class);

        // Sort by ID (newest first)
        allComplaints.sort((c1, c2) -> Integer.compare(c2.getId(), c1.getId()));

        // Calculate statistics
        int totalComplaints = allComplaints.size();
        int pendingComplaints = 0;
        int resolvedComplaints = 0;
        int rejectedComplaints = 0;
        
        // Group by branch
        Map<Integer, Integer> complaintsByBranch = new HashMap<>();
        
        for (Complaint complaint : allComplaints) {
            switch (complaint.getStatus()) {
                case PENDING:
                    pendingComplaints++;
                    break;
                case RESOLVED:
                    resolvedComplaints++;
                    break;
                case REJECTED:
                    rejectedComplaints++;
                    break;
            }
            
            if (complaint.getBranch() != null) {
                int branchId = complaint.getBranch().getId();
                complaintsByBranch.put(branchId, complaintsByBranch.getOrDefault(branchId, 0) + 1);
            }
        }

        report.append("📝 Chain-wide Complaints Report\n");
        report.append(String.format("   • Total Complaints: %d\n", totalComplaints));
        report.append(String.format("   • Pending: %d\n", pendingComplaints));
        report.append(String.format("   • Resolved: %d\n", resolvedComplaints));
        report.append(String.format("   • Rejected: %d\n\n", rejectedComplaints));

        // Show breakdown by branch
        List<Branch> allBranches = DataManager.fetchAll(Branch.class);
        report.append("📊 Complaints by Branch:\n");
        for (Branch branch : allBranches) {
            int branchComplaints = complaintsByBranch.getOrDefault(branch.getId(), 0);
            if (branchComplaints > 0) {
                report.append(String.format("   • %s: %d complaints\n",
                    branch.getLocation(), branchComplaints));
            }
        }

        if (allComplaints.isEmpty()) {
            report.append("\nNo complaints found.\n");
        } else {
            report.append("\n📋 Complaint Details (showing first 50):\n");
            int count = 0;
            for (Complaint complaint : allComplaints) {
                if (count >= 50) {
                    report.append(String.format("   ... and %d more complaints\n", allComplaints.size() - 50));
                    break;
                }
                String clientName = complaint.getClient() != null ? complaint.getClient().getName() : "Unknown";
                String description = complaint.getDescription();
                if (description.length() > 50) {
                    description = description.substring(0, 50) + "...";
                }
                String branchName = complaint.getBranch() != null ? complaint.getBranch().getLocation() : "Unknown";
                report.append(String.format("   • Complaint #%d [%s] - %s - %s - %s\n",
                    complaint.getId(),
                    branchName,
                    clientName,
                    complaint.getStatus(),
                    description));
                if (complaint.getManagerNotes() != null && !complaint.getManagerNotes().isEmpty()) {
                    report.append(String.format("      Response: %s\n", complaint.getManagerNotes()));
                }
                count++;
            }
        }

        return report.toString();
    }
}
