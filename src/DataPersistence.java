import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;

public class DataPersistence {
    private final String dataDir;
    public Admin[] admins = new Admin[20];
    public int adminCount = 0;
    public Product[] products = new Product[500];
    public int productCount = 0;
    public Order[] orders = new Order[500];
    public int orderCount = 0;
    public Invoice[] invoices = new Invoice[1000];
    public int invoiceCount = 0;
    public Shipment[] shipments = new Shipment[1000];
    public int shipmentCount = 0;
    public int orderSerial = 2001;
    public int invoiceSerial = 1;
    public int trackingSerial = 1;

    public DataPersistence(String dataDir) {
        this.dataDir = dataDir;
    }

    public String path(String file) {
        return dataDir + "/" + file;
    }

    public void loadAll() throws Exception {
        loadAdmins();
        loadProducts();
        loadOrders();
        loadInvoices();
        loadShipments();
        computeSerials();
    }

    public void saveAll() throws Exception {
        saveProducts();
        saveOrders();
        saveInvoices();
        saveShipments();
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
                Product p = new Product();
                p.productId = parts[0].trim();
                p.category  = parts[1].trim();
                p.brand     = parts[2].trim();
                p.name      = parts[3].trim();
                p.price     = toInt(parts[4].trim());
                p.stock     = toInt(parts[5].trim());
                products[productCount++] = p;
            }
        } catch (Exception e) {
            // file might not exist yet
        } finally {
            if (br != null) br.close();
        }
    }

    private void saveProducts() throws Exception {
        FileWriter fw = new FileWriter(path("products.txt"), false);
        int i = 0;
        while (i < productCount) {
            Product p = products[i];
            fw.write(p.productId + "|" + p.category + "|" + p.brand + "|" + p.name + "|" + p.price + "|" + p.stock + "\n");
            i++;
        }
        fw.close();
    }

    private void loadAdmins() throws Exception {
        adminCount = 0;
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(path("admins.txt")));
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.length() == 0) continue;
                // Format: username|passHashHex
                String[] parts = line.split("\\|");
                if (parts.length < 2) continue;
                Admin a = new Admin();
                a.username   = parts[0].trim();
                a.passHashHex = parts[1].trim();
                admins[adminCount++] = a;
            }
        } catch (Exception e) {
            // ignore if file not found
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
                // Format: OrderID|Address|PaymentMode|ItemList|STATUS
                String[] parts = line.split("\\|");
                if (parts.length < 5) continue;
                Order o = new Order();
                o.orderId     = parts[0].trim();
                o.address     = parts[1].trim();
                o.paymentMode = parts[2].trim();
                parseItemsIntoOrder(o, parts[3].trim());
                o.status      = parts[4].trim();
                orders[orderCount++] = o;
            }
        } catch (Exception e) {
            // ignore if file not found
        } finally {
            if (br != null) br.close();
        }
    }

    private void parseItemsIntoOrder(Order o, String itemsPart) {
        if (itemsPart == null) return;
        itemsPart = itemsPart.trim();
        if (itemsPart.length() == 0) return;
        String[] itemTokens = itemsPart.split(",");
        int i = 0;
        while (i < itemTokens.length && o.itemCount < 50) {
            String tok = itemTokens[i].trim();
            String[] kv = tok.split(":");
            if (kv.length == 2) {
                String pid = kv[0].trim();
                int qty = toInt(kv[1].trim());
                o.addItem(new Item(pid, qty));
            }
            i++;
        }
    }

    private void saveOrders() throws Exception {
        FileWriter fw = new FileWriter(path("orders.txt"), false);
        int i = 0;
        while (i < orderCount) {
            Order o = orders[i];
            String items = buildItemsString(o);
            fw.write(o.orderId + "|" + o.address + "|" + o.paymentMode + "|" + items + "|" + o.status + "\n");
            i++;
        }
        fw.close();
    }

    private String buildItemsString(Order o) {
        String items = "";
        int k = 0;
        while (k < o.itemCount) {
            Item it = o.items[k];
            if (k > 0) items += ",";
            items += it.productId + ":" + it.qty;
            k++;
        }
        return items;
    }

    private void loadInvoices() throws Exception {
        invoiceCount = 0;
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(path("invoices.txt")));
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.length() == 0) continue;
                // Format: InvoiceID|OrderID|Total
                String[] parts = line.split("\\|");
                if (parts.length < 3) continue;
                Invoice inv = new Invoice();
                inv.invoiceId = parts[0].trim();
                inv.orderId   = parts[1].trim();
                inv.total     = toInt(parts[2].trim());
                invoices[invoiceCount++] = inv;
            }
        } catch (Exception e) {
            // ignore if file not found
        } finally {
            if (br != null) br.close();
        }
    }

    private void saveInvoices() throws Exception {
        FileWriter fw = new FileWriter(path("invoices.txt"), false);
        int i = 0;
        while (i < invoiceCount) {
            Invoice inv = invoices[i];
            fw.write(inv.invoiceId + "|" + inv.orderId + "|" + inv.total + "\n");
            i++;
        }
        fw.close();
    }

    private void loadShipments() throws Exception {
        shipmentCount = 0;
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(path("shipments.txt")));
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.length() == 0) continue;
                // Format: TrackingID|OrderID|Status
                String[] parts = line.split("\\|");
                if (parts.length < 3) continue;
                Shipment sh = new Shipment();
                sh.trackingId = parts[0].trim();
                sh.orderId    = parts[1].trim();
                sh.status     = parts[2].trim();
                shipments[shipmentCount++] = sh;
            }
        } catch (Exception e) {
            // ignore if file not found
        } finally {
            if (br != null) br.close();
        }
    }

    private void saveShipments() throws Exception {
        FileWriter fw = new FileWriter(path("shipments.txt"), false);
        int i = 0;
        while (i < shipmentCount) {
            Shipment sh = shipments[i];
            fw.write(sh.trackingId + "|" + sh.orderId + "|" + sh.status + "\n");
            i++;
        }
        fw.close();
    }

    private void computeSerials() {
        // Next Order ID
        int maxOrderNum = 0;
        int i = 0;
        while (i < orderCount) {
            String id = orders[i].orderId;
            if (id != null && id.startsWith("O")) {
                int num = toInt(id.substring(1));
                if (num > maxOrderNum) {
                    maxOrderNum = num;
                }
            }
            i++;
        }
        orderSerial = (maxOrderNum >= 1) ? (maxOrderNum + 1) : 2001;
        // Next Invoice ID
        int maxInvNum = 0;
        i = 0;
        while (i < invoiceCount) {
            String invId = invoices[i].invoiceId;
            if (invId != null && invId.startsWith("INV-")) {
                String numStr = invId.substring(4);
                int num = toInt(numStr);
                if (num > maxInvNum) {
                    maxInvNum = num;
                }
            }
            i++;
        }
        invoiceSerial = (maxInvNum >= 1) ? (maxInvNum + 1) : 1;
        // Next Tracking ID
        int maxTrackNum = 0;
        i = 0;
        while (i < shipmentCount) {
            String tid = shipments[i].trackingId;
            if (tid != null && tid.startsWith("TRK-")) {
                String numStr = tid.substring(4);
                int num = toInt(numStr);
                if (num > maxTrackNum) {
                    maxTrackNum = num;
                }
            }
            i++;
        }
        trackingSerial = (maxTrackNum >= 1) ? (maxTrackNum + 1) : 1;
    }

    // Lookup and add methods
    public Product findProductById(String productId) {
        int i = 0;
        while (i < productCount) {
            if (products[i].productId.equals(productId)) return products[i];
            i++;
        }
        return null;
    }

    public Order findOrderById(String orderId) {
        int i = 0;
        while (i < orderCount) {
            if (orders[i].orderId.equals(orderId)) return orders[i];
            i++;
        }
        return null;
    }

    public String findNextPendingOrderId() {
        int i = 0;
        while (i < orderCount) {
            if ("PENDING".equals(orders[i].status)) return orders[i].orderId;
            i++;
        }
        return null;
    }

    public void addOrder(Order o) {
        orders[orderCount++] = o;
    }

    public void addInvoice(Invoice inv) {
        invoices[invoiceCount++] = inv;
    }

    public void addShipment(Shipment sh) {
        shipments[shipmentCount++] = sh;
    }

    public void addPurchaseHistory(Order o) {
        try {
            FileWriter fw = new FileWriter(path("purchase_history.txt"), true);
            String items = buildItemsString(o);
            fw.write(o.orderId + "|" + o.paymentMode + "|" + o.totalAmount + "|" + items + "\n");
            fw.close();
        } catch (Exception e) {
            // ignore
        }
    }

    public void printOrdersSummary() {
        System.out.print("\nOrders:\n");
        int i = 0;
        while (i < orderCount) {
            Order o = orders[i];
            System.out.print(o.orderId + " | " + o.status + " | " + o.paymentMode + " | " + o.address + "\n");
            i++;
        }
    }

    public void printPurchaseHistory() {
        System.out.print("\nPurchase History:\n");
        System.out.print("OrderID | PaymentMode | Total | Items\n");
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(path("purchase_history.txt")));
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.length() == 0) continue;
                System.out.print(line + "\n");
            }
        } catch (Exception e) {
            System.out.print("(No purchase history available)\n");
        } finally {
            try { if (br != null) br.close(); } catch (Exception e) { }
        }
    }

    public void printStockOverview() {
        System.out.print("\nStock Overview:\n");
        System.out.print("ProductID | Name | Price | Stock\n");
        try {
            FileWriter fw = new FileWriter(path("stock_report.txt"), false);
            int i = 0;
            while (i < productCount) {
                Product p = products[i];
                String line = p.productId + " | " + p.name + " | " + p.price + " | " + p.stock;
                System.out.print(line + "\n");
                fw.write(line + "\n");
                i++;
            }
            fw.close();
        } catch (Exception e) {
            System.out.print("Error generating stock report.\n");
        }
    }

    private int toInt(String s) {
        if (s == null) return 0;
        int n = 0;
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') break;
            n = n * 10 + (c - '0');
            i++;
        }
        return n;
    }
}
