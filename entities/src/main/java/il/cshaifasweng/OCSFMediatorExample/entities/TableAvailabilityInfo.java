package il.cshaifasweng.OCSFMediatorExample.entities;

import java.io.Serializable;

/**
 * DTO class to represent table availability information
 * Used for sending table status to clients without exposing full entity data
 */
public class TableAvailabilityInfo implements Serializable {
    
    private int tableId;
    private int seatingCapacity;
    private String location;
    private boolean available;
    
    public TableAvailabilityInfo() {
        // Default constructor for serialization
    }
    
    public TableAvailabilityInfo(int tableId, int seatingCapacity, String location, boolean available) {
        this.tableId = tableId;
        this.seatingCapacity = seatingCapacity;
        this.location = location;
        this.available = available;
    }
    
    // Getters and Setters
    public int getTableId() {
        return tableId;
    }
    
    public void setTableId(int tableId) {
        this.tableId = tableId;
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
    
    public void setLocation(String location) {
        this.location = location;
    }
    
    public boolean isAvailable() {
        return available;
    }
    
    public void setAvailable(boolean available) {
        this.available = available;
    }
    
    @Override
    public String toString() {
        return "TableAvailabilityInfo{" +
                "tableId=" + tableId +
                ", seatingCapacity=" + seatingCapacity +
                ", location='" + location + '\'' +
                ", available=" + available +
                '}';
    }
}
