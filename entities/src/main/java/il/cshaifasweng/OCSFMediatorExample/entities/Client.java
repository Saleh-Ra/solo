package il.cshaifasweng.OCSFMediatorExample.entities;

public class Client {
    int ClientID;
    String ClientName;
    String ContactDetails;

    public Client(int clientID, String clientName, String contactDetails) {
        ClientID = clientID;
        ClientName = clientName;
        ContactDetails = contactDetails;
    }

    public int getClientID() {
        return ClientID;
    }

    public void setClientID(int clientID) {
        ClientID = clientID;
    }

    public String getClientName() {
        return ClientName;
    }

    public void setClientName(String clientName) {
        ClientName = clientName;
    }

    public String getContactDetails() {
        return ContactDetails;
    }

    public void setContactDetails(String contactDetails) {
        ContactDetails = contactDetails;
    }
}
