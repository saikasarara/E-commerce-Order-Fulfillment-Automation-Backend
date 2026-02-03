import java.io.BufferedReader;
public class PaymentService {
private final Log log;
public PaymentService(Log log) {
this.log = log;
}
public boolean processPayment(Order order, BufferedReader console) throws
Exception {
String mode = safe(order.paymentMode);
if (equalsIgnoreCase(mode, "COD")) {
log.write(order.orderId, "PAYMENT", "OK", "COD approved");
return true;
}
if (equalsIgnoreCase(mode, "MockCard")) {
System.out.print(Utils.ANSI_BLUE + "MockCard Payment: Total " +
order.totalAmount + Utils.ANSI_RESET + "\n");
System.out.print(Utils.ANSI_BLUE + "Approve? (Y/N): " +
Utils.ANSI_RESET);
String ans = safe(console.readLine());
if (startsWithYes(ans)) {
log.write(order.orderId, "PAYMENT", "OK", "Approved");
return true;
} else {
log.write(order.orderId, "PAYMENT", "FAIL", "Declined");
return false;
}
}
log.write(order.orderId, "PAYMENT", "FAIL", "Unknown payment mode: " +
mode);
return false;
}
private String safe(String s) {
return (s == null ? "" : s.trim());
}
private boolean equalsIgnoreCase(String a, String b) {
return a != null && a.equalsIgnoreCase(b);
}
private boolean startsWithYes(String s) {
if (s.length() == 0) return false;
char c = s.charAt(0);
return (c == 'Y' || c == 'y');
}
}
