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


    public static void handleReserveRequest(String msgString, ConnectionToClient client) {
        System.out.println("Received reservation request: " + msgString);
        String[] parts = msgString.split(";");

        // Format: RESERVE_TABLE;branchId;guestCount;tableId;location;startDateTime;endDateTime;phoneNumber
        if (parts.length < 7) {
            System.out.println("Invalid format: Expected at least 8 parts, got " + parts.length);
            SimpleServer.sendFailureResponse(client, "RESERVATION_FAILURE", "Invalid format");
            return;
        }

        try {
            int branchId = Integer.parseInt(parts[1]);
            int guestCount = Integer.parseInt(parts[2]);
            String location = parts[3];
            LocalDateTime startTime = LocalDateTime.parse(parts[4], DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            LocalDateTime endTime = LocalDateTime.parse(parts[5], DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            String phoneNumber = parts[6];

            System.out.println("Processing reservation - Branch: " + branchId + 
                    ", Guests: " + guestCount +
                    ", Location: " + location +
                    ", Start: " + startTime + 
                    ", End: " + endTime +
                    ", Phone: " + phoneNumber);

            // Fetch branch with tables in one session to avoid lazy loading issues
            Branch branch;
            List<RestaurantTable> tables;
            Session branchSession = Database.getSessionFactoryInstance().openSession();
            try {
                branchSession.beginTransaction();
                branch = branchSession.get(Branch.class, branchId);
                if (branch == null) {
                    System.out.println("Branch not found with ID: " + branchId);
                    SimpleServer.sendFailureResponse(client, "RESERVATION_FAILURE", "Branch not found");
                    return;
                }
                // Initialize the lazy-loaded tables collection
                Hibernate.initialize(branch.getTables());
                tables = new ArrayList<>(branch.getTables());
                branchSession.getTransaction().commit();
            } finally {
                branchSession.close();
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
            boolean available=checkifpossible(branchId,guestCount,location,startTime,endTime);
            // Create tables list with just this table
            List<RestaurantTable> tablesToAdd = new ArrayList<>();

            
            // Create and save reservation in a single transaction
            Reservation newReservation = new Reservation(startTime, endTime, guestCount, phoneNumber, branch);
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
            /*try (Session updateSession = Database.getSessionFactoryInstance().openSession()) {
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
            }*/
            
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

    //check if there is an option to make the reservation
    private static boolean checkifpossible(int branchId, int guestCount, String location, LocalDateTime startTime, LocalDateTime endTime)
    {
        /**
         * check if tables are available at start< time< end
         * once we find the occupied tables, we wanna see if available seats>= guests number (based on location)
         * only after that we can make the reservation
         */
        try (Session session = Database.getSessionFactoryInstance().openSession()) {
            
            System.out.println("🔍 Checking reservation possibility for "
             + guestCount + " guests at " + location + " from " + startTime + " to " + endTime);
            
            // Get all tables for this branch
            Branch branch = session.get(Branch.class, branchId);
            if (branch == null) {
                System.out.println("❌ Branch not found with ID: " + branchId);
                return false;
            }
            
            List<RestaurantTable> allTables = branch.getTables();
            if (allTables == null || allTables.isEmpty()) {
                System.out.println("❌ No tables found for branch " + branchId);
                return false;
            }
            
            // Filter tables by location
            List<RestaurantTable> locationTables = new ArrayList<>();
            for (RestaurantTable table : allTables) {
                if (table.getLocation() != null && table.getLocation().equalsIgnoreCase(location)) {
                    locationTables.add(table);
                }
            }
            
            System.out.println("🔍 Found " + locationTables.size() + " tables in " + location + " area");
            
            if (locationTables.isEmpty()) {
                System.out.println("❌ No tables found in " + location + " area");
                return false;
            }
            
            // Get all reservations for this branch in the time window
            String hql = "FROM Reservation r WHERE r.branch.id = :branchId";
            List<Reservation> allReservations = session.createQuery(hql, Reservation.class)
                    .setParameter("branchId", branchId)
                    .list();
            
            // Find tables that are occupied during the requested time window
            List<Integer> occupiedTableIds = new ArrayList<>();
            for (Reservation reservation : allReservations) {
                if (reservation.getReservationTime().toLocalDate().equals(startTime.toLocalDate())) {
                    // Check if reservation overlaps with requested time
                    if (reservation.getReservationTime().isBefore(endTime) && 
                        reservation.getEndTime().isAfter(startTime)) {
                        
                        // Add all tables from this reservation to occupied list
                        if (reservation.getTables() != null) {
                            for (RestaurantTable table : reservation.getTables()) {
                                occupiedTableIds.add(table.getid());
                            }
                        }
                    }
                }
            }
            
            System.out.println("🔍 Found " + occupiedTableIds.size() + " occupied tables during requested time");
            
            // Calculate available seats in the requested location
            int availableSeats = 0;
            for (RestaurantTable table : locationTables) {
                if (!occupiedTableIds.contains(table.getid())) {
                    availableSeats += table.getSeatingCapacity();
                    System.out.println("✅ Table " + table.getid() + " available with " + table.getSeatingCapacity() + " seats");
                } else {
                    System.out.println("❌ Table " + table.getid() + " occupied during requested time");
                }
            }
            
            System.out.println("🔍 Total available seats in " + location + ": " + availableSeats + " (needed: " + guestCount + ")");
            
            boolean canAccommodate = availableSeats >= guestCount;
            if (canAccommodate) {
                System.out.println("✅ Reservation is possible - sufficient seats available");
            } else {
                System.out.println("❌ Reservation not possible - insufficient seats (need " + guestCount + ", have " + availableSeats + ")");
            }
            
            return canAccommodate;
            
        } catch (Exception e) {
            System.err.println("❌ Error checking reservation possibility: " + e.getMessage());
            e.printStackTrace();
            return false; // Assume not possible if there's an error
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
}    
