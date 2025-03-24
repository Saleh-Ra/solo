package il.cshaifasweng.OCSFMediatorExample.entities;

import java.time.DateTimeException;

public class Reservation {
    int ReservationID;
    int ClientID;
    int BranchID;
    int tablesID;
    DateTimeException ReservationDate;
    int NumberOfGuests;

    public Reservation(int reservationID, int clientID, int branchID, int tablesID, int numberOfGuests) {
        ReservationID = reservationID;
        ClientID = clientID;
        BranchID = branchID;
        tablesID = tablesID;
        NumberOfGuests = numberOfGuests;
    }

    public int getReservationID() {
        return ReservationID;
    }

    public void setReservationID(int reservationID) {
        ReservationID = reservationID;
    }

    public int getClientID() {
        return ClientID;
    }

    public void setClientID(int clientID) {
        ClientID = clientID;
    }

    public int getBranchID() {
        return BranchID;
    }

    public void setBranchID(int branchID) {
        BranchID = branchID;
    }

    public int getTablesID() {
        return tablesID;
    }

    public void setTablesID(int tablesID) {
        this.tablesID = tablesID;
    }

    public DateTimeException getReservationDate() {
        return ReservationDate;
    }

    public void setReservationDate(DateTimeException reservationDate) {
        ReservationDate = reservationDate;
    }

    public int getNumberOfGuests() {
        return NumberOfGuests;
    }

    public void setNumberOfGuests(int numberOfGuests) {
        NumberOfGuests = numberOfGuests;
    }
}
