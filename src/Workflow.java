import java.io.BufferedReader;

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
        System.out.print("\nAdmin Login Required\n");
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
                    System.out.print("No PENDING orders.\n");
                } else {
                    processOrder(next, console);
                }
            } else if ("4".equals(choice)) {
                dp.printPurchaseHistory();
            } else if ("5".equals(choice)) {
                dp.printStockOverview();
            } else if ("6".equals(choice)) {
                break;
            } else {
                System.out.print("Invalid.\n");
            }
        }
    }

    private void printMenu() {
        String pink = Utils.ANSI_MAGENTA;
        String blue = Utils.ANSI_BLUE;
        String reset = Utils.ANSI_RESET;
        // Colorized dashboard box
        System.out.print("\n" + pink + "╔═════════════════╗\n");
        System.out.print("║ " + blue + "Admin Dashboard" + pink + " ║\n");
        System.out.print("╚═════════════════╝" + reset + "\n");
        System.out.print("1) View Orders\n");
        System.out.print("2) Accept New Order\n");
        System.out.print("3) Process Next PENDING Order\n");
        System.out.print("4) View Purchase History\n");
        System.out.print("5) View Stock Overview\n");
        System.out.print("6) Exit\n");
        System.out.print("Choose: ");
    }

    private void acceptOrder(BufferedReader console) throws Exception {
        // Auto-generate Order ID
        String oid = "O" + dp.orderSerial++;
        System.out.print("Generated Order ID: " + oid + "\n");
        String address = ask(console, "Enter Address: ");
        String pay = ask(console, "Enter Payment Mode (COD/MockCard): ");
        Order o = new Order();
        o.orderId = oid;
        o.address = address;
        o.paymentMode = pay;
        o.status = "PENDING";
        // Menu-based product selection
        System.out.print("Available Products:\n");
        int idx = 1;
        int i = 0;
        while (i < dp.productCount) {
            Product p = dp.products[i];
            System.out.print(idx + ") " + p.productId + " - " + p.name + " (" + p.brand + ") - BDT " + p.price + " [" + p.stock + " in stock]\n");
            i++;
            idx++;
        }
        System.out.print("0) Done adding items\n");
        while (true) {
            System.out.print("Select product by number (0 to finish): ");
            String choice = readTrim(console);
            int choiceNum = toInt(choice);
            if (choiceNum <= 0) {
                break;
            }
            if (choiceNum > dp.productCount) {
                System.out.print("Invalid selection.\n");
                continue;
            }
            Product selected = dp.products[choiceNum - 1];
            String qtyStr = ask(console, "Enter quantity: ");
            int qty = toInt(qtyStr);
            if (qty <= 0) {
                System.out.print("Invalid quantity.\n");
                continue;
            }
            o.addItem(new Item(selected.productId, qty));
            if (o.itemCount >= 50) {
                System.out.print("Item limit reached.\n");
                break;
            }
        }
        dp.addOrder(o);
        log.write(o.orderId, "ORDER_INTAKE", "OK", "Order accepted into system (PENDING)");
        System.out.print("✅ Order " + o.orderId + " accepted and saved as PENDING.\n");
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
        dp.addPurchaseHistory(o);
        System.out.print("✅ COMPLETED\n");
    }

    private void cancel(Order o, String reason) {
        o.status = "CANCELLED";
        o.cancelReason = reason;
        log.write(o.orderId, "CANCEL", "OK", reason);
        System.out.print("❌ CANCELLED: " + reason + "\n");
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
        if (s == null) return "";
        return s.trim();
    }

    private int toInt(String s) {
        int n = 0, i = 0;
        s = safe(s);
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') break;
            n = n * 10 + (c - '0');
            i++;
        }
        return n;
    }
}


