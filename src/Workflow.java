import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;

public class Workflow {
    private final DataPersistence dp;
    private final Log log;
    private final ValidationService validator;
    private final InventoryService inventory;
    private final InvoiceService invoiceService;
    private final PaymentService paymentService;
    private final ShippingService shippingService;

    public Workflow(DataPersistence dp, Log log,
                    ValidationService validator,
                    InventoryService inventory,
                    InvoiceService invoiceService,
                    PaymentService paymentService,
                    ShippingService shippingService) {
        this.dp = dp;
        this.log = log;
        this.validator = validator;
        this.inventory = inventory;
        this.invoiceService = invoiceService;
        this.paymentService = paymentService;
        this.shippingService = shippingService;
    }

    public boolean adminLogin(BufferedReader console) throws Exception {
        System.out.print("\n" + Utils.ANSI_BLUE + "Admin Login Required" + Utils.ANSI_RESET + "\n");
        return Admin.authenticate(dp, console);
    }

    public void adminDashboard(BufferedReader console) throws Exception {
        while (true) {
            printMenu();
            String choice = readTrim(console);
            if ("1".equals(choice)) {
                dp.printOrdersSummary();
            } else if ("2".equals(choice)) {
                acceptOrder(console);
            } else if ("3".equals(choice)) {
                String next = dp.findNextPendingOrderId();
                if (next == null) {
                    System.out.print(Utils.ANSI_PINK + "No PENDING orders." + Utils.ANSI_RESET + "\n");
                } else {
                    processOrder(next, console);
                }
            } else if ("4".equals(choice)) {
                showPurchaseHistory();
            } else if ("5".equals(choice)) {
                generateStockReport();
            } else if ("6".equals(choice)) {
                break;
            } else {
                System.out.print(Utils.ANSI_PINK + "Invalid." + Utils.ANSI_RESET + "\n");
            }
        }
    }

    private void printMenu() {
        System.out.print("\n" + Utils.ANSI_BLUE + "--- Admin Dashboard ---" + Utils.ANSI_RESET + "\n");
        System.out.print(Utils.ANSI_BLUE + "1) View Orders\n2) Accept New Order (Order Intake)\n3) Process Next PENDING Order\n4) View Purchase History\n5) View Stock Report\n6) Exit\nChoose: " + Utils.ANSI_RESET);
    }

    private void acceptOrder(BufferedReader console) throws Exception {
        String oid = "O" + Utils.padLeft(dp.orderSerial++, 4);
        System.out.print(Utils.ANSI_BLUE + "New Order ID: " + oid + Utils.ANSI_RESET + "\n");
        String address = ask(console, "Enter Address: ");
        String pay = ask(console, "Enter Payment Mode (COD/MockCard): ");

        Order o = new Order();
        o.orderId = oid;
        o.address = address;
        o.paymentMode = pay;
        o.status = "PENDING";

        System.out.print("\n" + Utils.ANSI_BLUE + "Available Products:" + Utils.ANSI_RESET + "\n");
        for (int i = 0; i < dp.productCount; i++) {
            Product p = dp.products[i];
            System.out.print((i + 1) + ") " + p.productId + " - " + p.name +
                    " (Price: " + p.price + ", Stock: " + p.stock + ")\n");
        }

        while (true) {
            String choiceStr = ask(console, "Enter product number (0 to finish): ");
            int choiceNum = toInt(choiceStr);
            if (choiceNum == 0) break;
            if (choiceNum < 1 || choiceNum > dp.productCount) {
                System.out.print(Utils.ANSI_PINK + "Invalid product selection." + Utils.ANSI_RESET + "\n");
                continue;
            }
            Product selected = dp.products[choiceNum - 1];
            String qtyStr = ask(console, "Enter quantity for " + selected.name + ": ");
            int qty = toInt(qtyStr);
            if (qty <= 0) {
                System.out.print(Utils.ANSI_PINK + "Quantity must be positive." + Utils.ANSI_RESET + "\n");
                continue;
            }
            if (qty > selected.stock) {
                System.out.print(Utils.ANSI_PINK + "Only " + selected.stock + " in stock for " + selected.productId + "." + Utils.ANSI_RESET + "\n");
                continue;
            }
            o.addItem(new Item(selected.productId, qty));
            System.out.print("Added " + qty + " x " + selected.name + " to order.\n");
        }

        if (o.itemCount <= 0) {
            System.out.print(Utils.ANSI_PINK + "No items selected. Order canceled." + Utils.ANSI_RESET + "\n");
            return;
        }

        dp.addOrder(o);
        log.write(o.orderId, "ORDER_INTAKE", "OK", "Order accepted into system (PENDING)");
        System.out.print(Utils.ANSI_PINK + "✅ Order " + o.orderId + " accepted and saved as PENDING." + Utils.ANSI_RESET + "\n");
    }

