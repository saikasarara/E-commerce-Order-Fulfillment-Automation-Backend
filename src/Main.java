import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Main {
    public static void main(String[] args) {
        try {
            BufferedReader console = new BufferedReader(new InputStreamReader(System.in));

            // Load all data from ./data/
            DataPersistence dp = new DataPersistence("data");
            dp.loadAll();

            // Set up shared logger
            Log log = new Log(dp);

            // Create service instances
            ValidationService validator = new ValidationService(log);
            InventoryService inventory = new InventoryService(dp, log);
            InvoiceService invoice = new InvoiceService(dp, log);
            PaymentService payment = new PaymentService(log);
            ShippingService shipping = new ShippingService(dp, log);

            // Run main workflow
            Workflow flow = new Workflow(dp, log, validator, inventory, invoice, payment, shipping);
            if (flow.adminLogin(console)) {
                flow.adminDashboard(console);
            }

            // Save updated data
            dp.saveAll();

            System.out.print("\n" + Utils.ANSI_PINK + "Exiting system. Goodbye!" + Utils.ANSI_RESET + "\n");

        } catch (Exception e) {
            System.out.print("Fatal error: " + e.getMessage() + "\n");
        }
    }
}
