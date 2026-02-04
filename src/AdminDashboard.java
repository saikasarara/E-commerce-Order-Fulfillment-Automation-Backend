import java.time.LocalDate;
import java.io.BufferedReader;

public class AdminDashboard {
    private DataPersistence dp;
    private Log log;
    private BufferedReader console;

    public AdminDashboard(DataPersistence dp, Log log, BufferedReader console) {
        this.dp = dp;
        this.log = log;
        this.console = console;
    }

    /** Main CLI menu loop for admin dashboard */
    public void showMenu() throws Exception {
        String choice = "";
        // Loop until exit option (0) is chosen
        while (true) {
            // Display menu options
            System.out.print("\n==== Admin Dashboard ====\n");
            System.out.print("1. View All Orders\n");
            System.out.print("2. Add New Order\n");
            System.out.print("3. Update Order Status\n");
            System.out.print("4. View Order Timeline\n");
            System.out.print("5. Generate Report\n");
            System.out.print("6. Low Stock Alerts\n");
            System.out.print("7. Export Stock Report\n");
            System.out.print("8. Bulk Import Orders\n");
            System.out.print("9. Simulation Mode\n");
            System.out.print("10. Retry Failed Order\n");
            System.out.print("11. Archive Delivered Orders\n");
            System.out.print("12. Restock Product\n");
            System.out.print("13. Generate Receipt\n");
            System.out.print("0. Exit\n");
            System.out.print("Choose an option: ");
            choice = console.readLine();
            if (choice == null) choice = "";
            choice = choice.trim();
            if (choice.equals("0")) {
                break;
            }
            switch (choice) {
                case "1":
                    // View all orders in a formatted table
                    displayAllOrders();
                    break;
                case "2":
                    // Add a new order via CLI input
                    addNewOrder();
                    break;
                case "3":
                    // Manually update an order's status (process pending or advance shipping status)
                    handleStatusUpdate();
                    break;
                case "4":
                    // View order timeline from logs
                    System.out.print("Enter Order ID to view timeline: ");
                    String logId = console.readLine();
                    if (logId == null) logId = "";
                    logId = logId.trim().toUpperCase();
                    if (!logId.startsWith("O")) {
                        logId = "O" + logId;
                    }
                    log.showOrderTimeline(logId);
                    break;
                case "5":
                    // Generate a report (revenue, cancellations)
                    ReportService.generateReport(dp);
                    break;
                case "6":
                    // Show low stock alerts
                    InventoryService.showLowStockAlerts(dp);
                    break;
                case "7":
                    // Export current stock report to file
                    ReportService.exportStockReport(dp);
                    break;
                case "8":
                    // Bulk import orders from file
                    importOrdersFromFile();
                    break;
                case "9":
                    // Run simulation mode
                    runSimulation();
                    break;
                case "10":
                    // Retry a failed (cancelled) order
                    retryCancelledOrder();
                    break;
                case "11":
                    // Archive delivered orders
                    archiveDeliveredOrders();
                    break;
                case "12":
                    // Restock system: admin adds stock to a product
                    System.out.print("Enter Product ID to restock: ");
                    String pid = console.readLine();
                    if (pid == null) pid = "";
                    pid = pid.trim();
                    System.out.print("Enter quantity to add: ");
                    String qtyStr = console.readLine();
                    if (qtyStr == null) qtyStr = "";
                    qtyStr = qtyStr.trim();
                    int qty = DataPersistence.toInt(qtyStr);
                    boolean ok = InventoryService.restockProduct(pid, qty, dp, log);
                    if (ok) {
                        System.out.print("Product " + pid + " stock increased by " + qty + ".\n");
                    } else {
                        System.out.print("Restock failed (invalid product or quantity).\n");
                    }
                    break;
                case "13":
                    // Generate receipt for a delivered order
                    generateReceipt();
                    break;
                default:
                    System.out.print("Invalid choice. Please try again.\n");
            }
        }
    }

