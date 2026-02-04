import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.FileReader;

public class Log {
    private DataPersistence dp;

    public Log(DataPersistence dp) {
        this.dp = dp;
    }

    /** Write a log entry for a given order (or admin action) */
    public void write(String orderId, String message) {
        try {
            FileWriter fw = new FileWriter(dp.path("logs.txt"), true);  // append mode
            fw.write("Order " + orderId + " - " + message + "\n");
            fw.close();
        } catch (Exception e) {
            // If logging fails, ignore in this simple implementation
        }
    }

    /** Display the timeline of log events for a specific order ID */
    public void showOrderTimeline(String orderId) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(dp.path("logs.txt")));
            String line;
            boolean foundAny = false;
            while ((line = br.readLine()) != null) {
                if (line.contains("Order " + orderId + " ")) {
                    System.out.print(line + "\n");
                    foundAny = true;
                }
            }
            br.close();
            if (!foundAny) {
                System.out.print("No log entries found for Order " + orderId + ".\n");
            }
        } catch (Exception e) {
            System.out.print("Error reading logs.\n");
        }
    }
}

