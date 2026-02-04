import java.io.FileWriter;

public class ReportService {
    /** Generate a summary report of orders (revenue, cancellations) and write to report.txt */
    public static void generateReport(DataPersistence dp) {
        try {
            int totalOrders = dp.orderCount;
            int completedCount = 0;
            int cancelledCount = 0;
            int revenueSum = 0;
            // Count cancellation reasons
            String[] reasons = new String[totalOrders];
            int[] reasonCounts = new int[totalOrders];
            int reasonTypes = 0;
            for (int i = 0; i < dp.orderCount; i++) {
                Order o = dp.orders[i];
                if (o == null) continue;
                if (o.status.equalsIgnoreCase("DELIVERED")) {
                    completedCount++;
                    revenueSum += o.totalAmount;
                }
                if (o.status.equalsIgnoreCase("CANCELLED")) {
                    cancelledCount++;
                    String reason = (o.cancelReason == null || o.cancelReason.equals("") ? "Unknown" : o.cancelReason);
                    // accumulate reason counts
                    boolean found = false;
                    for (int r = 0; r < reasonTypes; r++) {
                        if (reasons[r].equalsIgnoreCase(reason)) {
                            reasonCounts[r]++;
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        reasons[reasonTypes] = reason;
                        reasonCounts[reasonTypes] = 1;
                        reasonTypes++;
                    }
                }
            }
            // Sort cancellation reasons by frequency (descending)
            for (int i = 0; i < reasonTypes - 1; i++) {
                int maxIndex = i;
                for (int j = i + 1; j < reasonTypes; j++) {
                    if (reasonCounts[j] > reasonCounts[maxIndex]) {
                        maxIndex = j;
                    }
                }
                // swap
                String tmpReason = reasons[i];
                reasons[i] = reasons[maxIndex];
                reasons[maxIndex] = tmpReason;
                int tmpCount = reasonCounts[i];
                reasonCounts[i] = reasonCounts[maxIndex];
                reasonCounts[maxIndex] = tmpCount;
            }
            // Build report content
            StringBuilder report = new StringBuilder();
            report.append("Total Orders: ").append(totalOrders).append("\n");
            report.append("Completed Orders: ").append(completedCount).append("\n");
            report.append("Cancelled Orders: ").append(cancelledCount).append("\n");
            report.append("Total Revenue: BDT ").append(revenueSum).append("\n");
            report.append("Top 3 Cancellation Reasons:\n");
            for (int k = 0; k < reasonTypes && k < 3; k++) {
                report.append((k + 1) + ". " + reasons[k] + " – " + reasonCounts[k] + "\n");
            }
            // Write report to file
            FileWriter fw = new FileWriter(dp.path("report.txt"), false);
            fw.write(report.toString());
            fw.close();
            // Also output summary to console
            System.out.print("=== Report Summary ===\n");
            System.out.print(report.toString());
            System.out.print("(Report saved to report.txt)\n");
        } catch (Exception e) {
            System.out.print("Failed to generate report.\n");
        }
    }

    /** Export current stock levels of all products to stock_report.txt */
    public static void exportStockReport(DataPersistence dp) {
        try {
            FileWriter fw = new FileWriter(dp.path("stock_report.txt"), false);
            fw.write("ProductID | Name | Price | Stock\n");
            for (int i = 0; i < dp.productCount; i++) {
                Product p = dp.products[i];
                if (p == null) continue;
                fw.write(p.productId + " | " + p.name + " | " + p.price + " | " + p.stock + "\n");
            }
            fw.close();
            System.out.print("Stock report generated in stock_report.txt\n");
        } catch (Exception e) {
            System.out.print("Failed to export stock report.\n");
        }
    }
}
