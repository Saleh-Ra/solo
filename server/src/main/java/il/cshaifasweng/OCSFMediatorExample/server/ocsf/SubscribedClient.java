package il.cshaifasweng.OCSFMediatorExample.server.ocsf;

public class SubscribedClient {
    private ConnectionToClient client;

    public SubscribedClient(ConnectionToClient client) {
        this.client = client;
    }

    public ConnectionToClient getClient() {
        return client;
    }

    public void setClient(ConnectionToClient client) {
        this.client = client;
    }
    private String phoneNumber;
    public Object getPhoneNumber() {
        return phoneNumber;
    }
    public void setPhoneNumber(String number){
        this.phoneNumber=number;
    }
}