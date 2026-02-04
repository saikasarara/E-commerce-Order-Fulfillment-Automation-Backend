public class InvoiceService {
    private final DataPersistence dp;
    private final Log log;
    public InvoiceService(DataPersistence dp, Log log) {
        this.dp = dp;
        this.log = log;
    }
    public Invoice generateInvoice(Order o) {
        int total = 0;
        int i = 0;
        while (i < o.itemCount) {
            Item it = o.items[i];
            Product p = dp.findProductById(it.productId);
            total += (p.price * it.qty);
            i++;
        }
        String invId = "INV-" + Utils.padLeft(dp.invoiceSerial++, 4);
        Invoice inv = new Invoice();
        inv.invoiceId = invId;
        inv.orderId = o.orderId;
        inv.total = total;
        dp.addInvoice(inv);
        o.totalAmount = total;
        o.invoiceId = invId;
        log.write(o.orderId, "INVOICE", "OK", "Generated " + invId + " Total=" + total);
        return inv;
    }
}
