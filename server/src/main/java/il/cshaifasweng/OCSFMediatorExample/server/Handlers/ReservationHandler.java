package il.cshaifasweng.OCSFMediatorExample.server.Handlers;

import il.cshaifasweng.OCSFMediatorExample.entities.Branch;
import il.cshaifasweng.OCSFMediatorExample.entities.Client;
import il.cshaifasweng.OCSFMediatorExample.entities.Reservation;
import il.cshaifasweng.OCSFMediatorExample.entities.RestaurantTable;
import il.cshaifasweng.OCSFMediatorExample.server.SimpleServer;
import il.cshaifasweng.OCSFMediatorExample.server.dataManagers.DataManager;
import il.cshaifasweng.OCSFMediatorExample.server.ocsf.ConnectionToClient;
import org.hibernate.annotations.Table;

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
        String[] parts = msgString.split(";");

        if (parts.length < 7) {
            SimpleServer.sendFailureResponse(client, "RESERVATION_FAILURE", "Invalid format");
            return;
        }

        try {
            int branchId = Integer.parseInt(parts[1]);
            int guestCount = Integer.parseInt(parts[2]);
            String seatingPref = parts[3];
            LocalDateTime arrival = LocalDateTime.parse(parts[4], DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            String phoneNumber = parts[5];
            String location=parts[6];

            List<Branch> branches = DataManager.fetchByField(Branch.class, "id", branchId);
            List<Client> clients = DataManager.fetchByField(Client.class, "account.phoneNumber", phoneNumber);

            if (branches.isEmpty() || clients.isEmpty()) {
                SimpleServer.sendFailureResponse(client, "RESERVATION_FAILURE", "Branch or Client not found");
                return;
            }

            Branch branch = branches.get(0);
            Client customer = clients.get(0);
            LocalDateTime departure = arrival.plusMinutes(Reservation.DEFAULT_DURATION_MINUTES);

            // Now search for a matching table in the branch
            List<RestaurantTable> tables = branch.getTables(); // Assuming proper JPA mapping exists

            //if the client added a location where they wanna sit then we will change the 'tables' list
            if(!location.isEmpty())
            {
                List<RestaurantTable> availableTables = returnRequiredTables(tables,location);
                tables=availableTables;
            }

            //here we have enough info to proceed, next step is to see if we can make it work
            //meaning we need enough tables, we need to pass the number of visitors and check the space
            List<RestaurantTable> tablesToAdd = new ArrayList<>();
            boolean reservation=CheckReservation(tables,guestCount,arrival,tablesToAdd);
            if (reservation) {
                reserveTables(tablesToAdd,toIndex(arrival));
                Reservation newreservation = new Reservation(arrival, guestCount, customer, branch,tablesToAdd);
                DataManager.add(newreservation);
                customer.getReservations().add(newreservation);
                SimpleServer.sendSuccessResponse(client, "RESERVATION_SUCCESS", "Table reserved successfully");
            }
            else {
                SimpleServer.sendFailureResponse(client, "RESERVATION_FAILURE", "No suitable table(s) available");
            }

        } catch (Exception e) {
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
}