    /** Display all orders in a table format with aligned columns */
    private void displayAllOrders() {
        if (dp.orderCount == 0) {
            System.out.print("(No orders)\n");
            return;
        }
        // Table header
        System.out.print(String.format("%-8s %-12s %-15s %s\n", "OrderID", "Date", "Status", "Total(BDT)"));
        for (int i = 0; i < dp.orderCount; i++) {
            Order o = dp.orders[i];
            if (o == null) continue;
            String id = o.orderId;
            String date = o.date;
            String status = o.status;
            String totalStr = String.valueOf(o.totalAmount);
            // Pad or truncate fields to fit columns (using simple formatting)
            System.out.print(String.format("%-8s %-12s %-15s %s\n", id, date, status, totalStr));
        }
    }

    /** Add a new order by taking product IDs and quantities from admin input */
    private void addNewOrder() throws Exception {
        // Create a new Order object
        Order newOrder = new Order();
        newOrder.orderId = dp.generateOrderId();
        // Use current date as order date (YYYY-MM-DD)
        newOrder.date = LocalDate.now().toString();
        // Prompt admin to enter items
        System.out.print("Enter items for the order (format: ProductIDxQuantity, e.g. M101x2, P501x1):\n");
        System.out.print("Items: ");
        String itemsInput = console.readLine();
        if (itemsInput == null) itemsInput = "";
        itemsInput = itemsInput.trim();
        if (itemsInput.equals("")) {
            System.out.print("No items entered. Order cancelled.\n");
            return;
        }
        // Parse the items input into the order
        dp.parseItemsIntoOrder(newOrder, itemsInput);
        if (newOrder.itemCount == 0) {
            System.out.print("No valid items. Order cancelled.\n");
            return;
        }
        // Calculate total amount for the order
        int total = 0;
        for (int j = 0; j < newOrder.itemCount; j++) {
            Item it = newOrder.items[j];
            Product prod = dp.findProductById(it.productId);
            total += (prod != null ? prod.price : 0) * it.quantity;
        }
        newOrder.totalAmount = total;
        // Process the order through inventory and payment
        boolean invOk = InventoryService.validateAndReserve(newOrder, dp, log);
        if (!invOk) {
            // Order was cancelled during validation (invalid product or shortage)
            dp.orders[dp.orderCount++] = newOrder;  // keep record of the failed order
            System.out.print("Order " + newOrder.orderId + " failed (" + newOrder.cancelReason + ").\n");
            return;
        }
        // Inventory reserved successfully, now simulate payment
        boolean paid = PaymentService.simulatePayment(newOrder);
        if (!paid) {
            // Payment failed: rollback stock
            for (int j = 0; j < newOrder.itemCount; j++) {
                Item it = newOrder.items[j];
                Product prod = dp.findProductById(it.productId);
                if (prod != null) prod.stock += it.quantity;
            }
            newOrder.status = "CANCELLED";
            newOrder.cancelReason = "Payment Failed";
            log.write(newOrder.orderId, "Order cancelled – Payment failed");
            dp.orders[dp.orderCount++] = newOrder;
            System.out.print("Order " + newOrder.orderId + " failed (Payment Failed).\n");
            return;
        }
        // Payment succeeded
        log.write(newOrder.orderId, "Payment successful");
        newOrder.status = "PACKED";
        log.write(newOrder.orderId, "Status changed to PACKED");
        // At this point, order is packed and ready for shipping
        dp.orders[dp.orderCount++] = newOrder;
        // Generate invoice for the order
        InvoiceService.generateInvoice(newOrder, dp);
        System.out.print("Order " + newOrder.orderId + " added successfully. Status: PACKED.\n");
    }

