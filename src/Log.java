import java.io.FileWriter;

public class Log {
    private final DataPersistence dp;
    public Log(DataPersistence dp) {
        this.dp = dp;
    }
    public void write(String orderId, String step, String result, String msg) {
        try {
            FileWriter fw = new FileWriter(dp.path("logs.txt"), true);
            fw.write(Utils.timestamp() + "|" + orderId + "|" + step + "|" + result + "|" + msg + "\n");
            fw.close();
        } catch (Exception e) {
            // ignore
        }
    }
}
