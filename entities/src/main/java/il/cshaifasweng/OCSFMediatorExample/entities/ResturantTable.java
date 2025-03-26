package il.cshaifasweng.OCSFMediatorExample.entities;

import javax.persistence.*;

@Entity
@Table(name = "resturant_table")
public class ResturantTable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int tableID;

    private int seatingCapacity;
    private boolean isOccupied;
    private int reservedID;

    // Default constructor (required by Hibernate)
    public ResturantTable() {}

    public ResturantTable(int seatingCapacity, boolean occupied, int reservedID) {
        this.seatingCapacity = seatingCapacity;
        this.isOccupied = occupied;
        this.reservedID = reservedID;
    }

    public int getTableID() {
        return tableID;
    }

    public void setTableID(int tableID) {
        this.tableID = tableID;
    }

    public int getSeatingCapacity() {
        return seatingCapacity;
    }

    public void setSeatingCapacity(int seatingCapacity) {
        this.seatingCapacity = seatingCapacity;
    }

    public boolean isOccupied() {
        return isOccupied;
    }

    public void setOccupied(boolean occupied) {
        isOccupied = occupied;
    }

    public int getReservedID() {
        return reservedID;
    }

    public void setReservedID(int reservedID) {
        this.reservedID = reservedID;
    }
}