    /** Handle manual status update (process pending orders or advance shipping status for packed/shipped orders) */
    private void handleStatusUpdate() throws Exception {
        System.out.print("Enter Order ID to update status: ");
        String id = console.readLine();
        if (id == null) id = "";
        id = id.trim();
        if (id.equals("")) {
            System.out.print("Order ID cannot be empty.\n");
            return;
        }
        // Normalize order ID (prepend 'O' if missing)
        if (!id.toUpperCase().startsWith("O")) {
            id = "O" + id;
        } else {
            id = id.toUpperCase();
        }
        // Find the order
        Order order = null;
        int orderIndex = -1;
        for (int i = 0; i < dp.orderCount; i++) {
            Order o = dp.orders[i];
            if (o != null && o.orderId.equalsIgnoreCase(id)) {
                order = o;
                orderIndex = i;
                break;
            }
        }
        if (order == null) {
            System.out.print("Order " + id + " not found.\n");
            return;
        }
        String currentStatus = order.status;
        if (currentStatus.equals("DELIVERED") || currentStatus.equals("CANCELLED")) {
            System.out.print("Order " + id + " is " + currentStatus + "; status cannot be changed.\n");
            return;
        }
        // If order is still PENDING (not processed yet), process it first through inventory & payment
        if (currentStatus.equals("PENDING")) {
            boolean processed = InventoryService.validateAndReserve(order, dp, log);
            if (!processed) {
                // Order cancelled during processing
                System.out.print("Order processing failed. Status updated to CANCELLED (" + order.cancelReason + ").\n");
                return;
            }
            // Inventory reserved, now simulate payment
            // Calculate total if not already
            if (order.totalAmount == 0) {
                int total = 0;
                for (int j = 0; j < order.itemCount; j++) {
                    Item it = order.items[j];
                    Product prod = dp.findProductById(it.productId);
                    total += (prod != null ? prod.price : 0) * it.quantity;
                }
                order.totalAmount = total;
            }
            boolean paid = PaymentService.simulatePayment(order);
            if (!paid) {
                // Rollback inventory
                for (int j = 0; j < order.itemCount; j++) {
                    Item it = order.items[j];
                    Product prod = dp.findProductById(it.productId);
                    if (prod != null) prod.stock += it.quantity;
                }
                order.status = "CANCELLED";
                order.cancelReason = "Payment Failed";
                log.write(order.orderId, "Order cancelled – Payment failed");
                System.out.print("Order processing failed. Status updated to CANCELLED (" + order.cancelReason + ").\n");
                return;
            }
            // Payment succeeded
            log.write(order.orderId, "Payment successful");
            order.status = "PACKED";
            log.write(order.orderId, "Status changed to PACKED");
            currentStatus = "PACKED";
            // Generate invoice since payment successful
            InvoiceService.generateInvoice(order, dp);
        }
        // Advance the status to the next step in shipping workflow
        String nextStatus = null;
        if (currentStatus.equals("PACKED")) {
            nextStatus = "SHIPPED";
        } else if (currentStatus.equals("SHIPPED")) {
            nextStatus = "OUT_FOR_DELIVERY";
        } else if (currentStatus.equals("OUT_FOR_DELIVERY")) {
            nextStatus = "DELIVERED";
        }
        if (nextStatus == null) {
            System.out.print("No further status transition available for " + currentStatus + ".\n");
            return;
        }
        // Perform the status transition
        order.status = nextStatus;
        if (nextStatus.equals("SHIPPED")) {
            // Assign tracking ID when order is shipped
            order.trackingId = "TRK" + (order.orderId.length() > 1 ? order.orderId.substring(1) : order.orderId);
            // Record shipment in shipments.txt
            try {
                FileWriter fw = new FileWriter(dp.path("shipments.txt"), true);
                fw.write(order.orderId + "|" + order.trackingId + "|" + "SHIPPED" + "\n");
                fw.close();
            } catch (Exception e) {
                // ignore
            }
        }
        if (nextStatus.equals("DELIVERED")) {
            // When delivered, record final shipment status
            try {
                FileWriter fw = new FileWriter(dp.path("shipments.txt"), true);
                fw.write(order.orderId + "|" + order.trackingId + "|" + "DELIVERED" + "\n");
                fw.close();
            } catch (Exception e) { }
        }
        log.write(order.orderId, "Status changed to " + nextStatus);
        System.out.print("Order " + order.orderId + " status updated to " + nextStatus + ".\n");
    }

