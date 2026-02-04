import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Main {
    public static void main(String[] args) {
        try {
            BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
            DataPersistence dp = new DataPersistence("data");
            dp.loadAll();
            Log log = new Log(dp);
            ValidationService validator = new ValidationService(log);
            InventoryService inventory = new InventoryService(dp, log);
            InvoiceService invoiceService = new InvoiceService(dp, log);
            PaymentService paymentService = new PaymentService(log);
            ShippingService shippingService = new ShippingService(dp, log);
            Workflow wf = new Workflow(dp, log, validator, inventory, invoiceService, paymentService, shippingService);
            System.out.print("=== Order Fulfillment Automation ===\n");
            if (!wf.adminLogin(console)) {
                System.out.print("Exiting...\n");
                return;
            }
            wf.adminDashboard(console);
            dp.saveAll();
            System.out.print("Saved. Bye.\n");
        } catch (Exception e) {
            System.out.print("Fatal Error: " + e.getMessage() + "\n");
        }
    }
}

