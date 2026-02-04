import java.io.BufferedReader;

/** Workflow.java – contributed by Maliha (Integrates order processing, inventory, payment, shipping, reporting, etc.) */
public class Workflow {
    private DataPersistence dp;
    private Log log;
    // ANSI color codes for console output (low stock alert)
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_RESET = "\u001B[0m";

    public Workflow(DataPersistence dp, Log log) {
        this.dp = dp;
        this.log = log;
    }

    /** Wrapper for Admin authentication */
    public boolean adminLogin(BufferedReader console) throws Exception {
        return Admin.authenticate(dp, console);
    }

    /** Admin Dashboard menu loop (CLI interface for all features) */
    public void adminDashboard(BufferedReader console) throws Exception {
        System.out.print("\n===== Admin Dashboard =====\n");
        while (true) {
            // Display menu options
            System.out.print("\nMenu:\n");
            System.out.print("1. Search/Filter Orders\n");
            System.out.print("2. Update Order Status\n");
            System.out.print("3. View Order Logs\n");
            System.out.print("4. Generate Report\n");
            System.out.print("5. Reorder Previous Order\n");
            System.out.print("6. Advanced Product Filter\n");
            System.out.print("7. Low Stock Alerts\n");
            System.out.print("8. Export Stock Report\n");
            System.out.print("9. Bulk Import Orders\n");
            System.out.print("10. Simulation Mode\n");
            System.out.print("11. Retry Failed Order\n");
            System.out.print("12. Archive Delivered Orders\n");
            System.out.print("13. Change Admin Password\n");
            System.out.print("14. Clear Logs\n");
            System.out.print("15. Generate Receipt\n");
            System.out.print("0. Exit\n");
            System.out.print("Choose an option: ");
            String choice = console.readLine();
            if (choice == null) choice = "";
            choice = choice.trim();
            System.out.print("\n");
            switch (choice) {
                case "1":
                    // Search or filter orders by ID or status
                    handleOrderSearch(console);  // (Saika)
                    break;
                case "2":
                    // Manual status update for an order
                    handleStatusUpdate(console);  // (Maliha for shipping flow)
                    break;
                case "3":
                    // View workflow logs for a specific order
                    System.out.print("Enter Order ID to view logs: ");
                    String logId = console.readLine();
                    if (logId != null) {
                        logId = logId.trim();
                        if (!logId.equals("")) {
                            log.viewLogsByOrder(logId);
                        }
                    }
                    break;
                case "4":
                    // Generate summary report and save to report.txt
                    generateReport();
                    break;
                case "5":
                    // Reorder a previous order (copy items to new order)
                    handleReorder(console);  // (Saika)
                    break;
                case "6":
                    // Advanced product filtering and sorting
                    handleAdvancedFilter(console);  // (Saika)
                    break;
                case "7":
                    // List low-stock items (stock < 5) with alert coloring
                    showLowStockAlerts();
                    break;
                case "8":
                    // Export current stock levels to stock_report.txt
                    exportStockReport();
                    break;
                case "9":
                    // Bulk import orders from orders_import.txt
                    importOrdersFromFile();
                    break;
                case "10":
                    // Simulation mode for generating test orders
                    runSimulation(console);
                    break;
                case "11":
                    // Retry processing a failed (cancelled) order
                    retryCancelledOrder(console);
                    break;
                case "12":
                    // Archive delivered orders older than N days
                    archiveOldOrders(console);
                    break;
                case "13":
                    // Change admin password
                    changeAdminPassword(console);
                    break;
                case "14":
                    // Clear all logs (audit trail)
                    clearLogs(console);
                    break;
                case "15":
                    // Generate a receipt for a delivered order
                    generateReceipt(console);
                    break;
                case "0":
                    // Exit dashboard loop
                    System.out.print("Exiting Admin Dashboard...\n");
                    return;
                default:
                    System.out.print("Invalid option. Please try again.\n");
                    break;
            }
            System.out.print("\n--------------------------------\n");
        }
    }

