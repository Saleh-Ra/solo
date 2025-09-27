package il.cshaifasweng.OCSFMediatorExample.server.Handlers;

import il.cshaifasweng.OCSFMediatorExample.entities.Branch;
import il.cshaifasweng.OCSFMediatorExample.entities.Client;
import il.cshaifasweng.OCSFMediatorExample.entities.Reservation;
import il.cshaifasweng.OCSFMediatorExample.entities.RestaurantTable;
import il.cshaifasweng.OCSFMediatorExample.entities.TableAvailabilityInfo;
import il.cshaifasweng.OCSFMediatorExample.entities.UserAccount;
import il.cshaifasweng.OCSFMediatorExample.server.SimpleServer;
import il.cshaifasweng.OCSFMediatorExample.server.dataManagers.DataManager;
import il.cshaifasweng.OCSFMediatorExample.server.dataManagers.Database;
import il.cshaifasweng.OCSFMediatorExample.server.ocsf.ConnectionToClient;
import org.hibernate.annotations.Table;
import org.hibernate.Hibernate;
import org.hibernate.Session;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationHandler {

    //let's do this: the customer presses on a table reservation button on the main page
    //then he chooses branch->how many people->where to sit->what time->how much time you staying
    //after he is done with this he presses "make reservation" so the server register him
    //if the table (or table that match the description) is occupied, the app will show the available times for the tables
    //from there the client can pick what suits him, or press "go back"??

    /*public static void handleReserveRequest(String msgString, ConnectionToClient client) {
        //just like other requests, this method doesn't check anything, it is required to only add to the database
        // TODO: Parse reservation data, check availability, and reserve
        //the message will probably be {command;required branch; number of guests; where to sit(out of 4 options);time of arrival;}
    }*/
    public static void handleReserveRequest(String msgString, ConnectionToClient client) {
        System.out.println("Received reservation request: " + msgString);
        String[] parts = msgString.split(";");

        // Format: RESERVE_TABLE;branchId;guestCount;tableId;seatingPref;startDateTime;endDateTime;phoneNumber;location
        if (parts.length < 9) {
            System.out.println("Invalid format: Expected at least 9 parts, got " + parts.length);
            SimpleServer.sendFailureResponse(client, "RESERVATION_FAILURE", "Invalid format");
            return;
        }

        try {
            int branchId = Integer.parseInt(parts[1]);
            int guestCount = Integer.parseInt(parts[2]);
            int tableId = Integer.parseInt(parts[3]);
            String seatingPref = parts[4];
            LocalDateTime startTime = LocalDateTime.parse(parts[5], DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            LocalDateTime endTime = LocalDateTime.parse(parts[6], DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            String phoneNumber = parts[7];
            String location = parts[8];

            System.out.println("Processing reservation - Branch: " + branchId + 
                    ", Guests: " + guestCount + 
                    ", Table ID: " + tableId +
                    ", Start: " + startTime + 
                    ", End: " + endTime +
                    ", Phone: " + phoneNumber + 
                    ", Location: " + location);

            // Fetch branch with tables in one session to avoid lazy loading issues
            Branch branch = null;
            List<RestaurantTable> tables = new ArrayList<>();
            
            // Use a session to load branch and its tables
            Session session = Database.getSessionFactoryInstance().openSession();
            try {
                session.beginTransaction();
                
                // Get branch by ID and initialize tables collection
                branch = session.get(Branch.class, branchId);
                if (branch != null) {
                    Hibernate.initialize(branch.getTables());
                    tables = new ArrayList<>(branch.getTables());
                }
                
                session.getTransaction().commit();
            } catch (Exception e) {
                if (session.getTransaction() != null) {
                    session.getTransaction().rollback();
                }
                System.err.println("Error loading branch data: " + e.getMessage());
                e.printStackTrace();
                SimpleServer.sendFailureResponse(client, "RESERVATION_FAILURE", "Error loading branch data: " + e.getMessage());
                return;
            } finally {
                session.close();
            }
            
            if (branch == null) {
                System.out.println("Branch not found with ID: " + branchId);
                SimpleServer.sendFailureResponse(client, "RESERVATION_FAILURE", "Branch not found");
                return;
            }
            
            // Find the specific table by ID
            RestaurantTable targetTable = null;
            for (RestaurantTable table : tables) {
                if (table.getid() == tableId) {
                    targetTable = table;
                    break;
                }
            }
            
            if (targetTable == null) {
                System.out.println("Table not found with ID: " + tableId);
                SimpleServer.sendFailureResponse(client, "RESERVATION_FAILURE", "Table not found");
                return;
            }
            
            // Check if the table is already reserved during this time
            boolean isTableReserved = checkIfTableIsReserved(branchId, tableId, startTime, endTime);
            
            if (isTableReserved) {
                System.out.println("Table " + tableId + " is already reserved during this time");
                SimpleServer.sendFailureResponse(client, "RESERVATION_FAILURE", "Table is already reserved during this time");
                return;
            }
            
            List<UserAccount> userAccounts = DataManager.fetchUserAccountsByPhoneNumber(phoneNumber);
            
            UserAccount userAccount;
            if (userAccounts.isEmpty()) {
                System.out.println("No user account found with phone: " + phoneNumber + ". Creating new account.");
                // Create a new user account - for Guest client with null branch fields
                userAccount = new UserAccount("Guest", phoneNumber, "client", "password");
                // Branch fields remain null by default for client accounts
                DataManager.add(userAccount);
                System.out.println("Created new user account with ID: " + userAccount.getId());
            } else {
                userAccount = userAccounts.get(0);
                System.out.println("Found user account: " + userAccount.getName() + " with ID: " + userAccount.getId());
            }

            // Get or create a Client for this UserAccount
            List<Client> clients = DataManager.fetchClientsByPhoneNumber(phoneNumber);
            Client customer;
            
            if (clients.isEmpty()) {
                customer = new Client(userAccount.getName(), userAccount);
                if (customer.getReservations() == null) {
                    customer.setReservation(new ArrayList<>());
                }
                DataManager.add(customer);
            } else {
                customer = clients.get(0);
                if (customer.getReservations() == null) {
                    customer.setReservation(new ArrayList<>());
                }
            }
            
            // Create tables list with just this table
            List<RestaurantTable> tablesToAdd = new ArrayList<>();
            tablesToAdd.add(targetTable);
            
            // Create and save reservation in a single transaction
            Reservation newReservation = new Reservation(startTime, endTime, guestCount, phoneNumber, branch, tableId);
            newReservation.setClient(customer);
            newReservation.setTables(tablesToAdd);
            
            // Save both reservation and update client in a single transaction
            Session saveSession = Database.getSessionFactoryInstance().openSession();
            try {
                saveSession.beginTransaction();
                
                // First, reload the client to get fresh data
                Client freshClient = saveSession.get(Client.class, customer.getId());
                
                // Save the reservation
                saveSession.save(newReservation);
                System.out.println("Reservation saved with ID: " + newReservation.getId());

                // Add reservation to client's list in the same session
                if (freshClient.getReservations() == null) {
                    freshClient.setReservation(new ArrayList<>());
                }
                freshClient.getReservations().add(newReservation);
                
                saveSession.getTransaction().commit();
                System.out.println("Reservation and client updated in the same transaction");
            } catch (Exception e) {
                System.err.println("Error saving reservation: " + e.getMessage());
                e.printStackTrace();
                if (saveSession.getTransaction() != null && saveSession.getTransaction().isActive()) {
                    saveSession.getTransaction().rollback();
                }
                SimpleServer.sendFailureResponse(client, "RESERVATION_FAILURE", "Error saving reservation: " + e.getMessage());
                return;
            } finally {
                saveSession.close();
            }
            
            System.out.println("Reservation successfully created for phone: " + phoneNumber + " at " + branch.getLocation());
            
            // Update table status to show it's reserved (for immediate visual feedback)
            try (Session updateSession = Database.getSessionFactoryInstance().openSession()) {
                updateSession.beginTransaction();
                
                // Reload the table and set reservedID
                RestaurantTable tableToUpdate = updateSession.get(RestaurantTable.class, tableId);
                if (tableToUpdate != null) {
                    tableToUpdate.setReservedID(1); // Mark as reserved
                    updateSession.update(tableToUpdate);
                    System.out.println("✅ Updated table " + tableId + " reservedID to 1 for immediate visual feedback");
                }
                
                updateSession.getTransaction().commit();
            } catch (Exception e) {
                System.err.println("⚠️ Warning: Could not update table reservedID: " + e.getMessage());
                // Don't fail the reservation if this update fails
            }
            
            System.out.println("Reservation created successfully - table marked as reserved for immediate feedback");
            
            // Send navigation command to return to main page
            client.sendToClient("NAVIGATE_TO_MAIN");
            
            SimpleServer.sendSuccessResponse(client, "RESERVATION_SUCCESS", "Table reserved successfully. Redirecting to main page.");


        } catch (Exception e) {
            System.err.println("Error processing reservation: " + e.getMessage());
            e.printStackTrace();
            SimpleServer.sendFailureResponse(client, "RESERVATION_FAILURE", "Server error: " + e.getMessage());
        }
    }
    
    /**
     * Checks if a table is already reserved during the specified time window.
     */
    private static boolean checkIfTableIsReserved(int branchId, int tableId, LocalDateTime startTime, LocalDateTime endTime) {
        try (Session session = Database.getSessionFactoryInstance().openSession()) {
            System.out.println("🔍🔍🔍 DEEP DEBUG: Starting table availability check 🔍🔍🔍");
            System.out.println("🔍 Requested: Table " + tableId + " at branch " + branchId);
            System.out.println("🔍 Time window: " + startTime + " to " + endTime);
            System.out.println("🔍 Date: " + startTime.toLocalDate());
            
            // Use a simpler approach - get all reservations for this branch and filter by table manually
            String hql = "FROM Reservation r WHERE r.branch.id = :branchId";
            System.out.println("🔍 Using HQL: " + hql);
            
            List<Reservation> allReservations = session.createQuery(hql, Reservation.class)
                    .setParameter("branchId", branchId)
                    .list();
            
            // Now filter by table manually to avoid JOIN issues
            List<Reservation> tableReservations = new ArrayList<>();
            for (Reservation r : allReservations) {
                if (r.getTables() != null) {
                    for (RestaurantTable table : r.getTables()) {
                        if (table.getid() == tableId) {
                            tableReservations.add(r);
                            break; // Found this table in this reservation, move to next reservation
                        }
                    }
                }
            }
            
            System.out.println("🔍 Found " + allReservations.size() + " TOTAL reservations for branch " + branchId);
            System.out.println("🔍 Found " + tableReservations.size() + " reservations that include table " + tableId);
            
            // Now filter by date manually to see what we're dealing with
            List<Reservation> sameDateReservations = new ArrayList<>();
            for (Reservation r : tableReservations) {
                LocalDateTime reservationDate = r.getReservationTime();
                System.out.println("🔍 Reservation " + r.getId() + ": " + reservationDate + " (date: " + reservationDate.toLocalDate() + ")");
                
                if (reservationDate.toLocalDate().equals(startTime.toLocalDate())) {
                    sameDateReservations.add(r);
                    System.out.println("🔍 -> SAME DATE! Adding to check list");
                } else {
                    System.out.println("🔍 -> DIFFERENT DATE! Skipping");
                }
            }
            
            System.out.println("🔍 After date filtering: " + sameDateReservations.size() + " reservations for same date");
            
            // Check if any reservation overlaps with the requested time window
            for (Reservation reservation : sameDateReservations) {
                LocalDateTime reservationStart = reservation.getReservationTime();
                LocalDateTime reservationEnd = reservation.getEndTime();
                
                System.out.println("🔍 Checking against reservation: " + reservation.getId() + 
                                 " from " + reservationStart + " to " + reservationEnd);
                
                // Debug the overlaps logic
                boolean startBeforeEnd = reservationStart.isBefore(endTime);
                boolean endAfterStart = reservationEnd.isAfter(startTime);
                boolean overlaps = startBeforeEnd && endAfterStart;
                
                System.out.println("🔍 Overlap check: " + reservationStart + ".isBefore(" + endTime + ") = " + startBeforeEnd);
                System.out.println("🔍 Overlap check: " + reservationEnd + ".isAfter(" + startTime + ") = " + endAfterStart);
                System.out.println("🔍 Final overlap result: " + overlaps);
                
                if (overlaps) {
                    System.out.println("❌❌❌ CONFLICT FOUND: Reservation " + reservation.getId() + " overlaps with requested time! ❌❌❌");
                    return true;
                }
            }
            
            System.out.println("✅✅✅ No conflicts found - table is available! ✅✅✅");
            return false;
        } catch (Exception e) {
            System.err.println("❌❌❌ Error checking if table is reserved: " + e.getMessage());
            e.printStackTrace();
            return true; // Assume reserved if there's an error
        }
    }

    public static void handleCancelReservation(String msgString, ConnectionToClient client) {
        String[] parts = msgString.split(";");
        //the message has to be {command;reservation id}
        if (parts.length < 2) {
            SimpleServer.sendFailureResponse(client, "CANCELLING_RESERVATION_FAILURE", "Invalid format");
            return;
        }

        try {
            int reservationId = Integer.parseInt(parts[1]);

            List<Reservation> reservations = DataManager.fetchByField(Reservation.class, "id", reservationId);
            if (reservations.isEmpty()) {
                SimpleServer.sendFailureResponse(client, "CANCELLING_RESERVATION_FAILURE", "Reservation not found");
                return;
            }

            Reservation reservation = reservations.get(0);
            LocalDateTime startTime = reservation.getReservationTime();
            LocalDateTime now = LocalDateTime.now();

            Duration diff = Duration.between(now, startTime);
            long minutesBefore = diff.toMinutes();

            double refundPercentage;
            if (minutesBefore >= 180) {
                refundPercentage = 1.0; // Full refund
            } else if (minutesBefore >= 60) {
                refundPercentage = 0.5; // Half refund
            } else {
                refundPercentage = 0.0; // No refund
            }
            
            // Remove the reservation from the client's list
            Client customer = reservation.getClient();
            if (customer != null && customer.getReservations() != null) {
                customer.getReservations().remove(reservation);
                // Update the customer in the database using a session
                Session updateSession = Database.getSessionFactoryInstance().openSession();
                try {
                    updateSession.beginTransaction();
                    updateSession.update(customer);
                    updateSession.getTransaction().commit();
                } catch (Exception e) {
                    if (updateSession.getTransaction() != null) {
                        updateSession.getTransaction().rollback();
                    }
                    System.err.println("Error updating client: " + e.getMessage());
                } finally {
                    updateSession.close();
                }
            }
            
            // Reset table reservedID to make it available again
            try (Session tableUpdateSession = Database.getSessionFactoryInstance().openSession()) {
                tableUpdateSession.beginTransaction();
                
                // Get the table from the reservation and reset its reservedID
                if (reservation.getTables() != null && !reservation.getTables().isEmpty()) {
                    for (RestaurantTable table : reservation.getTables()) {
                        RestaurantTable tableToUpdate = tableUpdateSession.get(RestaurantTable.class, table.getid());
                        if (tableToUpdate != null) {
                            tableToUpdate.setReservedID(0); // Mark as available
                            tableUpdateSession.update(tableToUpdate);
                            System.out.println("✅ Reset table " + table.getid() + " reservedID to 0 (available)");
                        }
                    }
                }
                
                tableUpdateSession.getTransaction().commit();
            } catch (Exception e) {
                System.err.println("⚠️ Warning: Could not reset table reservedID: " + e.getMessage());
                // Don't fail the cancellation if this update fails
            }
            
            // Delete the reservation
            DataManager.delete(reservation);

            // TODO: Process refund (optional, maybe just a print for now)
            System.out.println("Refund for client: " + (refundPercentage * 100) + "%");

            SimpleServer.sendSuccessResponse(client, "CANCELLING_RESERVATION_SUCCESS", 
                    "Reservation cancelled. Refund: " + (int)(refundPercentage * 100) + "%");
            System.out.println("Reservation cancelled successfully: " + reservationId);

        } catch (Exception e) {
            e.printStackTrace();
            SimpleServer.sendFailureResponse(client, "CANCELLING_RESERVATION_FAILURE", "Server error: " + e.getMessage());
        }
    }

    public static void handleGetAvailableTables(String msgString,ConnectionToClient client) {
        // TODO: Return list of available tables to client
        String[] parts = msgString.split(";");
        //the message seems to be {command;branch;required arrival time,required location}
        if (parts.length < 4) {
            SimpleServer.sendFailureResponse(client, "GET_AVAILABLE_TABLES_FAILURE", "Invalid format");
        }
        try{
            Branch branch =DataManager.fetchByField(Branch.class, "id", parts[1]).get(0);
            List<RestaurantTable> tables = branch.getTables();
            client.sendToClient(tables);
        } catch (Exception e) {
            e.printStackTrace();
            SimpleServer.sendFailureResponse(client, "CANCELLING_RESERVATION_FAILURE", "Server error: " + e.getMessage());
        }
    }
    
    public static void handleGetBranchTables(String msgString, ConnectionToClient client) {
        System.out.println("Received get branch tables request: " + msgString);
        String[] parts = msgString.split(";");
        
        if (parts.length < 2) {
            SimpleServer.sendFailureResponse(client, "GET_BRANCH_TABLES_FAILURE", "Invalid format");
            return;
        }
        
        try {
            int branchId = Integer.parseInt(parts[1]);
            System.out.println("Fetching tables for branch ID: " + branchId);
            
            // Use a proper Hibernate session to avoid lazy loading issues
            Session session = Database.getSessionFactoryInstance().openSession();
            try {
                session.beginTransaction();
                
                // Get branch by ID and initialize its tables collection
                Branch branch = session.get(Branch.class, branchId);
                if (branch == null) {
                    SimpleServer.sendFailureResponse(client, "GET_BRANCH_TABLES_FAILURE", "Branch not found");
                    return;
                }
                
                // Initialize the lazy-loaded tables collection
                Hibernate.initialize(branch.getTables());
                List<RestaurantTable> tables = branch.getTables();
                
                if (tables == null || tables.isEmpty()) {
                    System.out.println("No tables found for branch " + branchId);
                    SimpleServer.sendFailureResponse(client, "GET_BRANCH_TABLES_FAILURE", "No tables found for this branch");
                    return;
                }
                
                System.out.println("Found " + tables.size() + " tables for branch " + branchId);
                
                // Send tables to client
                System.out.println("Sending " + tables.size() + " tables to client");
                client.sendToClient(tables);
                
                session.getTransaction().commit();
                
            } catch (Exception e) {
                if (session.getTransaction() != null) {
                    session.getTransaction().rollback();
                }
                throw e;
            } finally {
                session.close();
            }
            
        } catch (Exception e) {
            System.err.println("Error fetching branch tables: " + e.getMessage());
            e.printStackTrace();
            SimpleServer.sendFailureResponse(client, "GET_BRANCH_TABLES_FAILURE", "Server error: " + e.getMessage());
        }
    }
    
    /**
     * Handle GET_BRANCH_TABLES_WITH_AVAILABILITY request - return tables with real-time availability
     * Format: GET_BRANCH_TABLES_WITH_AVAILABILITY;branchId;date;time
     */
    public static void handleGetBranchTablesWithAvailability(String msgString, ConnectionToClient client) {
        System.out.println("Received get branch tables with availability request: " + msgString);
        String[] parts = msgString.split(";");
        
        if (parts.length < 4) {
            SimpleServer.sendFailureResponse(client, "GET_BRANCH_TABLES_AVAILABILITY_FAILURE", "Invalid format. Expected: GET_BRANCH_TABLES_WITH_AVAILABILITY;branchId;date;time");
            return;
        }
        
        try {
            int branchId = Integer.parseInt(parts[1]);
            String dateStr = parts[2];
            String timeStr = parts[3];
            
            // Parse date and time
            LocalDateTime requestedDateTime = LocalDateTime.parse(dateStr + "T" + timeStr);
            LocalDateTime endDateTime = requestedDateTime.plusMinutes(90); // 1.5 hour reservation
            
            System.out.println("Checking availability for branch " + branchId + " at " + requestedDateTime + " to " + endDateTime);
            
            // Use a proper Hibernate session
            Session session = Database.getSessionFactoryInstance().openSession();
            try {
                session.beginTransaction();
                
                // Get branch and tables
                Branch branch = session.get(Branch.class, branchId);
                if (branch == null) {
                    SimpleServer.sendFailureResponse(client, "GET_BRANCH_TABLES_AVAILABILITY_FAILURE", "Branch not found");
                    return;
                }
                
                Hibernate.initialize(branch.getTables());
                List<RestaurantTable> tables = branch.getTables();
                
                if (tables == null || tables.isEmpty()) {
                    SimpleServer.sendFailureResponse(client, "GET_BRANCH_TABLES_AVAILABILITY_FAILURE", "No tables found for this branch");
                    return;
                }
                
                // Check availability for each table at the requested time
                List<TableAvailabilityInfo> tableAvailabilityList = new ArrayList<>();
                
                for (RestaurantTable table : tables) {
                    boolean isAvailable = checkTableAvailabilityAtTime(branchId, table.getid(), requestedDateTime, endDateTime);
                    
                    TableAvailabilityInfo tableInfo = new TableAvailabilityInfo();
                    tableInfo.setTableId(table.getid());
                    tableInfo.setSeatingCapacity(table.getSeatingCapacity());
                    tableInfo.setLocation(table.getLocation());
                    tableInfo.setAvailable(isAvailable);
                    
                    tableAvailabilityList.add(tableInfo);
                }
                
                System.out.println("Sending " + tableAvailabilityList.size() + " tables with availability info");
                client.sendToClient(tableAvailabilityList);
                
                session.getTransaction().commit();
                
            } catch (Exception e) {
                if (session.getTransaction() != null) {
                    session.getTransaction().rollback();
                }
                throw e;
            } finally {
                session.close();
            }
            
        } catch (Exception e) {
            System.err.println("Error fetching branch tables with availability: " + e.getMessage());
            e.printStackTrace();
            SimpleServer.sendFailureResponse(client, "GET_BRANCH_TABLES_AVAILABILITY_FAILURE", "Server error: " + e.getMessage());
        }
    }
    
    /**
     * Handle GET_ALL_BRANCHES request - return all branches with opening hours
     */
    public static void handleGetAllBranches(ConnectionToClient client) {
        System.out.println("Received get all branches request");
        
        try {
            // Use a proper Hibernate session
            Session session = Database.getSessionFactoryInstance().openSession();
            try {
                session.beginTransaction();
                
                // Get all branches
                String hql = "FROM Branch";
                List<Branch> branches = session.createQuery(hql, Branch.class).list();
                
                if (branches == null || branches.isEmpty()) {
                    SimpleServer.sendFailureResponse(client, "GET_ALL_BRANCHES_FAILURE", "No branches found");
                    return;
                }
                
                System.out.println("Found " + branches.size() + " branches");
                
                // Send branches to client
                client.sendToClient(branches);
                
                session.getTransaction().commit();
                
            } catch (Exception e) {
                if (session.getTransaction() != null) {
                    session.getTransaction().rollback();
                }
                throw e;
            } finally {
                session.close();
            }
            
        } catch (Exception e) {
            System.err.println("Error fetching all branches: " + e.getMessage());
            e.printStackTrace();
            SimpleServer.sendFailureResponse(client, "GET_ALL_BRANCHES_FAILURE", "Server error: " + e.getMessage());
        }
    }
    
    /**
     * Check if a specific table is available at a specific time
     */
    private static boolean checkTableAvailabilityAtTime(int branchId, int tableId, LocalDateTime startTime, LocalDateTime endTime) {
        try (Session session = Database.getSessionFactoryInstance().openSession()) {
            // Get all reservations for this branch and table
            String hql = "FROM Reservation r WHERE r.branch.id = :branchId";
            List<Reservation> allReservations = session.createQuery(hql, Reservation.class)
                    .setParameter("branchId", branchId)
                    .list();
            
            // Filter reservations that include this table
            List<Reservation> tableReservations = new ArrayList<>();
            for (Reservation r : allReservations) {
                if (r.getTables() != null) {
                    for (RestaurantTable table : r.getTables()) {
                        if (table.getid() == tableId) {
                            tableReservations.add(r);
                            break;
                        }
                    }
                }
            }
            
            // Check if any reservation overlaps with the requested time
            for (Reservation reservation : tableReservations) {
                LocalDateTime reservationStart = reservation.getReservationTime();
                LocalDateTime reservationEnd = reservation.getEndTime();
                
                // Check for overlap
                boolean overlaps = reservationStart.isBefore(endTime) && reservationEnd.isAfter(startTime);
                
                if (overlaps) {
                    System.out.println("Table " + tableId + " is NOT available - overlaps with reservation " + reservation.getId());
                    return false; // Table is not available
                }
            }
            
            System.out.println("Table " + tableId + " is available at requested time");
            return true; // Table is available
        } catch (Exception e) {
            System.err.println("Error checking table availability: " + e.getMessage());
            return false; // Assume not available if there's an error
        }
    }

    public static void handleGetUserReservations(String msgString, ConnectionToClient client) {
        // Format: GET_USER_RESERVATIONS;phoneNumber
        String[] parts = msgString.split(";");
        if (parts.length < 2) {
            System.out.println("Invalid format for getting user reservations");
            SimpleServer.sendFailureResponse(client, "GET_RESERVATIONS_FAILURE", "Invalid format");
            return;
        }
        
        String phoneNumber = parts[1];
        System.out.println("Fetching reservations for phone number: " + phoneNumber);
        
        try {
            // First attempt to find reservations directly by phone number
            List<Reservation> reservationsByPhone = fetchReservationsByPhoneNumber(phoneNumber);
            
            // If no reservations found directly, attempt the traditional client lookup
            if (reservationsByPhone.isEmpty()) {
                System.out.println("No reservations found directly by phone number, trying client lookup...");
                
                // Get user account by phone number
                List<UserAccount> userAccounts = DataManager.fetchUserAccountsByPhoneNumber(phoneNumber);
                
                if (userAccounts.isEmpty()) {
                    System.out.println("No user account found with phone: " + phoneNumber + ". Creating new account.");
                    // Create a new user account - for Guest client with null branch fields
                    UserAccount userAccount = new UserAccount("Guest", phoneNumber, "client", "password");
                    // Branch fields remain null by default for client accounts
                    DataManager.add(userAccount);
                    System.out.println("Created new user account with ID: " + userAccount.getId());
                    
                    // Send empty reservations list for new account
                    client.sendToClient(new ArrayList<Reservation>()); 
                    return;
                }
                
                // Find client for this user account
                List<Client> clients = DataManager.fetchClientsByPhoneNumber(phoneNumber);
                if (clients.isEmpty()) {
                    System.out.println("No client record found for phone: " + phoneNumber);
                    client.sendToClient(new ArrayList<Reservation>()); // Send empty list
                    return;
                }
                
                Client customer = clients.get(0);
                List<Reservation> reservations = customer.getReservations();
                
                if (reservations == null || reservations.isEmpty()) {
                    System.out.println("No reservations found for client: " + customer.getName());
                    client.sendToClient(new ArrayList<Reservation>()); // Send empty list
                    return;
                }
                
                System.out.println("Found " + reservations.size() + " reservations for client: " + customer.getName());
                client.sendToClient(reservations);
            } else {
                System.out.println("Found " + reservationsByPhone.size() + " reservations directly by phone number");
                client.sendToClient(reservationsByPhone);
            }
        } catch (Exception e) {
            System.err.println("Error fetching reservations: " + e.getMessage());
            e.printStackTrace();
            try {
                SimpleServer.sendFailureResponse(client, "GET_RESERVATIONS_FAILURE", "Error fetching reservations");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    
    // Helper method to fetch reservations directly by phone number
    private static List<Reservation> fetchReservationsByPhoneNumber(String phoneNumber) {
        Session session = Database.getSessionFactoryInstance().openSession();
        List<Reservation> reservations = new ArrayList<>();
        
        try {
            session.beginTransaction();
            
            String hql = "FROM Reservation r WHERE r.phoneNumber = :phoneNumber";
            reservations = session.createQuery(hql, Reservation.class)
                    .setParameter("phoneNumber", phoneNumber)
                    .getResultList();
            
            session.getTransaction().commit();
        } catch (Exception e) {
            System.err.println("Error fetching reservations by phone: " + e.getMessage());
            if (session.getTransaction() != null) {
                session.getTransaction().rollback();
            }
        } finally {
            session.close();
        }
        
        return reservations;
    }
}