    private void processOrder(String orderId, BufferedReader console) throws Exception {
        Order o = dp.findOrderById(orderId);
        if (o == null) return;

        if (!validator.validate(o)) {
            cancel(o, "Validation failed");
            return;
        }
        if (!inventory.verifyAndReserve(o)) {
            cancel(o, "Inventory insufficient");
            return;
        }

        Invoice inv = invoiceService.generateInvoice(o);
        if (!paymentService.processPayment(o, console)) {
            inventory.rollbackReservation(o);
            cancel(o, "Payment failed");
            return;
        }

        Shipment sh = shippingService.createShipment(o);
        o.status = "COMPLETED";
        log.write(o.orderId, "ORDER_COMPLETE", "OK", "Invoice=" + inv.invoiceId + " Tracking=" + sh.trackingId);

        try {
            FileWriter fw = new FileWriter(dp.path("purchase_history.txt"), true);
            fw.write(o.orderId + "|" + o.paymentMode + "|" + o.status + "|" + o.totalAmount + "\n");
            fw.close();
        } catch (Exception e) {}

        System.out.print(Utils.ANSI_PINK + "✅ COMPLETED" + Utils.ANSI_RESET + "\n");
    }

    private void cancel(Order o, String reason) {
        o.status = "CANCELLED";
        o.cancelReason = reason;
        log.write(o.orderId, "CANCEL", "OK", reason);
        try {
            FileWriter fw = new FileWriter(dp.path("purchase_history.txt"), true);
            fw.write(o.orderId + "|" + o.paymentMode + "|" + o.status + "|" + o.totalAmount + "\n");
            fw.close();
        } catch (Exception e) {}
        System.out.print(Utils.ANSI_PINK + "❌ CANCELLED: " + reason + Utils.ANSI_RESET + "\n");
    }

    private void showPurchaseHistory() {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(dp.path("purchase_history.txt")));
            System.out.print("\n" + Utils.ANSI_BLUE + "Purchase History:" + Utils.ANSI_RESET + "\n");
            System.out.print(Utils.ANSI_BLUE +
                    Utils.pad("OrderID", 10) +
                    Utils.pad("Payment Mode", 15) +
                    Utils.pad("Status", 12) +
                    Utils.pad("Total", 8) +
                    Utils.ANSI_RESET + "\n");

            String line;
            boolean foundAny = false;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.length() == 0) continue;
                String[] parts = line.split("\\|");
                if (parts.length < 4) continue;
                System.out.print(
                        Utils.pad(parts[0], 10) +
                        Utils.pad(parts[1], 15) +
                        Utils.pad(parts[2], 12) +
                        Utils.pad(parts[3], 8) + "\n"
                );
                foundAny = true;
            }
            if (!foundAny) {
                System.out.print("No purchase history available.\n");
            }
        } catch (Exception e) {
            System.out.print("No purchase history available.\n");
        } finally {
            try { if (br != null) br.close(); } catch (Exception ex) {}
        }
    }

    private void generateStockReport() {
        FileWriter fw = null;
        try {
            fw = new FileWriter(dp.path("stock_report.txt"), false);
            System.out.print("\n" + Utils.ANSI_BLUE + "Stock Report:" + Utils.ANSI_RESET + "\n");
            System.out.print(Utils.ANSI_BLUE +
                    Utils.pad("ProductID", 12) +
                    Utils.pad("Name", 20) +
                    Utils.pad("Stock", 8) +
                    Utils.ANSI_RESET + "\n");
            fw.write("ProductID|Name|Stock\n");

            for (int i = 0; i < dp.productCount; i++) {
                Product p = dp.products[i];
                String line = p.productId + "|" + p.name + "|" + p.stock;
                fw.write(line + "\n");
                System.out.print(
                        Utils.pad(p.productId, 12) +
                        Utils.pad(p.name, 20) +
                        Utils.pad(String.valueOf(p.stock), 8) + "\n"
                );
            }
        } catch (Exception e) {
            System.out.print("Error generating stock report.\n");
        } finally {
            try { if (fw != null) fw.close(); } catch (Exception ex) {}
        }
    }

    private String ask(BufferedReader console, String prompt) throws Exception {
        System.out.print(prompt);
        return readTrim(console);
    }

    private String readTrim(BufferedReader console) throws Exception {
        String s = console.readLine();
        return safe(s);
    }

    private String safe(String s) {
        return (s == null ? "" : s.trim());
    }

    private int toInt(String s) {
        s = safe(s);
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') break;
            n = n * 10 + (c - '0');
        }
        return n;
    }
}

