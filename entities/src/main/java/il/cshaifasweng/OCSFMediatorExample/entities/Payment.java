package il.cshaifasweng.OCSFMediatorExample.entities;

public class Payment {
    int PaymentID;
    int ClientID;
    int OrderID;
    double Amount;
    String PaymentMethod;
    String Status;

    public Payment(int paymentID, int clientID, int orderID, double amount, String paymentMethod, String status) {
        PaymentID = paymentID;
        ClientID = clientID;
        OrderID = orderID;
        Amount = amount;
        PaymentMethod = paymentMethod;
        Status = status;
    }

    public int getPaymentID() {
        return PaymentID;
    }

    public void setPaymentID(int paymentID) {
        PaymentID = paymentID;
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

    public double getAmount() {
        return Amount;
    }

    public void setAmount(double amount) {
        Amount = amount;
    }

    public String getPaymentMethod() {
        return PaymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        PaymentMethod = paymentMethod;
    }

    public String getStatus() {
        return Status;
    }

    public void setStatus(String status) {
        Status = status;
    }
}
