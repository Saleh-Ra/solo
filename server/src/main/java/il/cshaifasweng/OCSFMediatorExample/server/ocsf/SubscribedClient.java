package il.cshaifasweng.OCSFMediatorExample.server.ocsf;

public class SubscribedClient {
    private ConnectionToClient client;
    private String phoneNumber;
    private Integer branchId; // Store the branch the client is currently viewing

    public SubscribedClient(ConnectionToClient client) {
        this.client = client;
    }

    public ConnectionToClient getClient() {
        return client;
    }

    public void setClient(ConnectionToClient client) {
        this.client = client;
    }
    
    public Object getPhoneNumber() {
        return phoneNumber;
    }
    
    public void setPhoneNumber(String number){
        this.phoneNumber=number;
    }
    
    public Integer getBranchId() {
        return branchId;
    }
    
    public void setBranchId(Integer branchId) {
        this.branchId = branchId;
    }
}