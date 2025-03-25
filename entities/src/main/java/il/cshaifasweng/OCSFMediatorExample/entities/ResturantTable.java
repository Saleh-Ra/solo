package il.cshaifasweng.OCSFMediatorExample.entities;

public class ResturantTable {
    int TableID;
    int SeatingCapacity;
    boolean IsOccupied;
    int ReservedID;

    public ResturantTable(int tableID, int seatingCapacity, boolean occupied, int reservedID) {
        TableID = tableID;
        SeatingCapacity = seatingCapacity;
        IsOccupied = occupied;
        ReservedID = reservedID;
    }

    public int getTableID() {
        return TableID;
    }

    public void setTableID(int tableID) {
        TableID = tableID;
    }

    public int getSeatingCapacity() {
        return SeatingCapacity;
    }

    public void setSeatingCapacity(int seatingCapacity) {
        SeatingCapacity = seatingCapacity;
    }

    public boolean isOccupied() {
        return IsOccupied;
    }

    public void setOccupied(boolean occupied) {
        IsOccupied = occupied;
    }

    public int getReservedID() {
        return ReservedID;
    }

    public void setReservedID(int reservedID) {
        ReservedID = reservedID;
    }
}