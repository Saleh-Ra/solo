package il.cshaifasweng.OCSFMediatorExample.entities;

public class Complaint {
        private int complaintId;
        private int clientId;
        private int branchId;
        private String description;
        private String status;

        public Complaint(int complaintId, int clientId, int branchId, String description, String status) {
            this.complaintId = complaintId;
            this.clientId = clientId;
            this.branchId = branchId;
            this.description = description;
            this.status = status;
        }

        public int getComplaintId() {
            return complaintId;
        }

        public void setComplaintId(int complaintId) {
            this.complaintId = complaintId;
        }

        public int getClientId() {
            return clientId;
        }

        public void setClientId(int clientId) {
            this.clientId = clientId;
        }

        public int getBranchId() {
            return branchId;
        }

        public void setBranchId(int branchId) {
            this.branchId = branchId;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
}
