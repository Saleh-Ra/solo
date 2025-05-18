package il.cshaifasweng.OCSFMediatorExample.server.Handlers;

import il.cshaifasweng.OCSFMediatorExample.entities.Branch;
import il.cshaifasweng.OCSFMediatorExample.entities.Client;
import il.cshaifasweng.OCSFMediatorExample.entities.Reservation;
import il.cshaifasweng.OCSFMediatorExample.entities.RestaurantTable;
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
            SimpleServer.sendSuccessResponse(client, "RESERVATION_SUCCESS", "Table reserved successfully");

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
            // Find all reservations for this table
            String hql = "FROM Reservation r WHERE r.tableId = :tableId AND r.branch.id = :branchId";
            List<Reservation> reservations = session.createQuery(hql, Reservation.class)
                    .setParameter("tableId", tableId)
                    .setParameter("branchId", branchId)
                    .list();
            
            // Check if any reservation overlaps with the requested time window
            for (Reservation reservation : reservations) {
                if (reservation.overlaps(startTime, endTime)) {
                    return true;
                }
            }
            
            return false;
        } catch (Exception e) {
            System.err.println("Error checking if table is reserved: " + e.getMessage());
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
            if (minutesBefore >= 60) {
                refundPercentage = 1.0; // Full refund
            } else if (minutesBefore >= 15) {
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

