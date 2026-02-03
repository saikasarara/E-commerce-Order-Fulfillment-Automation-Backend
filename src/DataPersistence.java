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
public int orderSerial = 1;
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
// ===================== PRODUCTS =====================
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
p.category = parts[1].trim();
p.brand = parts[2].trim();
p.name = parts[3].trim();
p.price = toInt(parts[4].trim());
p.stock = toInt(parts[5].trim());
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
for (int i = 0; i < productCount; i++) {
Product p = products[i];
fw.write(p.productId + "|" + p.category + "|" + p.brand + "|" +
p.name + "|" + p.price + "|" + p.stock + "\n");
}
fw.close();
}
// ===================== ADMINS =====================
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
a.username = parts[0].trim();
a.passHashHex = parts[1].trim();
admins[adminCount++] = a;
}
} catch (Exception e) {
// ignore if file missing
} finally {
if (br != null) br.close();
}
}
// ===================== ORDERS =====================
private void loadOrders() throws Exception {
orderCount = 0;
BufferedReader br = null;
try {
br = new BufferedReader(new FileReader(path("orders.txt")));
String line;
while ((line = br.readLine()) != null) {
line = line.trim();
if (line.length() == 0) continue;
// Format: OrderID|Address|PaymentMode|Items|Status
String[] parts = line.split("\\|");
if (parts.length < 5) continue;
Order o = new Order();
o.orderId = parts[0].trim();
o.address = parts[1].trim();
o.paymentMode = parts[2].trim();
parseItemsIntoOrder(o, parts[3].trim());
o.status = parts[4].trim();
orders[orderCount++] = o;
}
} catch (Exception e) {
// ignore if file missing
} finally {
if (br != null) br.close();
}
}
private void parseItemsIntoOrder(Order o, String itemsPart) {
if (itemsPart == null) return;
itemsPart = itemsPart.trim();
if (itemsPart.length() == 0) return;
String[] itemTokens = itemsPart.split(",");
for (String token : itemTokens) {
String[] kv = token.trim().split(":");
if (kv.length == 2 && o.itemCount < 50) {
String pid = kv[0].trim();
int qty = toInt(kv[1].trim());
o.addItem(new Item(pid, qty));
}
}
}
private void saveOrders() throws Exception {
FileWriter fw = new FileWriter(path("orders.txt"), false);
for (int i = 0; i < orderCount; i++) {
Order o = orders[i];
String items = buildItemsString(o);
fw.write(o.orderId + "|" + o.address + "|" + o.paymentMode + "|" +
items + "|" + o.status + "\n");
}
fw.close();
}
private String buildItemsString(Order o) {
String items = "";
for (int k = 0; k < o.itemCount; k++) {
Item it = o.items[k];
if (k > 0) items += ",";
items += it.productId + ":" + it.qty;
}
return items;
}
// ===================== INVOICES =====================
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
inv.orderId = parts[1].trim();
inv.total = toInt(parts[2].trim());
invoices[invoiceCount++] = inv;
}
} catch (Exception e) {
// ignore if file missing
} finally {
if (br != null) br.close();
}
}
private void saveInvoices() throws Exception {
FileWriter fw = new FileWriter(path("invoices.txt"), false);
for (int i = 0; i < invoiceCount; i++) {
Invoice inv = invoices[i];
fw.write(inv.invoiceId + "|" + inv.orderId + "|" + inv.total +
"\n");
}
fw.close();
}
// ===================== SHIPMENTS =====================
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
sh.orderId = parts[1].trim();
sh.status = parts[2].trim();
shipments[shipmentCount++] = sh;
}
} catch (Exception e) {
// ignore if file missing
} finally {
if (br != null) br.close();
}
}
private void saveShipments() throws Exception {
FileWriter fw = new FileWriter(path("shipments.txt"), false);
for (int i = 0; i < shipmentCount; i++) {
Shipment sh = shipments[i];
fw.write(sh.trackingId + "|" + sh.orderId + "|" + sh.status + "\n");
}
fw.close();
}
private void computeSerials() {
// Determine next invoiceSerial
int maxInvNum = 0;
for (int i = 0; i < invoiceCount; i++) {
String invId = invoices[i].invoiceId;
if (invId != null && invId.startsWith("INV-")) {
String numPart = invId.substring(4);
int num = toInt(numPart);
if (num > maxInvNum) {
maxInvNum = num;
}
}
}
invoiceSerial = (maxInvNum >= 1) ? (maxInvNum + 1) : 1;
// Determine next trackingSerial
int maxTrkNum = 0;
for (int i = 0; i < shipmentCount; i++) {
String trkId = shipments[i].trackingId;
if (trkId != null && trkId.startsWith("TRK-")) {
String numPart = trkId.substring(4);
int num = toInt(numPart);
if (num > maxTrkNum) {
maxTrkNum = num;
}
}
}
trackingSerial = (maxTrkNum >= 1) ? (maxTrkNum + 1) : 1;
// Determine next orderSerial
int maxOrderNum = 0;
for (int i = 0; i < orderCount; i++) {
String oid = orders[i].orderId;
if (oid != null) {
// assume prefix letters followed by numeric part
int k = 0;
while (k < oid.length() && !Character.isDigit(oid.charAt(k))) {
k++;
}
String numPart = oid.substring(k);
int num = toInt(numPart);
if (num > maxOrderNum) {
maxOrderNum = num;
}
}
}
orderSerial = (maxOrderNum >= 1) ? (maxOrderNum + 1) : 1;
}
// ===================== LOOKUPS & UTILITIES =====================
public Product findProductById(String productId) {
for (int i = 0; i < productCount; i++) {
if (products[i].productId.equals(productId)) {
return products[i];
}
}
return null;
}
public Order findOrderById(String orderId) {
for (int i = 0; i < orderCount; i++) {
if (orders[i].orderId.equals(orderId)) {
return orders[i];
}
}
return null;
}
public String findNextPendingOrderId() {
for (int i = 0; i < orderCount; i++) {
if ("PENDING".equals(orders[i].status)) {
return orders[i].orderId;
}
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
public void printOrdersSummary() {
System.out.print("\n" + Utils.ANSI_BLUE + "Orders:" + Utils.ANSI_RESET
+ "\n");
System.out.print(Utils.ANSI_BLUE + "OrderID | Status | Payment Mode |Address" + Utils.ANSI_RESET + "\n");
for (int i = 0; i < orderCount; i++) {
Order o = orders[i];
System.out.print(o.orderId + " | " + o.status + " | " +
o.paymentMode + " | " + o.address + "\n");
}
}
private int toInt(String s) {
if (s == null) return 0;
int n = 0;
for (int i = 0; i < s.length(); i++) {
char c = s.charAt(i);
if (c < '0' || c > '9') break;
n = n * 10 + (c - '0');
}
return n;
}
}
