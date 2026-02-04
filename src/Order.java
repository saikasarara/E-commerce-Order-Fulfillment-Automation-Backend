public class Order {
    public String orderId;
    public String address;
    public String paymentMode;
    public Item[] items = new Item[50];
    public int itemCount = 0;
    public String status = "PENDING";
    public String cancelReason = "";
    public int totalAmount = 0;
    public String invoiceId = "";
    public String trackingId = "";

    public void addItem(Item it) {
        if (itemCount < 50) {
            items[itemCount++] = it;
        }
    }
}
