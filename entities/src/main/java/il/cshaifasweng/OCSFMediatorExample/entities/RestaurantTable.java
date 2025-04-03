package il.cshaifasweng.OCSFMediatorExample.entities;

import javax.persistence.*;

@Entity
@javax.persistence.Table(name = "Restaurant_table")
public class RestaurantTable {

    public static final int MINUTES = 720;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int tableID;

    private int seatingCapacity;
    //private boolean isOccupied;
    private int reservedID;
    private String location;
    private String preferences;
    private String label; //table 1, table 6...
    @Transient
    private boolean[] minutes ;

    // Default constructor (required by Hibernate)
    public RestaurantTable() {
        this.minutes = new boolean[MINUTES]; // 12 hours * 60 mins
    }

    public RestaurantTable(int seatingCapacity, int reservedID,String location, String preferences,String label,boolean[] time_array) {
        this.seatingCapacity = seatingCapacity;
        //this.isOccupied = occupied;
        this.reservedID = reservedID;
        this.location = location;
        this.preferences = preferences;
        this.label=label;
        this.minutes=time_array;
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

    public String getPreferences() {
        return preferences;
    }

    public void setPreferences(String preferences) { this.preferences = preferences; }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) { this.location = location; }

    public String getLabel() {
        return label;
    }
    public void setLabel(String label) { this.label = label; }

    /*public boolean isOccupied() {
        return isOccupied;
    }

    public void setOccupied(boolean occupied) {
        isOccupied = occupied;
    }*/

    public int getReservedID() {
        return reservedID;
    }

    public void setReservedID(int reservedID) {
        this.reservedID = reservedID;
    }

    public boolean[] getMinutes() {return minutes;}
    public void setMinutes(boolean[] minutes) {this.minutes = minutes;}
}
