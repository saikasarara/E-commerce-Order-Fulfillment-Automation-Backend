import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.security.MessageDigest;

public class Main {
    public static void main(String[] args) {
        try {
            // Set up console input
            BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
            // Initialize data persistence and load data from text files
            DataPersistence dp = new DataPersistence("");  // base directory "" for current
            dp.loadAll();
            // Initialize logging system
            Log log = new Log(dp);
            // Secure Admin Login
            boolean loginSuccess = Admin.authenticate(dp, console);
            if (!loginSuccess) {
                System.out.print("Exiting...\n");
                return;
            }
            // Launch Admin Dashboard (CLI menu)
            AdminDashboard dashboard = new AdminDashboard(dp, log, console);
            dashboard.showMenu();
            // On exit, save all data back to files
            dp.saveAll();
            System.out.print("Saved. Bye.\n");
        } catch (Exception e) {
            System.out.print("Fatal Error: " + e.getMessage() + "\n");
        }
    }
}



