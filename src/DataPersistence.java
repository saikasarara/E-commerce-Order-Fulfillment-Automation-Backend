import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;

public class DataPersistence {
    private String baseDir;
    // Data stores in memory
    public Product[] products = new Product[100];
    public int productCount = 0;
    public Order[] orders = new Order[100];
    public int orderCount = 0;
    public Admin admin;  // single admin account
    private int nextOrderNumber = 1001;  // next numeric ID for new orders (starts from 1001)

    public DataPersistence(String baseDir) {
        this.baseDir = baseDir;
    }

    /** Helper to construct file path with base directory */
    public String path(String filename) {
        if (baseDir == null || baseDir.equals("")) {
            return filename;
        }
        return baseDir + "/" + filename;
    }

    /** Load all data from text files: products, orders, admins */
    public void loadAll() throws Exception {
        loadProducts();
        loadOrders();
        loadAdmins();
        // Set initial next order number based on max existing order number
        computeNextOrderNumber();
    }

    private void loadProducts() throws Exception {
        productCount = 0;
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(path("products.txt")));
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.length() == 0) continue;
                // Format: ProductID|Category|Brand|Name|Price|Stock
                String[] parts = line.split("\\|");
                if (parts.length < 6) continue;
                String pid = parts[0].trim();
                String category = parts[1].trim();
                String brand = parts[2].trim();
                String name = parts[3].trim();
                String priceStr = parts[4].trim().replace(",", "");
                String stockStr = parts[5].trim();
                int price = toInt(priceStr);
                int stock = toInt(stockStr);
                products[productCount++] = new Product(pid, category, brand, name, price, stock);
            }
        } catch (Exception e) {
            // If products.txt not found or format error, ignore (start with no products)
        } finally {
            if (br != null) br.close();
        }
    }

    private void loadOrders() throws Exception {
        orderCount = 0;
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(path("orders.txt")));
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.length() == 0) continue;
                // Format: OrderID|Date|Status|ItemList|Total|CancelReason
                String[] parts = line.split("\\|");
                if (parts.length < 5) continue;
                Order o = new Order();
                o.orderId = parts[0].trim();
                o.date = parts[1].trim();
                o.status = parts[2].trim();
                // Parse items list (e.g., "P501x2, L201x1")
                String itemsPart = parts[3].trim();
                parseItemsIntoOrder(o, itemsPart);
                // Total amount
                if (parts.length > 4) {
                    o.totalAmount = toInt(parts[4].trim());
                }
                // Cancel reason (if any)
                if (parts.length > 5) {
                    o.cancelReason = parts[5].trim();
                }
                // If order was shipped in a previous session, trackingId might be missing; assign if needed
                if ((o.status.equals("SHIPPED") || o.status.equals("OUT_FOR_DELIVERY") || o.status.equals("DELIVERED"))
                        && (o.trackingId == null || o.trackingId.equals(""))) {
                    o.trackingId = "TRK" + (o.orderId.length() > 1 ? o.orderId.substring(1) : o.orderId);
                }
                orders[orderCount++] = o;
            }
        } catch (Exception e) {
            // Ignore if orders.txt not found or unreadable
        } finally {
            if (br != null) br.close();
        }
    }

    private void loadAdmins() throws Exception {
        // Load admin credentials (assuming one admin account)
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(path("admins.txt")));
            String line;
            if ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.length() > 0) {
                    // Format: username,hashedPassword
                    String[] parts = line.split(",");
                    if (parts.length >= 2) {
                        String user = parts[0].trim();
                        String hash = parts[1].trim();
                        if (!user.equals("") && !hash.equals("")) {
                            admin = new Admin(user, hash);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // If admins.txt not found, use default admin
        } finally {
            if (br != null) br.close();
        }
        if (admin == null) {
            // If no admin loaded, create a default admin with username "admin" and password "admin123"
            admin = new Admin("admin", Admin.hashPassword("admin123"));
        }
    }

    private void computeNextOrderNumber() {
        int maxNum = 1000;
        for (int i = 0; i < orderCount; i++) {
            Order o = orders[i];
            if (o == null) continue;
            try {
                // Order IDs start with a letter (e.g., 'O1001'), so remove the first char
                String numStr = o.orderId.substring(1);
                int num = toInt(numStr);
                if (num > maxNum) {
                    maxNum = num;
                }
            } catch (Exception e) {
                // ignore malformed orderId
            }
        }
        nextOrderNumber = maxNum + 1;
    }

    /** Generate a new unique Order ID (e.g., "O1004") */
    public String generateOrderId() {
        String oid = "O" + nextOrderNumber;
        nextOrderNumber++;
        return oid;
    }

    /** Parse a string of items into an Order (e.g., "P501x2, L201x1") */
    public void parseItemsIntoOrder(Order order, String itemsPart) {
        if (order == null || itemsPart == null) return;
        String part = itemsPart.trim();
        if (part.length() == 0) return;
        String[] itemTokens = part.split(",");
        for (String token : itemTokens) {
            token = token.trim();
            if (token.length() == 0) continue;
            // Each token like "P501x2" (ProductID 'P501', quantity '2' separated by 'x')
            String[] kv = token.split("x");
            if (kv.length == 2) {
                String pid = kv[0].trim();
                int qty = toInt(kv[1].trim());
                if (!pid.equals("") && qty > 0) {
                    order.addItem(new Item(pid, qty));
                }
            }
        }
    }

    /** Find a Product by ID (returns null if not found) */
    public Product findProductById(String productId) {
        if (productId == null) return null;
        for (int i = 0; i < productCount; i++) {
            if (products[i] != null && products[i].productId.equalsIgnoreCase(productId)) {
                return products[i];
            }
        }
        return null;
    }

    /** Utility: convert string to int (return 0 if invalid) */
    public static int toInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return 0;
        }
    }

    /** Save all data back to text files (products, orders, admins) */
    public void saveAll() throws Exception {
        saveProducts();
        saveOrders();
        saveAdmins();
        // (Logs and receipts are written in real-time; no need to rewrite them here)
    }

    private void saveProducts() throws Exception {
        FileWriter fw = new FileWriter(path("products.txt"), false);
        for (int i = 0; i < productCount; i++) {
            Product p = products[i];
            if (p == null) continue;
            // Write in same format: ProductID|Category|Brand|Name|Price|Stock
            fw.write(p.productId + "|" + p.category + "|" + p.brand + "|" + p.name + "|" + p.price + "|" + p.stock + "\n");
        }
        fw.close();
    }

    private void saveOrders() throws Exception {
        FileWriter fw = new FileWriter(path("orders.txt"), false);
        for (int i = 0; i < orderCount; i++) {
            Order o = orders[i];
            if (o == null) continue;
            // Only save active (non-archived) orders. Format matches loadOrders.
            // If an order has no cancelReason, we omit that field.
            String itemList = "";
            for (int j = 0; j < o.itemCount; j++) {
                Item it = o.items[j];
                if (it == null) continue;
                itemList += it.productId + "x" + it.quantity;
                if (j < o.itemCount - 1) itemList += ", ";
            }
            if (o.cancelReason != null && !o.cancelReason.equals("")) {
                // Cancelled order: include cancel reason field
                fw.write(o.orderId + "|" + o.date + "|" + o.status + "|" + itemList + "|" + o.totalAmount + "|" + o.cancelReason + "\n");
            } else {
                // No cancel reason (pending or completed orders)
                fw.write(o.orderId + "|" + o.date + "|" + o.status + "|" + itemList + "|" + o.totalAmount + "\n");
            }
        }
        fw.close();
    }

    private void saveAdmins() throws Exception {
        FileWriter fw = new FileWriter(path("admins.txt"), false);
        if (admin != null) {
            fw.write(admin.username + "," + admin.passHash + "\n");
        }
        fw.close();
    }
}


