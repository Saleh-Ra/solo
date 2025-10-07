package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.OCSFMediatorExample.client.ocsf.AbstractClient;
import il.cshaifasweng.OCSFMediatorExample.entities.Branch;
import il.cshaifasweng.OCSFMediatorExample.entities.Cart;
import il.cshaifasweng.OCSFMediatorExample.entities.MenuItem;
import il.cshaifasweng.OCSFMediatorExample.entities.Order;
import il.cshaifasweng.OCSFMediatorExample.entities.Reservation;
import il.cshaifasweng.OCSFMediatorExample.entities.RestaurantTable;
import il.cshaifasweng.OCSFMediatorExample.entities.Warning;
import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javafx.application.Platform;

public class SimpleClient extends AbstractClient {

	private static SimpleClient client = null;
	private static final Cart cart = new Cart();
	private static List<MenuItem> cachedMenuItems = new ArrayList<>();
	private static String currentUserPhone;
	private static String currentUserRole;
	private static String branchName;
	private static int selectedBranchId;
	private static String selectedBranchName;
	private MenuUpdateListener menuUpdateListener;

	private SimpleClient(String host, int port) {
		super(host, port);
		System.out.println("Attempting to connect to server at " + host + ":" + port);
	}

	public static SimpleClient getClient() {
		if (client == null) {
			client = new SimpleClient("localhost", 3000);
		}
		return client;
	}

	public static Cart getCart() {
		return cart;
	}

	public static List<MenuItem> getMenuItems() {
		return cachedMenuItems;
	}
	
	public static String getCurrentUserPhone() {
		return currentUserPhone;
	}
	
	public static void setCurrentUserPhone(String phoneNumber) {
		currentUserPhone = phoneNumber;
		System.out.println("Set current user phone to: " + phoneNumber);
	}

	public static String getCurrentUserRole() {
		return currentUserRole;
	}

	public static void setCurrentUserRole(String role) {
		currentUserRole = role;
		System.out.println("Set current user role to: " + role);
	}

	public static String getBranchName() {
		return branchName;
	}

	public static void setBranchName(String name) {
		branchName = name;
		System.out.println("Set branch name to: " + name);
	}

	public static int getSelectedBranchId() {
		return selectedBranchId;
	}

	public static void setSelectedBranchId(int branchId) {
		selectedBranchId = branchId;
		System.out.println("Set selected branch ID to: " + branchId);
	}

	public static String getSelectedBranchName() {
		return selectedBranchName;
	}

	public static void setSelectedBranchName(String branchName) {
		selectedBranchName = branchName;
		System.out.println("Set selected branch name to: " + branchName);
	}