    /** Bulk import orders from an external file (e.g., "orders_import.txt") */
    private void importOrdersFromFile() throws Exception {
        BufferedReader br = null;
        int importedCount = 0;
        try {
            br = new BufferedReader(new FileReader(dp.path("orders_import.txt")));
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.length() == 0) continue;
                // Assuming import format: Date|ItemList  (e.g., "2026-02-10|M101x1, P501x2")
                String[] parts = line.split("\\|");
                if (parts.length < 2) {
                    System.out.print("Skipping invalid import line: " + line + "\n");
                    continue;
                }
                String dateStr = parts[0].trim();
                String itemsPart = parts[1].trim();
                Order newOrder = new Order();
                newOrder.orderId = dp.generateOrderId();
                newOrder.date = (dateStr.equals("") ? LocalDate.now().toString() : dateStr);
                dp.parseItemsIntoOrder(newOrder, itemsPart);
                if (newOrder.itemCount == 0) {
                    System.out.print("Skipping import order " + newOrder.orderId + " (no valid items).\n");
                    continue;
                }
                // Calculate total
                int total = 0;
                for (int j = 0; j < newOrder.itemCount; j++) {
                    Item it = newOrder.items[j];
                    Product prod = dp.findProductById(it.productId);
                    total += (prod != null ? prod.price : 0) * it.quantity;
                }
                newOrder.totalAmount = total;
                // Process order (inventory + payment)
                boolean processed = InventoryService.validateAndReserve(newOrder, dp, log);
                if (!processed) {
                    // failed during validation
                    dp.orders[dp.orderCount++] = newOrder;
                    System.out.print("Imported Order " + newOrder.orderId + " (CANCELLED, " + newOrder.cancelReason + ")\n");
                } else {
                    boolean paid = PaymentService.simulatePayment(newOrder);
                    if (!paid) {
                        // rollback inventory
                        for (int j = 0; j < newOrder.itemCount; j++) {
                            Item it = newOrder.items[j];
                            Product prod = dp.findProductById(it.productId);
                            if (prod != null) prod.stock += it.quantity;
                        }
                        newOrder.status = "CANCELLED";
                        newOrder.cancelReason = "Payment Failed";
                        log.write(newOrder.orderId, "Order cancelled – Payment failed");
                        dp.orders[dp.orderCount++] = newOrder;
                        System.out.print("Imported Order " + newOrder.orderId + " (CANCELLED, Payment Failed)\n");
                    } else {
                        log.write(newOrder.orderId, "Payment successful");
                        newOrder.status = "PACKED";
                        log.write(newOrder.orderId, "Status changed to PACKED");
                        dp.orders[dp.orderCount++] = newOrder;
                        InvoiceService.generateInvoice(newOrder, dp);
                        System.out.print("Imported Order " + newOrder.orderId + " (" + newOrder.status + ")\n");
                    }
                }
                importedCount++;
            }
        } catch (Exception e) {
            System.out.print("Error during import: " + e.getMessage() + "\n");
        } finally {
            if (br != null) br.close();
        }
        if (importedCount > 0) {
            System.out.print(importedCount + " orders imported successfully.\n");
        } else {
            System.out.print("No orders were imported.\n");
        }
    }

    /** Simulation mode to generate and process a sample order scenario */
    private void runSimulation() throws Exception {
        System.out.print("Simulation scenarios:\n");
        System.out.print("1. Successful order\n");
        System.out.print("2. Payment failure scenario\n");
        System.out.print("3. Inventory shortage scenario\n");
        System.out.print("4. Random order scenario\n");
        System.out.print("Choose scenario (1-4): ");
        String opt = console.readLine();
        if (opt == null) opt = "";
        opt = opt.trim();
        Order simOrder = new Order();
        simOrder.orderId = dp.generateOrderId();
        simOrder.date = LocalDate.now().toString();
        // Build order based on scenario
        if (opt.equals("2")) {
            // Payment failure scenario: choose items such that total is divisible by 7 to force failure
            Product p = (dp.productCount > 0 ? dp.products[0] : null);
            if (p == null) {
                System.out.print("No products available for simulation.\n");
                return;
            }
            // Choose quantity so that (price * qty) % 7 == 0
            int qty = 1;
            while ((p.price * qty) % 7 != 0) {
                qty++;
            }
            simOrder.addItem(new Item(p.productId, qty));
        } else if (opt.equals("3")) {
            // Inventory shortage scenario: pick a product and set quantity higher than available stock
            Product p = null;
            for (int i = 0; i < dp.productCount; i++) {
                if (dp.products[i] != null && dp.products[i].stock > 0) {
                    p = dp.products[i];
                    break;
                }
            }
            if (p == null) {
                System.out.print("No products available for simulation.\n");
                return;
            }
            int qty = p.stock + 5;  // request more than current stock
            simOrder.addItem(new Item(p.productId, qty));
        } else if (opt.equals("4")) {
            // Random order scenario: pick random items and quantities
            if (dp.productCount == 0) {
                System.out.print("No products available for simulation.\n");
                return;
            }
            // e.g., choose 1-3 random products
            int itemCount = 1 + (int)(Math.random() * 3);
            for (int i = 0; i < itemCount; i++) {
                Product p = dp.products[(int)(Math.random() * dp.productCount)];
                int qty = 1 + (int)(Math.random() * 3);
                simOrder.addItem(new Item(p.productId, qty));
            }
        } else {
            // Default scenario 1: successful order with one item
            if (dp.productCount == 0) {
                System.out.print("No products available for simulation.\n");
                return;
            }
            // Take the first product with stock and quantity 1
            Product p = null;
            for (int i = 0; i < dp.productCount; i++) {
                if (dp.products[i] != null && dp.products[i].stock > 0) {
                    p = dp.products[i];
                    break;
                }
            }
            if (p == null) {
                System.out.print("No products available for simulation.\n");
                return;
            }
            simOrder.addItem(new Item(p.productId, 1));
        }
        // Calculate total for sim order
        int total = 0;
        for (int j = 0; j < simOrder.itemCount; j++) {
            Item it = simOrder.items[j];
            Product prod = dp.findProductById(it.productId);
            total += (prod != null ? prod.price : 0) * it.quantity;
        }
        simOrder.totalAmount = total;
        // Process the simulated order
        boolean ok = InventoryService.validateAndReserve(simOrder, dp, log);
        if (!ok) {
            dp.orders[dp.orderCount++] = simOrder;
            System.out.print("Simulated Order " + simOrder.orderId + " failed (" + simOrder.cancelReason + ").\n");
        } else {
            boolean paid = PaymentService.simulatePayment(simOrder);
            if (!paid) {
                // rollback
                for (int j = 0; j < simOrder.itemCount; j++) {
                    Item it = simOrder.items[j];
                    Product prod = dp.findProductById(it.productId);
                    if (prod != null) prod.stock += it.quantity;
                }
                simOrder.status = "CANCELLED";
                simOrder.cancelReason = "Payment Failed";
                log.write(simOrder.orderId, "Order cancelled – Payment failed");
                dp.orders[dp.orderCount++] = simOrder;
                System.out.print("Simulated Order " + simOrder.orderId + " failed (Payment Failed).\n");
            } else {
                log.write(simOrder.orderId, "Payment successful");
                simOrder.status = "PACKED";
                log.write(simOrder.orderId, "Status changed to PACKED");
                dp.orders[dp.orderCount++] = simOrder;
                InvoiceService.generateInvoice(simOrder, dp);
                System.out.print("Simulated Order " + simOrder.orderId + " processed successfully. Status: PACKED.\n");
            }
        }
    }

    /** Retry processing a cancelled order */
    private void retryCancelledOrder() throws Exception {
        // List cancelled orders
        Order[] cancelledList = new Order[dp.orderCount];
        int cancelledCount = 0;
        for (int i = 0; i < dp.orderCount; i++) {
            Order o = dp.orders[i];
            if (o != null && o.status.equalsIgnoreCase("CANCELLED")) {
                cancelledList[cancelledCount++] = o;
            }
        }
        if (cancelledCount == 0) {
            System.out.print("No cancelled orders to retry.\n");
            return;
        }
        System.out.print("Cancelled Orders:\n");
        for (int i = 0; i < cancelledCount; i++) {
            Order o = cancelledList[i];
            System.out.print(" - " + o.orderId + " (" + o.cancelReason + ")\n");
        }
        System.out.print("Enter Order ID to retry: ");
        String cid = console.readLine();
        if (cid == null) cid = "";
        cid = cid.trim().toUpperCase();
        if (!cid.startsWith("O")) {
            cid = "O" + cid;
        }
        // Find the cancelled order
        Order target = null;
        for (int i = 0; i < cancelledCount; i++) {
            if (cancelledList[i].orderId.equalsIgnoreCase(cid)) {
                target = cancelledList[i];
                break;
            }
        }
        if (target == null) {
            System.out.print("Order " + cid + " is not in cancelled list.\n");
            return;
        }
        // Make a copy of the cancelled order (to retry as a new order or same ID?)
        // Here, we will attempt to reprocess the same Order object.
        target.status = "PENDING";
        target.cancelReason = "";
        // Try processing again
        boolean processed = InventoryService.validateAndReserve(target, dp, log);
        if (!processed) {
            // If still fails, mark back to cancelled
            target.status = "CANCELLED";
            // (cancelReason already set by validateAndReserve)
            System.out.print("Retry failed. Order remains CANCELLED (" + target.cancelReason + ").\n");
        } else {
            boolean paid = PaymentService.simulatePayment(target);
            if (!paid) {
                // rollback
                for (int j = 0; j < target.itemCount; j++) {
                    Item it = target.items[j];
                    Product prod = dp.findProductById(it.productId);
                    if (prod != null) prod.stock += it.quantity;
                }
                target.status = "CANCELLED";
                target.cancelReason = "Payment Failed";
                log.write(target.orderId, "Order cancelled – Payment failed");
                System.out.print("Retry failed. Order remains CANCELLED (" + target.cancelReason + ").\n");
            } else {
                log.write(target.orderId, "Payment successful");
                target.status = "PACKED";
                log.write(target.orderId, "Status changed to PACKED");
                InvoiceService.generateInvoice(target, dp);
                System.out.print("Order " + target.orderId + " reprocessed successfully. Status: PACKED.\n");
            }
        }
    }

    /** Archive all delivered orders to archive_orders.txt and remove them from active list */
    private void archiveDeliveredOrders() throws Exception {
        System.out.print("Enter threshold (days) for archiving delivered orders (e.g., 0 for all): ");
        String daysStr = console.readLine();
        if (daysStr == null) daysStr = "";
        daysStr = daysStr.trim();
        int N = DataPersistence.toInt(daysStr);
        if (N < 0) N = 0;
        // Current date as day count
        int todayCount = dateToDayCount(LocalDate.now().toString());
        FileWriter fw = new FileWriter(dp.path("archive_orders.txt"), true);
        int archivedCount = 0;
        // Use a new array to hold remaining orders
        Order[] remaining = new Order[dp.orders.length];
        int newCount = 0;
        for (int i = 0; i < dp.orderCount; i++) {
            Order o = dp.orders[i];
            if (o == null) continue;
            if (o.status.equalsIgnoreCase("DELIVERED")) {
                // Check how long ago delivered
                int orderDay = dateToDayCount(o.date);
                int age = todayCount - orderDay;
                if (age >= N) {
                    // Archive this order
                    // Write to archive file in same format as orders.txt
                    String itemList = "";
                    for (int j = 0; j < o.itemCount; j++) {
                        Item it = o.items[j];
                        if (it == null) continue;
                        itemList += it.productId + "x" + it.quantity;
                        if (j < o.itemCount - 1) itemList += ", ";
                    }
                    fw.write(o.orderId + "|" + o.date + "|" + o.status + "|" + itemList + "|" + o.totalAmount + "|" + (o.cancelReason == null ? "" : o.cancelReason) + "\n");
                    log.write(o.orderId, "Archived (Delivered " + age + " days ago)");
                    archivedCount++;
                    // do not add to remaining list (i.e., remove from active)
                    continue;
                }
            }
            // keep orders not archived
            remaining[newCount++] = o;
        }
        fw.close();
        dp.orders = remaining;
        dp.orderCount = newCount;
        if (archivedCount > 0) {
            System.out.print(archivedCount + " delivered orders archived (older than " + N + " days). See archive_orders.txt.\n");
        } else {
            System.out.print("No delivered orders were archived.\n");
        }
    }

    /** Generate a receipt text file for a delivered order */
    private void generateReceipt() throws Exception {
        System.out.print("Enter Order ID for receipt: ");
        String rid = console.readLine();
        if (rid == null) rid = "";
        rid = rid.trim().toUpperCase();
        if (!rid.startsWith("O")) {
            rid = "O" + rid;
        }
        // Find the order
        Order order = null;
        for (int i = 0; i < dp.orderCount; i++) {
            Order o = dp.orders[i];
            if (o != null && o.orderId.equalsIgnoreCase(rid)) {
                order = o;
                break;
            }
        }
        if (order == null) {
            System.out.print("Order " + rid + " not found.\n");
            return;
        }
        if (!order.status.equals("DELIVERED")) {
            System.out.print("Receipt can only be generated for delivered orders.\n");
            return;
        }
        // Prepare receipt content and write to separate file
        String filename = "receipt_" + order.orderId + ".txt";
        FileWriter fw = new FileWriter(dp.path(filename), false);
        fw.write("Receipt for Order " + order.orderId + "\n");
        fw.write("Date: " + order.date + "\n");
        fw.write("Status: " + order.status + "\n");
        fw.write("--------------------------------------\n");
        fw.write("Items:\n");
        for (int j = 0; j < order.itemCount; j++) {
            Item it = order.items[j];
            if (it == null) continue;
            Product p = dp.findProductById(it.productId);
            String itemName = (p != null ? p.name : it.productId);
            int priceEach = (p != null ? p.price : 0);
            fw.write("- " + itemName + " (x" + it.quantity + " @ BDT " + priceEach + " each)\n");
        }
        fw.write("--------------------------------------\n");
        fw.write("Total Paid: BDT " + order.totalAmount + "\n");
        fw.write("Thank you for your purchase!\n");
        fw.close();
        System.out.print("Receipt generated: " + filename + "\n");
    }

    /** Helper: convert date string "YYYY-MM-DD" to an approximate day count for difference calculation */
    private int dateToDayCount(String dateStr) {
        if (dateStr == null || dateStr.length() == 0) return 0;
        String[] parts = dateStr.split("-");
        if (parts.length < 3) return 0;
        int y = DataPersistence.toInt(parts[0]);
        int m = DataPersistence.toInt(parts[1]);
        int d = DataPersistence.toInt(parts[2]);
        // approximate days (treat each month as 30 days, year as 360 days for simplicity)
        return y * 360 + m * 30 + d;
    }
}
