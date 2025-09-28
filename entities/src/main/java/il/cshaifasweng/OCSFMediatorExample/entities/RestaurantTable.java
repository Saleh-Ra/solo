package il.cshaifasweng.OCSFMediatorExample.entities;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
@javax.persistence.Table(name = "Restaurant_table")
public class RestaurantTable implements Serializable {

    public static final int MINUTES = 720;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private int seatingCapacity;
    private int reservedID;
    private String location;
    private String label; //table 1, table 6...
    private String reservedBy; // phone number of person who reserved
    
    // Store availability as a string of 0s and 1s in database
    @Column(length = 1000)
    private String minutesData;
    
    @Transient
    private boolean[] minutes;

    // Default constructor (required by Hibernate)
    public RestaurantTable() {
        this.minutes = new boolean[MINUTES]; // 12 hours * 60 mins
        updateMinutesData(); // Initialize the string representation
    }

    public RestaurantTable(int seatingCapacity, int reservedID, String location, String label, boolean[] time_array) {
        this.seatingCapacity = seatingCapacity;
        this.reservedID = reservedID;
        this.location = location;
        this.label = label;
        this.minutes = time_array != null ? time_array : new boolean[MINUTES];
        updateMinutesData(); // Initialize the string representation
    }

    // Convert the boolean array to a string for database storage
    private void updateMinutesData() {
        if (minutes == null) {
            minutes = new boolean[MINUTES];
        }
        StringBuilder sb = new StringBuilder(MINUTES);
        for (boolean b : minutes) {
            sb.append(b ? '1' : '0');
        }
        this.minutesData = sb.toString();
    }

    // Convert the string back to boolean array when loading from database
    private void updateMinutesArray() {
        if (minutesData == null || minutesData.isEmpty()) {
            minutes = new boolean[MINUTES];
            return;
        }
        
        minutes = new boolean[MINUTES];
        for (int i = 0; i < Math.min(minutesData.length(), MINUTES); i++) {
            minutes[i] = minutesData.charAt(i) == '1';
        }
    }

    public int getid() {
        return id;
    }

    public void setid(int id) {
        this.id = id;
    }

    public int getSeatingCapacity() {
        return seatingCapacity;
    }

    public void setSeatingCapacity(int seatingCapacity) {
        this.seatingCapacity = seatingCapacity;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) { this.location = location; }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) { this.label = label; }

    public int getReservedID() {
        return reservedID;
    }

    public void setReservedID(int reservedID) {
        this.reservedID = reservedID;
    }

    public boolean[] getMinutes() {
        if (minutes == null) {
            updateMinutesArray();
        }
        return minutes;
    }
    
    public void setMinutes(boolean[] minutes) {
        this.minutes = minutes;
        updateMinutesData();
    }
    
    // For debugging purposes
    public String getMinutesData() {
        return minutesData;
    }
    
    public void setMinutesData(String minutesData) {
        this.minutesData = minutesData;
        updateMinutesArray();
    }
}
