import java.io.FileWriter;

public class InvoiceService {
    /** Generate an invoice record for a completed order (writes to invoices.txt). */
    public static void generateInvoice(Order order, DataPersistence dp) {
        if (order == null) return;
        try {
            FileWriter fw = new FileWriter(dp.path("invoices.txt"), true);
            String invoiceId = "INV" + (order.orderId.length() > 1 ? order.orderId.substring(1) : order.orderId);
            fw.write(invoiceId + "|" + order.orderId + "|" + order.totalAmount + "|" + order.date + "\n");
            fw.close();
        } catch (Exception e) {
            // ignore invoice writing errors
        }
    }
}