    /** Handle searching orders by ID or filtering by status, then viewing details (Feature 1 & view details) */
    private void handleOrderSearch(BufferedReader console) throws Exception {
        System.out.print("Enter Order ID or Status to search: ");
        String query = console.readLine();
        if (query == null) query = "";
        query = query.trim();
        if (query.equalsIgnoreCase("")) {
            System.out.print("Search query cannot be empty.\n");
            return;
        }
        boolean isStatusQuery = false;
        String qUpper = query.toUpperCase();
        // Determine if input matches a known status
        String[] validStatuses = {"PENDING", "PACKED", "SHIPPED", "OUT_FOR_DELIVERY", "DELIVERED", "CANCELLED"};
        for (String st : validStatuses) {
            if (st.equals(qUpper)) {
                isStatusQuery = true;
                break;
            }
        }
        if (isStatusQuery) {
            // Filter orders by status
            String status = qUpper;
            Order[] results = new Order[dp.orderCount];
            int count = 0;
            for (int i = 0; i < dp.orderCount; i++) {
                Order o = dp.orders[i];
                if (o != null && o.status.equalsIgnoreCase(status)) {
                    results[count++] = o;
                }
            }
            if (count == 0) {
                System.out.print("No orders found with status \"" + status + "\".\n");
            } else {
                System.out.print("Orders with status " + status + ":\n");
                for (int i = 0; i < count; i++) {
                    Order o = results[i];
                    System.out.print("- " + o.orderId + " | Status: " + o.status + " | Total: BDT " + o.totalAmount);
                    if (o.cancelReason != null && !o.cancelReason.equals("")) {
                        System.out.print(" | CancelReason: " + o.cancelReason);
                    }
                    System.out.print("\n");
                }
                // Prompt to view details of one of these orders
                System.out.print("Enter Order ID to view details (or press Enter to skip): ");
                String selId = console.readLine();
                if (selId != null) {
                    selId = selId.trim();
                    if (!selId.equals("")) {
                        viewOrderDetails(selId);
                    }
                }
            }
        } else {
            // Treat query as Order ID
            String orderId = query.toUpperCase();
            // If user omitted prefix "O", add it (assuming numeric given)
            if (!orderId.startsWith("O")) {
                orderId = "O" + orderId;
            }
            viewOrderDetails(orderId);
        }
    }

    /** Display full details of a specific order by ID (items, total, status, tracking, cancellation reason) */
    private void viewOrderDetails(String orderId) {
        Order order = null;
        for (int i = 0; i < dp.orderCount; i++) {
            if (dp.orders[i] != null && dp.orders[i].orderId.equalsIgnoreCase(orderId)) {
                order = dp.orders[i];
                break;
            }
        }
        if (order == null) {
            System.out.print("Order " + orderId + " not found.\n");
            return;
        }
        // Print detailed information
        System.out.print("Order ID: " + order.orderId + "\n");
        System.out.print("Date: " + order.date + "\n");
        System.out.print("Status: " + order.status + "\n");
        if (order.status.equalsIgnoreCase("CANCELLED")) {
            System.out.print("Cancellation Reason: " + (order.cancelReason.isEmpty() ? "(None)" : order.cancelReason) + "\n");
        }
        if (order.trackingId != null && !order.trackingId.equals("")) {
            System.out.print("Tracking ID: " + order.trackingId + "\n");
        } else {
            if (order.status.equalsIgnoreCase("SHIPPED") || order.status.equalsIgnoreCase("OUT_FOR_DELIVERY") || order.status.equalsIgnoreCase("DELIVERED")) {
                System.out.print("Tracking ID: " + "(Unavailable)\n");
            }
        }
        System.out.print("Items:\n");
        for (int j = 0; j < order.itemCount; j++) {
            Item it = order.items[j];
            if (it == null) continue;
            Product p = dp.findProductById(it.productId);
            String itemName = (p != null ? p.name : it.productId);
            int pricePerUnit = (p != null ? p.price : 0);
            System.out.print("  - " + itemName + " (ID:" + it.productId + "), Qty: " + it.quantity);
            if (pricePerUnit > 0) {
                System.out.print(", Price: BDT " + pricePerUnit + " each");
            }
            System.out.print("\n");
        }
        System.out.print("Total Amount: BDT " + order.totalAmount + "\n");
    }

