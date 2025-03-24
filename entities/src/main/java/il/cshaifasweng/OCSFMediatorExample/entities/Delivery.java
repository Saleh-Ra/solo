package il.cshaifasweng.OCSFMediatorExample.entities;

import java.util.Date;

public class Delivery {//d.id c.id orderid d.adress-string d.time-datetime status-string
    int DeliveryID;
    int ClientID;
    int OrderID;
    String DeliveryAddress;
    Date DeliveryDate;
    String Status;

    public Delivery(int deliveryID, int clientID, int orderID, String deliveryAddress, Date deliveryDate, String status) {
        DeliveryID = deliveryID;
        ClientID = clientID;
        OrderID = orderID;
        DeliveryAddress = deliveryAddress;
        DeliveryDate = deliveryDate;
        Status = status;
    }

    public int getDeliveryID() {
        return DeliveryID;
    }

    public void setDeliveryID(int deliveryID) {
        DeliveryID = deliveryID;
    }

    public int getClientID() {
        return ClientID;
    }

    public void setClientID(int clientID) {
        ClientID = clientID;
    }

    public int getOrderID() {
        return OrderID;
    }

    public void setOrderID(int orderID) {
        OrderID = orderID;
    }

    public String getDeliveryAddress() {
        return DeliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        DeliveryAddress = deliveryAddress;
    }

    public Date getDeliveryDate() {
        return DeliveryDate;
    }

    public void setDeliveryDate(Date deliveryDate) {
        DeliveryDate = deliveryDate;
    }

    public String getStatus() {
        return Status;
    }

    public void setStatus(String status) {
        Status = status;
    }
}
