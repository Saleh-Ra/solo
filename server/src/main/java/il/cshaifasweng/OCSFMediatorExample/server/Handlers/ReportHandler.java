package il.cshaifasweng.OCSFMediatorExample.server.Handlers;

import il.cshaifasweng.OCSFMediatorExample.entities.*;
import il.cshaifasweng.OCSFMediatorExample.server.SimpleServer;
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
     */
    private static String generateCustomReport(int branchId, String branchName, 
                                            String reportType, String startDateStr, String endDateStr) {
        try {
            // Parse dates (simplified for now)
            LocalDateTime startDate = LocalDateTime.parse(startDateStr);
            LocalDateTime endDate = LocalDateTime.parse(endDateStr);

            // Get orders in date range for this branch
            List<Order> allOrders = DataManager.fetchAll(Order.class);
            List<Order> branchOrders = new ArrayList<>();
            
            for (Order order : allOrders) {
                if (order.getBranchId() == branchId && 
                    order.getOrderTime().isAfter(startDate) && 
                    order.getOrderTime().isBefore(endDate)) {
                    branchOrders.add(order);
                }
            }

            // Calculate statistics
            int totalOrders = branchOrders.size();
            double totalRevenue = 0.0;
            
            for (Order order : branchOrders) {
                totalRevenue += order.getTotalCost();
            }

            // Format the custom report
            String report = String.format(
                "🏪 %s Branch - Custom Report\n" +
                "📅 Period: %s to %s\n" +
                "📊 Statistics:\n" +
                "   • Orders in Period: %d\n" +
                "   • Revenue in Period: $%.2f\n" +
                "   • Report Type: %s\n" +
                "   • Generated: %s",
                branchName,
                startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                endDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                totalOrders,
                totalRevenue,
                reportType,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
            );

            return report;
        } catch (Exception e) {
            return "Error generating custom report: " + e.getMessage();
        }
    }

    /**
     * Generate a chain-wide report for chain managers
     */
    private static String generateChainReport(String reportType, String startDateStr, String endDateStr) {
        try {
            // Parse dates (simplified for now)
            LocalDateTime startDate = LocalDateTime.parse(startDateStr);
            LocalDateTime endDate = LocalDateTime.parse(endDateStr);

            // Get all orders in date range across all branches
            List<Order> allOrders = DataManager.fetchAll(Order.class);
            List<Order> periodOrders = new ArrayList<>();
            
            for (Order order : allOrders) {
                if (order.getOrderTime().isAfter(startDate) && 
                    order.getOrderTime().isBefore(endDate)) {
                    periodOrders.add(order);
                }
            }

            // Calculate chain-wide statistics
            int totalOrders = periodOrders.size();
            double totalRevenue = 0.0;
            int pendingOrders = 0;
            
            for (Order order : periodOrders) {
                totalRevenue += order.getTotalCost();
                if ("Pending".equals(order.getStatus())) {
                    pendingOrders++;
                }
            }

            // Get all branches for visitor count
            List<Branch> allBranches = DataManager.fetchAll(Branch.class);
            int totalVisitors = 0;
            for (Branch branch : allBranches) {
                totalVisitors += branch.getMonthlyVisitCount();
            }

            // Format the chain report
            String report = String.format(
                "🏢 Restaurant Chain Report\n" +
                "📅 Period: %s to %s\n" +
                "📊 Chain-wide Statistics:\n" +
                "   • Total Orders: %d\n" +
                "   • Pending Orders: %d\n" +
                "   • Total Revenue: $%.2f\n" +
                "   • Total Monthly Visitors: %d\n" +
                "   • Number of Branches: %d\n" +
                "   • Report Type: %s\n" +
                "   • Generated: %s",
                startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                endDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                totalOrders,
                pendingOrders,
                totalRevenue,
                totalVisitors,
                allBranches.size(),
                reportType,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
            );

            return report;
        } catch (Exception e) {
            return "Error generating chain report: " + e.getMessage();
        }
    }
}
