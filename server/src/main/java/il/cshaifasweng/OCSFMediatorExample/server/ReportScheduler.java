package il.cshaifasweng.OCSFMediatorExample.server;

import il.cshaifasweng.OCSFMediatorExample.entities.*;
import il.cshaifasweng.OCSFMediatorExample.server.dataManagers.DataManager;
import il.cshaifasweng.OCSFMediatorExample.server.ocsf.ConnectionToClient;

import java.io.IOException;
import java.util.*;

/*to be added:
String report = "Total guests this month: " + branch.getMonthlyVisitCount()
             + "\nOrders: " + serializeOrders(branch.getMonthlyOrders());*/
public class ReportScheduler {

    private static final Timer timer = new Timer();

    public static void scheduleMonthlyReportTask() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1); // First day of the month
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        Date firstOfMonth = calendar.getTime();
        long period = 1000L * 60 * 60 * 24 * 30; // Roughly 1 month

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                generateAndSendReports();
            }
        }, firstOfMonth, period);
    }

    public static void generateAndSendReports() {
        RestaurantChainManager ChainManager=DataManager.fetchAll(RestaurantChainManager.class).get(0);
        List<BranchManager> managers=DataManager.fetchAll(BranchManager.class);
        int totalVisitors=0;
        List<Order> orders=DataManager.fetchAll(Order.class);
        for(BranchManager manager:managers){
            Branch branch=manager.getBranch();
            String summary = "Total guests this month: " + branch.getMonthlyVisitCount()+
                    "Total orders this month: "+ branch.getMonthlyOrders();
            totalVisitors+=branch.getMonthlyVisitCount();
            orders.addAll(branch.getMonthlyOrders());
            ConnectionToClient client = SimpleServer.getClientByPhone(
                    manager.getManager().getPhoneNumber()
            );
            try{
                client.sendToClient("monthly summary: "+ summary);
            }catch (IOException e) {
                e.printStackTrace();
            }
            //send to manager
        }
        //send to chain manager

        // Send results using sendToManagers("monthly_report;" + summary);
        System.out.println("Monthly report sent to managers.");
    }
}
