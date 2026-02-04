import java.security.MessageDigest;

public class Utils {
    public static String sha256Hex(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(input.getBytes("UTF-8"));
        char[] digits = "0123456789abcdef".toCharArray();
        char[] hex = new char[hash.length * 2];
        int i = 0;
        while (i < hash.length) {
            int v = hash[i] & 0xFF;
            hex[i * 2] = digits[v >>> 4];
            hex[i * 2 + 1] = digits[v & 0x0F];
            i++;
        }
        return new String(hex);
    }
    public static String padLeft(int n, int width) {
        String s = "" + n;
        while (s.length() < width) s = "0" + s;
        return s;
    }
    public static String timestamp() {
        return "T" + System.currentTimeMillis();
    }
    // ANSI color codes for console output
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLUE = "\u001B[94m";
    public static final String ANSI_MAGENTA = "\u001B[95m";
}

