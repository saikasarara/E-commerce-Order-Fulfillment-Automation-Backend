import java.util.Arrays;
public class Order {
    public String orderId;
    public String date;        // e.g., "2026-02-01"
    public String status;
    public Item[] items = new Item[10];  // max 10 items per order
    public int itemCount = 0;
    public int totalAmount;
    public String cancelReason;
    public String trackingId;

    public Order() {
        this.status = "PENDING";
        this.cancelReason = "";
        this.trackingId = "";
    }

    /** Add an item to this order (returns false if capacity reached or invalid quantity) */
    public boolean addItem(Item it) {
        if (it == null || it.quantity <= 0) {
            // Reject invalid item input (e.g., zero quantity)
            return false;
        }
        if (itemCount >= items.length) {
            return false;
        }
        items[itemCount++] = it;
        return true;
    }

    @Override
    public String toString() {
        // Useful for debugging: represent order as string
        String[] itemStrs = new String[itemCount];
        for (int i = 0; i < itemCount; i++) {
            Item it = items[i];
            if (it != null) {
                itemStrs[i] = it.productId + "x" + it.quantity;
            }
        }
        String itemList = String.join(", ", Arrays.copyOf(itemStrs, itemCount));
        return orderId + "|" + date + "|" + status + "|" + itemList + "|" + totalAmount + "|" + (cancelReason == null ? "" : cancelReason);
    }
}

