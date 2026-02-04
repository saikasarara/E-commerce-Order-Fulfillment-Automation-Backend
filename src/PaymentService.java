public class PaymentService {
    /** Simulate a payment process for an order. Returns true if payment succeeds, false if it fails. */
    public static boolean simulatePayment(Order order) {
        // Simulate payment success/failure by a simple heuristic:
        // Fail the payment if the total amount is divisible by 7 (arbitrary condition to simulate failures).
        if (order.totalAmount % 7 == 0) {
            return false;
        }
        return true;
    }
}