	@Override
	protected void handleMessageFromServer(Object msg) {
		System.out.println("Received message from server: " + msg);
		// Check if it's a Warning
		if (msg instanceof Warning) {
			Warning warning = (Warning) msg;
			String message = warning.getMessage();
			System.out.println("Handling warning: " + message);
			
			// Handle login responses
			if (message.startsWith("LOGIN_SUCCESS")) {
				try {
					// Extract phone number, role, and branch name from login success message
					String[] parts = message.split(";");
					if (parts.length > 2) {
						String phoneNumber = parts[1];
						String role = parts[2];
						setCurrentUserPhone(phoneNumber);
						setCurrentUserRole(role);
						
                        // Extract branch name if available (for managers)
                        if (parts.length > 3) {
                            String branch = parts[3];
                            setBranchName(branch);
                            // Derive branch id from branch name and store it for unified menu requests
                            try {
                                Integer branchId = null;
                                if (branch != null) {
                                    switch (branch) {
                                        case "Tel-Aviv":
                                            branchId = 1; break;
                                        case "Haifa":
                                            branchId = 2; break;
                                        case "Jerusalem":
                                            branchId = 3; break;
                                        case "Beer-Sheva":
                                            branchId = 4; break;
                                        default: {
                                            String[] partsNum = branch.split("\\D+");
                                            for (String p : partsNum) {
                                                if (!p.isEmpty()) {
                                                    try {
                                                        branchId = Integer.parseInt(p);
                                                        break;
                                                    } catch (NumberFormatException ignored) {}
                                                }
                                            }
                                        }
                                    }
                                }
                                if (branchId != null) {
                                    setSelectedBranchId(branchId);
                                }
                            } catch (Exception ignored) {}
                            System.out.println("Logged in user with phone: " + phoneNumber + ", role: " + role + ", branch: " + branch);
                        } else {
							setBranchName(null);
							System.out.println("Logged in user with phone: " + phoneNumber + " and role: " + role);
						}
						
						// Redirect based on role
						switch (role.toLowerCase()) {
							case "manager":
							case "chain_manager":
							case "customer_support":
							case "nutritionist":
								App.setRoot("secondary2");
								break;
							default: // client
								App.setRoot("personal_area");
								break;
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			} 
			// Handle signup responses
			else if (message.startsWith("SIGNUP_SUCCESS")) {
				try {
					System.out.println("Signup successful, redirecting to sign-in page");
					App.setRoot("sign_in");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			// Post other warnings to EventBus for display
			else {
				EventBus.getDefault().post(new WarningEvent(warning));
			}
		} else if (msg instanceof List) {
			// Check if it's a list of orders
			if (!((List<?>) msg).isEmpty() && ((List<?>) msg).get(0) instanceof Order) {
				List<Order> orders = (List<Order>) msg;
				System.out.println("Received " + orders.size() + " orders from server");
				EventBus.getDefault().post(new OrdersReceivedEvent(orders));
			}
			// Check if it's a list of reservations
			else if (!((List<?>) msg).isEmpty() && ((List<?>) msg).get(0) instanceof Reservation) {
				List<Reservation> reservations = (List<Reservation>) msg;
				System.out.println("Received " + reservations.size() + " reservations from server");
				EventBus.getDefault().post(new ReservationsReceivedEvent(reservations));
			}
			            // Check if it's a list of restaurant tables
            else if (!((List<?>) msg).isEmpty() && ((List<?>) msg).get(0) instanceof RestaurantTable) {
                List<RestaurantTable> tables = (List<RestaurantTable>) msg;
                System.out.println("🟡 SimpleClient: Received " + tables.size() + " restaurant tables from server");
                System.out.println("🟡 SimpleClient: Posting TablesReceivedEvent with " + tables.size() + " tables");
                EventBus.getDefault().post(new TablesReceivedEvent(tables));
                System.out.println("🟡 SimpleClient: TablesReceivedEvent posted to EventBus");
            }
            // Check if it's a list of branches
            else if (!((List<?>) msg).isEmpty() && ((List<?>) msg).get(0) instanceof Branch) {
                List<Branch> branches = (List<Branch>) msg;
                System.out.println("🟡 SimpleClient: Received " + branches.size() + " branches from server");
                EventBus.getDefault().post(new BranchesReceivedEvent(branches));
                System.out.println("🟡 SimpleClient: BranchesReceivedEvent posted to EventBus");
            }
            // Or it's an empty list - check the instance type from generic signature
			else if (((List<?>) msg).isEmpty()) {
				// Since we can't tell what type of empty list it is, post both empty events
				// The controller will handle whichever is relevant
				System.out.println("Received empty list from server");
				EventBus.getDefault().post(new OrdersReceivedEvent(new ArrayList<>()));
				EventBus.getDefault().post(new ReservationsReceivedEvent(new ArrayList<>()));
			}
			// Otherwise assume it's menu items
			else {
				cachedMenuItems = (List<MenuItem>) msg;
				System.out.println("Received " + cachedMenuItems.size() + " menu items from server.");
				if (menuUpdateListener != null) {
					menuUpdateListener.onMenuUpdate(cachedMenuItems);
				}
			}
		} else if (msg instanceof String) {
			// Handle string messages (reports, etc.)
			String message = (String) msg;
			System.out.println("Received string message: " + message);
			
			if (message.startsWith("REPORTS_DATA;")) {
				String reportData = message.substring("REPORTS_DATA;".length());
				EventBus.getDefault().post(new ReportReceivedEvent(reportData));
			} else if (message.startsWith("REPORT_GENERATED;")) {
				String reportData = message.substring("REPORT_GENERATED;".length());
				EventBus.getDefault().post(new ReportReceivedEvent(reportData));
			} else if (message.startsWith("REPORT_ERROR;")) {
				String errorMsg = message.substring("REPORT_ERROR;".length());
				EventBus.getDefault().post(new ReportErrorEvent(errorMsg));
			            } else if (message.startsWith("REPORT_EXPORTED;")) {
                String exportMsg = message.substring("REPORT_EXPORTED;".length());
                EventBus.getDefault().post(new ReportExportEvent(exportMsg));
            } else if (message.equals("NAVIGATE_TO_MAIN")) {
                System.out.println("🟡 SimpleClient: Received NAVIGATE_TO_MAIN command");
                Platform.runLater(() -> {
                    try {
                        App.setRoot("primary1");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
		} else {
			System.out.println("Unhandled message type: " + msg.getClass());
		}
	}

	public void setMenuUpdateListener(MenuUpdateListener listener) {
		this.menuUpdateListener = listener;
	}

	public void placeOrder(Cart cart) throws IOException {
		StringBuilder builder = new StringBuilder("PLACE_ORDER_CART;");
		builder.append(cart.getId()).append(";");

		for (Map.Entry<MenuItem, Integer> entry : cart.getItems().entrySet()) {
			MenuItem item = entry.getKey();
			int quantity = entry.getValue();
			for (int i = 0; i < quantity; i++) {
				builder.append(item.getId()).append(",");
			}
		}

		if (!cart.getItems().isEmpty()) {
			builder.setLength(builder.length() - 1);
		}

		sendToServer(builder.toString());
	}
	
	public void getUserOrders() throws IOException {
		if (currentUserPhone != null && !currentUserPhone.isEmpty()) {
			String message = "GET_USER_ORDERS;" + currentUserPhone;
			sendToServer(message);
			System.out.println("Requesting orders for user with phone: " + currentUserPhone);
		} else {
			System.out.println("Cannot get orders - no current user phone number");
		}
	}

	public void getUserReservations() throws IOException {
		if (currentUserPhone != null && !currentUserPhone.isEmpty()) {
			String message = "GET_USER_RESERVATIONS;" + currentUserPhone;
			sendToServer(message);
			System.out.println("Requesting reservations for user with phone: " + currentUserPhone);
		} else {
			System.out.println("Cannot get reservations - no current user phone number");
		}
	}

	@Override
	public void sendToServer(Object msg) throws IOException {
		System.out.println("Sending to server: " + msg);
		super.sendToServer(msg);
	}
	
	// Event class for orders received
	public static class OrdersReceivedEvent {
		private final List<Order> orders;
		
		public OrdersReceivedEvent(List<Order> orders) {
			this.orders = orders;
		}
		
		public List<Order> getOrders() {
			return orders;
		}
	}

	// Event class for reservations received
	public static class ReservationsReceivedEvent {
		private final List<Reservation> reservations;
		
		public ReservationsReceivedEvent(List<Reservation> reservations) {
			this.reservations = reservations;
		}
		
		public List<Reservation> getReservations() {
			return reservations;
		}
	}
	
	// Event class for reports received
	public static class ReportReceivedEvent {
		private final String reportData;
		
		public ReportReceivedEvent(String reportData) {
			this.reportData = reportData;
		}
		
		public String getReportData() {
			return reportData;
		}
	}
	
	// Event class for restaurant tables received
	public static class TablesReceivedEvent {
		private final List<RestaurantTable> tables;
		
		public TablesReceivedEvent(List<RestaurantTable> tables) {
			this.tables = tables;
		}
		
		public List<RestaurantTable> getTables() {
			return tables;
		}
	}
	
	// Event class for report errors
	public static class ReportErrorEvent {
		private final String errorMessage;
		
		public ReportErrorEvent(String errorMessage) {
			this.errorMessage = errorMessage;
		}
		
		public String getErrorMessage() {
			return errorMessage;
		}
	}
	
	// Event class for report exports
	public static class ReportExportEvent {
		private final String exportMessage;
		
		public ReportExportEvent(String exportMessage) {
			this.exportMessage = exportMessage;
		}
		
		public String getExportMessage() {
			return exportMessage;
		}
	}
	
	// Event class for branches received
	public static class BranchesReceivedEvent {
		private final List<Branch> branches;
		
		public BranchesReceivedEvent(List<Branch> branches) {
			this.branches = branches;
		}
		
		public List<Branch> getBranches() {
			return branches;
		}
	}
}