    /** Handle manual status update (Feature 2: Manual Status Control) */
    private void handleStatusUpdate(BufferedReader console) throws Exception {
        System.out.print("Enter Order ID to update status: ");
        String id = console.readLine();
        if (id == null) id = "";
        id = id.trim();
        if (id.equals("")) {
            System.out.print("Order ID cannot be empty.\n");
            return;
        }
        // Normalize ID
        if (!id.toUpperCase().startsWith("O")) {
            id = "O" + id;
        } else {
            id = id.toUpperCase();
        }
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
        // Check current status
        String currentStatus = order.status;
        if (currentStatus.equals("DELIVERED") || currentStatus.equals("CANCELLED")) {
            System.out.print("Order " + id + " is " + currentStatus + "; status cannot be changed.\n");
            return;
        }
        // Determine next valid status in sequence
        String nextStatus = null;
        if (currentStatus.equals("PENDING")) {
            // If an order is pending (unprocessed), run inventory & payment processing first
            boolean processed = processPendingOrder(order);
            if (!processed) {
                // If processing failed, order status is now CANCELLED with reason (logged in processPendingOrder)
                System.out.print("Order processing failed. Status updated to CANCELLED (" + order.cancelReason + ").\n");
                return;
            }
            // If processing succeeded, status is now PACKED (set inside processPendingOrder)
            currentStatus = order.status;
        }
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
            // Assign a tracking ID when order is shipped
            order.trackingId = "TRK" + order.orderId.substring(1);
        }
        if (nextStatus.equals("DELIVERED")) {
            // When delivered, we keep data in system until archived by separate feature
        }
        log.write(order.orderId, "Status changed to " + nextStatus);
        System.out.print("Order " + order.orderId + " status updated to " + nextStatus + ".\n");
    }

    /** Process a PENDING order through inventory and payment steps (used in manual status control) */
    private boolean processPendingOrder(Order order) {
        if (order == null || !order.status.equals("PENDING")) return false;
        // Inventory verification and reservation (Adiba's logic)
        for (int i = 0; i < order.itemCount; i++) {
            Item it = order.items[i];
            if (it == null) continue;
            Product prod = dp.findProductById(it.productId);
            if (prod == null) {
                order.status = "CANCELLED";
                order.cancelReason = "Invalid product " + it.productId;
                log.write(order.orderId, "Order cancelled – " + order.cancelReason);
                return false;
            }
            if (prod.stock < it.quantity) {
                order.status = "CANCELLED";
                order.cancelReason = "Inventory Shortage";
                log.write(order.orderId, "Order cancelled – " + order.cancelReason);
                return false;
            }
        }
        // Reserve stock (deduct from inventory)
        for (int i = 0; i < order.itemCount; i++) {
            Item it = order.items[i];
            Product prod = dp.findProductById(it.productId);
            if (prod != null) {
                prod.stock -= it.quantity;
            }
        }
        log.write(order.orderId, "Inventory OK – stock reserved");
        // Invoice & Total calculation (Maliha)
        int total = 0;
        for (int i = 0; i < order.itemCount; i++) {
            Item it = order.items[i];
            Product prod = dp.findProductById(it.productId);
            int price = (prod != null ? prod.price : 0);
            total += price * it.quantity;
        }
        order.totalAmount = total;
        // Payment simulation (Maliha)
        boolean paymentSuccess = simulatePayment(order);
        if (!paymentSuccess) {
            // Rollback inventory if payment failed (return reserved stock)
            for (int i = 0; i < order.itemCount; i++) {
                Item it = order.items[i];
                Product prod = dp.findProductById(it.productId);
                if (prod != null) {
                    prod.stock += it.quantity;
                }
            }
            order.status = "CANCELLED";
            order.cancelReason = "Payment Failed";
            log.write(order.orderId, "Order cancelled – Payment failed");
            return false;
        }
        // If payment succeeded:
        log.write(order.orderId, "Payment successful");
        order.status = "PACKED";
        log.write(order.orderId, "Status changed to PACKED");
        return true;
    }

    /** Simulate a payment process for an order (success or failure) */
    private boolean simulatePayment(Order order) {
        // For simplicity, simulate payment success/failure by random or heuristic
        // Here, fail if total amount is divisible by 7 (just an arbitrary condition to simulate some failures)
        if (order.totalAmount % 7 == 0) {
            return false;
        }
        return true;
    }

    /** Generate and output the reporting system summary (Feature 4: Reporting System) */
    private void generateReport() throws Exception {
        int totalOrders = dp.orderCount;
        int completedCount = 0;
        int cancelledCount = 0;
        int revenueSum = 0;
        // Tally cancellation reasons
        String[] reasons = new String[totalOrders];
        int[] reasonCounts = new int[totalOrders];
        int reasonTypes = 0;
        for (int i = 0; i < dp.orderCount; i++) {
            Order o = dp.orders[i];
            if (o == null) continue;
            if (o.status.equalsIgnoreCase("DELIVERED")) {
                completedCount++;
                revenueSum += o.totalAmount;
            }
            if (o.status.equalsIgnoreCase("CANCELLED")) {
                cancelledCount++;
                String reason = (o.cancelReason == null || o.cancelReason.equals("") ? "Unknown" : o.cancelReason);
                // Count this reason
                boolean found = false;
                for (int r = 0; r < reasonTypes; r++) {
                    if (reasons[r].equalsIgnoreCase(reason)) {
                        reasonCounts[r]++;
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    reasons[reasonTypes] = reason;
                    reasonCounts[reasonTypes] = 1;
                    reasonTypes++;
                }
            }
        }
        // Determine top 3 cancellation reasons
        // Sort the reasons by count (simple selection sort)
        for (int i = 0; i < reasonTypes - 1; i++) {
            int maxIndex = i;
            for (int j = i + 1; j < reasonTypes; j++) {
                if (reasonCounts[j] > reasonCounts[maxIndex]) {
                    maxIndex = j;
                }
            }
            // swap
            String tempReason = reasons[i];
            reasons[i] = reasons[maxIndex];
            reasons[maxIndex] = tempReason;
            int tempCount = reasonCounts[i];
            reasonCounts[i] = reasonCounts[maxIndex];
            reasonCounts[maxIndex] = tempCount;
        }
        // Prepare report content
        StringBuilder report = new StringBuilder();
        report.append("Total Orders: ").append(totalOrders).append("\n");
        report.append("Completed Orders: ").append(completedCount).append("\n");
        report.append("Cancelled Orders: ").append(cancelledCount).append("\n");
        report.append("Total Revenue: BDT ").append(revenueSum).append("\n");
        report.append("Top 3 Cancellation Reasons:\n");
        for (int k = 0; k < reasonTypes && k < 3; k++) {
            report.append((k + 1) + ". " + reasons[k] + " – " + reasonCounts[k] + "\n");
        }
        // Write to report.txt
        FileWriter fw = new FileWriter(dp.path("report.txt"), false);
        fw.write(report.toString());
        fw.close();
        // Also output to console
        System.out.print("=== Report Summary ===\n");
        System.out.print(report.toString());
        System.out.print("(Report saved to report.txt)\n");
    }

    /** Reorder a previous order: copy its items into a new order and process it (Feature 7) */
    private void handleReorder(BufferedReader console) throws Exception {
        System.out.print("Enter Order ID to reorder: ");
        String oldId = console.readLine();
        if (oldId == null) oldId = "";
        oldId = oldId.trim().toUpperCase();
        if (oldId.equals("")) {
            System.out.print("Order ID cannot be empty.\n");
            return;
        }
        if (!oldId.startsWith("O")) {
            oldId = "O" + oldId;
        }
        Order original = null;
        for (int i = 0; i < dp.orderCount; i++) {
            if (dp.orders[i] != null && dp.orders[i].orderId.equalsIgnoreCase(oldId)) {
                original = dp.orders[i];
                break;
            }
        }
        if (original == null) {
            System.out.print("Order " + oldId + " not found.\n");
            return;
        }
        // Create a new order with the same items
        Order newOrder = new Order();
        newOrder.orderId = dp.generateOrderId();
        newOrder.date = currentDateString();
        // Copy items
        for (int j = 0; j < original.itemCount; j++) {
            Item it = original.items[j];
            if (it == null) continue;
            newOrder.addItem(new Item(it.productId, it.quantity));
        }
        // Calculate total
        int total = 0;
        for (int j = 0; j < newOrder.itemCount; j++) {
            Item it = newOrder.items[j];
            Product p = dp.findProductById(it.productId);
            if (p != null) {
                total += p.price * it.quantity;
            }
        }
        newOrder.totalAmount = total;
        // Process inventory & payment for reorder
        if (!processPendingOrder(newOrder)) {
            // If reorder fails, add it to orders list as cancelled (for record)
            dp.orders[dp.orderCount++] = newOrder;
            System.out.print("Reorder created as " + newOrder.orderId + " but failed (" + newOrder.cancelReason + ").\n");
        } else {
            // If success, status is PACKED and reserved; add to list
            dp.orders[dp.orderCount++] = newOrder;
            System.out.print("Reorder successful! New Order ID: " + newOrder.orderId + " (Status: " + newOrder.status + ").\n");
            log.write(newOrder.orderId, "Reordered from " + oldId);
        }
    }

    /** Advanced product filter by brand/category with optional sorting (Feature 8) */
    private void handleAdvancedFilter(BufferedReader console) throws Exception {
        System.out.print("Filter by Brand or Category? (B/C): ");
        String choice = console.readLine();
        if (choice == null) choice = "";
        choice = choice.trim().toUpperCase();
        if (!choice.equals("B") && !choice.equals("C")) {
            System.out.print("Invalid choice. Enter 'B' for Brand or 'C' for Category.\n");
            return;
        }
        System.out.print("Enter " + (choice.equals("B") ? "Brand" : "Category") + " name: ");
        String keyword = console.readLine();
        if (keyword == null) keyword = "";
        keyword = keyword.trim();
        if (keyword.equals("")) {
            System.out.print("Input cannot be empty.\n");
            return;
        }
        // Filter products matching brand or category (case-insensitive contains)
        Product[] filtered = new Product[dp.productCount];
        int count = 0;
        for (int i = 0; i < dp.productCount; i++) {
            Product p = dp.products[i];
            if (p == null) continue;
            if (choice.equals("B")) {
                if (p.brand.equalsIgnoreCase(keyword)) {
                    filtered[count++] = p;
                }
            } else {
                if (p.category.equalsIgnoreCase(keyword)) {
                    filtered[count++] = p;
                }
            }
        }
        if (count == 0) {
            System.out.print("No products found for " + (choice.equals("B") ? "brand" : "category") + " \"" + keyword + "\".\n");
            return;
        }
        // Ask for sorting preference
        System.out.print("Sort by Price or Stock? (P/S, Enter for no sort): ");
        String sortChoice = console.readLine();
        if (sortChoice == null) sortChoice = "";
        sortChoice = sortChoice.trim().toUpperCase();
        if (sortChoice.equals("P") || sortChoice.equals("S")) {
            // Perform simple selection sort on filtered array
            for (int i = 0; i < count - 1; i++) {
                int minIdx = i;
                for (int j = i + 1; j < count; j++) {
                    if (sortChoice.equals("P")) {
                        if (filtered[j].price < filtered[minIdx].price) {
                            minIdx = j;
                        }
                    } else {
                        if (filtered[j].stock < filtered[minIdx].stock) {
                            minIdx = j;
                        }
                    }
                }
                // swap
                Product temp = filtered[i];
                filtered[i] = filtered[minIdx];
                filtered[minIdx] = temp;
            }
        }
        // Display filtered (and sorted) results
        System.out.print("Filtered Products (" + (choice.equals("B") ? "Brand" : "Category") + ": " + keyword + "):\n");
        for (int i = 0; i < count; i++) {
            Product p = filtered[i];
            if (p == null) continue;
            String stockStr = String.valueOf(p.stock);
            // Highlight low stock <5 in red
            if (p.stock < 5) {
                stockStr = ANSI_RED + stockStr + ANSI_RESET;
            }
            System.out.print(p.productId + " | " + p.name + " | BDT " + p.price + " | Stock: " + stockStr + "\n");
        }
    }

    /** Display low stock items (stock < 5) with color-coded alert (Feature 9) */
    private void showLowStockAlerts() {
        boolean anyLow = false;
        System.out.print("Low Stock Items (stock < 5):\n");
        for (int i = 0; i < dp.productCount; i++) {
            Product p = dp.products[i];
            if (p == null) continue;
            if (p.stock < 5) {
                anyLow = true;
                System.out.print(ANSI_RED + p.productId + " | " + p.name + " | Stock: " + p.stock + ANSI_RESET + "\n");
            }
        }
        if (!anyLow) {
            System.out.print("None (all products have sufficient stock).\n");
        }
    }

    /** Export current stock of all products to stock_report.txt (Feature 10) */
    private void exportStockReport() throws Exception {
        FileWriter fw = new FileWriter(dp.path("stock_report.txt"), false);
        fw.write("ProductID | Name | Price | Stock\n");
        for (int i = 0; i < dp.productCount; i++) {
            Product p = dp.products[i];
            if (p == null) continue;
            fw.write(p.productId + " | " + p.name + " | " + p.price + " | " + p.stock + "\n");
        }
        fw.close();
        System.out.print("Stock report generated in stock_report.txt\n");
    }

    /** Bulk import orders from orders_import.txt (Feature 11) */
    private void importOrdersFromFile() throws Exception {
        BufferedReader br = null;
        int importedCount = 0;
        try {
            br = new BufferedReader(new FileReader(dp.path("orders_import.txt")));
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.length() == 0) continue;
                // Assuming import format: Date|ItemList (items separated by commas like in orders file)
                // We will treat imported orders as new pending orders that we process immediately
                String[] parts = line.split("\\|");
                if (parts.length < 2) {
                    System.out.print("Skipping invalid import line: " + line + "\n");
                    continue;  // Test scenario: missing fields
                }
                String date = parts[0].trim();
                String itemsPart = parts[1].trim();
                Order newOrder = new Order();
                newOrder.orderId = dp.generateOrderId();
                newOrder.date = (date.equals("") ? currentDateString() : date);
                // parse items
                dp.parseItemsIntoOrder(newOrder, itemsPart);
                // If no items or invalid items, skip
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
                // Process the order (inventory + payment)
                processPendingOrder(newOrder);
                dp.orders[dp.orderCount++] = newOrder;
                importedCount++;
                System.out.print("Imported Order " + newOrder.orderId + " (" + newOrder.status +
                        (newOrder.status.equals("CANCELLED") ? ", " + newOrder.cancelReason : "") + ")\n");
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

    /** Simulation mode to generate a random or scenario-specific order (Feature 12) */
    private void runSimulation(BufferedReader console) throws Exception {
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
        simOrder.date = currentDateString();
        // Build order based on scenario
        if (opt.equals("2")) {
            // Payment failure: choose items such that total triggers payment fail condition (multiple of 7)
            // We ensure total becomes a multiple of 7 for failure
            Product p = dp.products[0];
            if (p == null) {
                System.out.print("No products available for simulation.\n");
                return;
            }
            // Choose quantity to make total %7==0
            int qty = 1;
            while ((p.price * qty) % 7 != 0) {
                qty++;
            }
            simOrder.addItem(new Item(p.productId, qty));
        } else if (opt.equals("3")) {
            // Inventory shortage: pick a product and set quantity higher than available stock
            Product p = null;
            for (int i = 0; i < dp.productCount; i++) {
                if (dp.products[i] != null && dp.products[i].stock > 0) {
                    p = dp.products[i];
                    break;
                }
            }
            if (p == null) {
                System.out.print("No products in stock to simulate shortage.\n");
                return;
            }
            simOrder.addItem(new Item(p.productId, p.stock + 5));  // 5 more than in stock
        } else if (opt.equals("1") || opt.equals("4")) {
            // Successful or random: pick 1-2 random items within stock
            if (dp.productCount == 0) {
                System.out.print("No products available to simulate order.\n");
                return;
            }
            // add one random product
            Product p1 = dp.products[0];
            if (p1 != null) {
                int qty1 = (p1.stock > 0 ? 1 : 0);
                if (qty1 == 0) qty1 = 1;  // even if stock 0, try anyway (to test fail?)
                simOrder.addItem(new Item(p1.productId, qty1));
            }
            // possibly add another item if we have more products (for random scenario only)
            if (opt.equals("4") && dp.productCount > 1) {
                Product p2 = dp.products[1];
                if (p2 != null) {
                    int qty2 = (p2.stock > 0 ? 1 : 1);
                    simOrder.addItem(new Item(p2.productId, qty2));
                }
            }
        } else {
            System.out.print("Invalid simulation choice.\n");
            return;
        }
        // Calculate total
        int total = 0;
        for (int j = 0; j < simOrder.itemCount; j++) {
            Item it = simOrder.items[j];
            Product pr = dp.findProductById(it.productId);
            total += (pr != null ? pr.price : 0) * it.quantity;
        }
        simOrder.totalAmount = total;
        // Process simulation order
        processPendingOrder(simOrder);
        dp.orders[dp.orderCount++] = simOrder;
        // Output result summary
        System.out.print("Simulated Order " + simOrder.orderId + " created. Status: " + simOrder.status);
        if (simOrder.status.equals("CANCELLED")) {
            System.out.print(" (" + simOrder.cancelReason + ")");
        }
        System.out.print(".\n");
    }

    /** Retry processing a cancelled order (Feature 13) */
    private void retryCancelledOrder(BufferedReader console) throws Exception {
        // List cancelled orders
        Order[] cancelledList = new Order[dp.orderCount];
        int count = 0;
        for (int i = 0; i < dp.orderCount; i++) {
            Order o = dp.orders[i];
            if (o != null && o.status.equals("CANCELLED")) {
                cancelledList[count++] = o;
            }
        }
        if (count == 0) {
            System.out.print("No cancelled orders to retry.\n");
            return;
        }
        System.out.print("Cancelled Orders:\n");
        for (int i = 0; i < count; i++) {
            Order o = cancelledList[i];
            System.out.print("- " + o.orderId + " (" + o.cancelReason + ")\n");
        }
        System.out.print("Enter Order ID to retry: ");
        String cid = console.readLine();
        if (cid == null) cid = "";
        cid = cid.trim().toUpperCase();
        if (!cid.startsWith("O")) {
            cid = "O" + cid;
        }
        Order target = null;
        for (int i = 0; i < count; i++) {
            if (cancelledList[i] != null && cancelledList[i].orderId.equalsIgnoreCase(cid)) {
                target = cancelledList[i];
                break;
            }
        }
        if (target == null) {
            System.out.print("Order " + cid + " not found in cancelled list.\n");
            return;
        }
        // Attempt to reprocess the order (inventory & payment) using same Order object
        target.cancelReason = ""; // clear previous reason
        target.status = "PENDING";
        boolean processed = processPendingOrder(target);
        if (!processed) {
            System.out.print("Retry failed. Order remains CANCELLED (" + target.cancelReason + ").\n");
        } else {
            System.out.print("Order " + target.orderId + " reprocessed successfully (Status: " + target.status + ").\n");
        }
    }

    /** Archive delivered orders older than N days to archive_orders.txt (Feature 14) */
    private void archiveOldOrders(BufferedReader console) throws Exception {
        System.out.print("Enter threshold (N days) for archiving delivered orders: ");
        String daysStr = console.readLine();
        if (daysStr == null) daysStr = "0";
        daysStr = daysStr.trim();
        int N = DataPersistence.toInt(daysStr);
        if (N <= 0) {
            System.out.print("Invalid number of days.\n");
            return;
        }
        // Compute current date in "YYYY-MM-DD" (assuming date format stored)
        String todayStr = currentDateString();
        // Convert date strings to a simplified day count (approximate, assuming 30 days per month)
        int todayCount = dateToDayCount(todayStr);
        FileWriter fw = new FileWriter(dp.path("archive_orders.txt"), true);
        int archivedCount = 0;
        // Use a new array to hold remaining orders after archiving
        Order[] remaining = new Order[dp.orders.length];
        int remCount = 0;
        for (int i = 0; i < dp.orderCount; i++) {
            Order o = dp.orders[i];
            if (o == null) continue;
            if (o.status.equals("DELIVERED")) {
                // Calculate age in days
                int orderDayCount = dateToDayCount(o.date);
                int age = todayCount - orderDayCount;
                if (age > N) {
                    // Archive this order
                    fw.write(o.toRecord() + "\n");
                    archivedCount++;
                    log.write(o.orderId, "Archived (Delivered " + age + " days ago)");
                    continue;  // skip adding to remaining list
                }
            }
            // keep order if not archived
            remaining[remCount++] = o;
        }
        fw.close();
        // Replace orders list with remaining orders and update count
        dp.orders = remaining;
        dp.orderCount = remCount;
        System.out.print("Archived " + archivedCount + " delivered orders older than " + N + " days (see archive_orders.txt).\n");
    }

    /** Change the admin password (Feature 15) */
    private void changeAdminPassword(BufferedReader console) throws Exception {
        System.out.print("Enter current password: ");
        String currentPass = console.readLine();
        if (currentPass == null) currentPass = "";
        currentPass = currentPass.trim();
        Admin admin = dp.admin;
        if (!admin.passHash.equals(Admin.hashPassword(currentPass))) {
            System.out.print("Current password is incorrect.\n");
            return;
        }
        System.out.print("Enter new password: ");
        String newPass1 = console.readLine();
        if (newPass1 == null) newPass1 = "";
        newPass1 = newPass1.trim();
        System.out.print("Confirm new password: ");
        String newPass2 = console.readLine();
        if (newPass2 == null) newPass2 = "";
        newPass2 = newPass2.trim();
        if (!newPass1.equals(newPass2) || newPass1.equals("")) {
            System.out.print("Password mismatch or empty. Password not changed.\n");
            return;
        }
        // Update password hash and save to file
        admin.passHash = Admin.hashPassword(newPass1);
        dp.saveAll();
        log.write("ADMIN", "Password changed");
        System.out.print("Admin password changed successfully.\n");
    }

    /** Clear all logs (Feature 16) */
    private void clearLogs(BufferedReader console) throws Exception {
        System.out.print("Are you sure you want to clear all logs? (Y/N): ");
        String confirm = console.readLine();
        if (confirm == null) confirm = "";
        confirm = confirm.trim().toUpperCase();
        if (!confirm.equals("Y")) {
            System.out.print("Log clearing aborted.\n");
            return;
        }
        // Overwrite logs.txt with nothing
        FileWriter fw = new FileWriter(dp.path("logs.txt"), false);
        fw.write("");
        fw.close();
        System.out.print("All logs have been cleared.\n");
    }

    /** Generate a receipt text file for a delivered order (Feature 17) */
    private void generateReceipt(BufferedReader console) throws Exception {
        System.out.print("Enter Order ID for receipt: ");
        String rid = console.readLine();
        if (rid == null) rid = "";
        rid = rid.trim().toUpperCase();
        if (!rid.startsWith("O")) {
            rid = "O" + rid;
        }
        Order order = null;
        for (int i = 0; i < dp.orderCount; i++) {
            if (dp.orders[i] != null && dp.orders[i].orderId.equalsIgnoreCase(rid)) {
                order = dp.orders[i];
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
        // Prepare receipt content
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

    /** Helper: get current date as YYYY-MM-DD (for simplicity, return a fixed date or sample date) */
    private String currentDateString() {
        // In a real scenario, we'd get today's date. Here we return the date from the system (approximation).
        java.time.LocalDate today = java.time.LocalDate.now();
        return today.toString();
    }

    /** Helper: convert a YYYY-MM-DD date string to an approximate day count */
    private int dateToDayCount(String dateStr) {
        if (dateStr == null || dateStr.length() == 0) return 0;
        String[] parts = dateStr.split("-");
        if (parts.length < 3) return 0;
        int y = DataPersistence.toInt(parts[0]);
        int m = DataPersistence.toInt(parts[1]);
        int d = DataPersistence.toInt(parts[2]);
        // approximate conversion: year*360 + month*30 + day
        return y * 360 + m * 30 + d;
    }
}



