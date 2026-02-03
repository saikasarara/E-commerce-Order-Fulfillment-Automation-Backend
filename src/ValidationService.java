public class ValidationService {
private final Log log;
public ValidationService(Log log) {
this.log = log;
}
public boolean validate(Order o) {
if (o.orderId == null || o.orderId.trim().length() == 0) {
log.write(o.orderId, "VALIDATION", "FAIL", "Missing orderId");
return false;
}
if (o.itemCount <= 0) {
log.write(o.orderId, "VALIDATION", "FAIL", "No items");
return false;
}
for (int i = 0; i < o.itemCount; i++) {
Item it = o.items[i];
if (it == null || it.productId == null ||
it.productId.trim().length() == 0) {
log.write(o.orderId, "VALIDATION", "FAIL", "Invalid productId");
return false;
}
if (it.qty <= 0) {
log.write(o.orderId, "VALIDATION", "FAIL", "Invalid qty");
return false;
}
}
log.write(o.orderId, "VALIDATION", "OK", "Valid order");
return true;
}
}

