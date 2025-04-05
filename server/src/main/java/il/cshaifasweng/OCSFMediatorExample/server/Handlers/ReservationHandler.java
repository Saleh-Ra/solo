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

        if (parts.length < 7) {
            System.out.println("Invalid format: Expected at least 7 parts, got " + parts.length);
            SimpleServer.sendFailureResponse(client, "RESERVATION_FAILURE", "Invalid format");
            return;
        }

        try {
            int branchId = Integer.parseInt(parts[1]);
            int guestCount = Integer.parseInt(parts[2]);
            String seatingPref = parts[3];
            LocalDateTime arrival = LocalDateTime.parse(parts[4], DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            String phoneNumber = parts[5];
            String location = parts[6];

            System.out.println("Processing reservation - Branch: " + branchId + 
                    ", Guests: " + guestCount + 
                    ", Arrival: " + arrival + 
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
            
            List<UserAccount> userAccounts = DataManager.fetchUserAccountsByPhoneNumber(phoneNumber);
            
            UserAccount userAccount;
            if (userAccounts.isEmpty()) {
                System.out.println("User not found with phone: " + phoneNumber + ". Creating new user account.");
                // Create a new user account
                userAccount = new UserAccount("Guest", phoneNumber, false, "password");
                DataManager.add(userAccount);
                System.out.println("Created new user account with ID: " + userAccount.getId());
            } else {
                userAccount = userAccounts.get(0);
                System.out.println("Found user account: " + userAccount.getName() + " with ID: " + userAccount.getId());
            }

            LocalDateTime departure = arrival.plusMinutes(Reservation.DEFAULT_DURATION_MINUTES);

            System.out.println("Found branch: " + branch.getLocation());

            // Use the tables list loaded earlier
            if (tables.isEmpty()) {
                System.out.println("No tables found in branch: " + branch.getLocation() + " - will create reservation anyway");
                // Continuing without tables - will create empty reservation
            } else {
                System.out.println("Found " + tables.size() + " tables in branch");
            }

            //if the client added a location where they wanna sit then we will change the 'tables' list
            if(!location.isEmpty()) {
                List<RestaurantTable> availableTables = returnRequiredTables(tables, location);
                tables = availableTables;
                System.out.println("Filtered to " + tables.size() + " tables in location: " + location);
            }

            //here we have enough info to proceed, next step is to see if we can make it work
            //meaning we need enough tables, we need to pass the number of visitors and check the space
            List<RestaurantTable> tablesToAdd = new ArrayList<>();
            
            // REMOVING TABLE AVAILABILITY CHECK - always proceed with reservation
            boolean reservation = true;
            
            // Add at least one table to the reservation (if any exists)
            if (!tables.isEmpty()) {
                tablesToAdd.add(tables.get(0));
            }
            
            System.out.println("Bypassing reservation check - proceeding with reservation");
            
            if (reservation) {
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
                
                // Create and save reservation in a single transaction
                Reservation newReservation = new Reservation(arrival, guestCount, phoneNumber, branch, tablesToAdd);
                newReservation.setClient(customer);
                
                // If tables exist, mark them as reserved
                if (!tablesToAdd.isEmpty()) {
                    reserveTables(tablesToAdd, toIndex(arrival));
                }
                
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
            }
            else {
                System.out.println("No suitable tables available for reservation");
                SimpleServer.sendFailureResponse(client, "RESERVATION_FAILURE", "No suitable table(s) available");
            }

        } catch (Exception e) {
            System.err.println("Error processing reservation: " + e.getMessage());
            e.printStackTrace();
            SimpleServer.sendFailureResponse(client, "RESERVATION_FAILURE", "Server error: " + e.getMessage());
        }
    }
    private static List<RestaurantTable> returnRequiredTables(List<RestaurantTable> tables,String location)
    {
        List<RestaurantTable> newlist=new ArrayList<>();
        for(RestaurantTable table:tables)
        {
            if(table.getLocation().equals(location)){newlist.add(table);}
        }

        return newlist;
    }

    private static boolean CheckReservation(List<RestaurantTable> tablesList,int guestCount,LocalDateTime arrival, List<RestaurantTable> tablesToAdd)
    {
        int guestsWeCanAbsorb = 0;
        int start=toIndex(arrival);
        for(RestaurantTable t:tablesList){
            if(isAvailable(t,start,tablesToAdd)){
                guestsWeCanAbsorb+=t.getSeatingCapacity();
            }
            if(guestsWeCanAbsorb>=guestCount){return true;}
        }
        return false;
    }

    private static int toIndex(LocalDateTime arrival) {
        return arrival.getHour()*60+arrival.getMinute();
    }

    private static boolean isAvailable(RestaurantTable table,int start,List<RestaurantTable> tablesToAdd) {
        boolean[] timeSlots=table.getMinutes();
        for (int i = start; i < start + Reservation.DEFAULT_DURATION_MINUTES; i++) {
            if (timeSlots[i]) return false;
        }
        tablesToAdd.add(table);
        return true;
    }

    private static void reserveTables(List<RestaurantTable> tables, int arrival) {
        for (RestaurantTable table : tables) {
            boolean[] time=table.getMinutes();
            for(int i=arrival;i<arrival+ Reservation.DEFAULT_DURATION_MINUTES;i++) {
                time[i]=true;
            }
        }
    }



    public static void handleCancelReservation(String msgString, ConnectionToClient client) {
        // TODO: Cancel reservation by ID or table/client
        //this method can't be activated unless a reservation was made before
        //the client press on the "cancel" button, then we can activate the method on a reservation that for sure in the database
        String[] parts = msgString.split(";");
        //the message has to be {command;reservation id}
        if (parts.length < 2) {
            SimpleServer.sendFailureResponse(client, "CANCELLING_RESERVATION_FAILURE", "Invalid format");
        }

        try {
            int reservationId = Integer.parseInt(parts[1]);

            List<Reservation> reservations = DataManager.fetchByField(Reservation.class, "id", reservationId);
            if (reservations.isEmpty()) {
                SimpleServer.sendFailureResponse(client, "CANCELLING_RESERVATION_FAILURE", "Reservation not found");
                return;
            }

            Reservation reservation = reservations.get(0);
            LocalDateTime arrival = reservation.getReservationTime();
            LocalDateTime now = LocalDateTime.now();

            Duration diff = Duration.between(now, arrival);
            long minutesBefore = diff.toMinutes();

            double refundPercentage;
            if (minutesBefore >= 60) {
                refundPercentage = 1.0; // Full refund
            } else if (minutesBefore >= 15) {
                refundPercentage = 0.5; // Half refund
            } else {
                refundPercentage = 0.0; // No refund
            }
            Client customer=reservation.getClient();
            customer.getReservations().remove(reservation);
            int start = arrival.getHour() * 60 + arrival.getMinute();

            // Free the occupied minutes
            for (RestaurantTable table : reservation.getTables()) {
                boolean[] minutes = table.getMinutes();
                for (int i = start; i < start + Reservation.DEFAULT_DURATION_MINUTES; i++) {
                    minutes[i] = false;
                }
            }

            DataManager.delete(reservation);

            // TODO: Process refund (optional, maybe just a print for now)
            System.out.println("Refund for client: " + (refundPercentage * 100) + "%");

            SimpleServer.sendSuccessResponse(client, "CANCELLING_RESERVATION_SUCCESS", "Reservation cancelled");
            System.out.println("Received cancel reservation request: " + msgString);

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
                    // Create a new user account
                    UserAccount userAccount = new UserAccount("Guest", phoneNumber, false, "password");
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