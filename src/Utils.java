import java.security.MessageDigest;
public class Utils {
public static final String ANSI_BLUE = "\u001B[34m";
public static final String ANSI_PINK = "\u001B[95m";
public static final String ANSI_RESET = "\u001B[0m";
public static String sha256Hex(String input) throws Exception {
MessageDigest md = MessageDigest.getInstance("SHA-256");
byte[] hash = md.digest(input.getBytes("UTF-8"));
char[] digits = "0123456789abcdef".toCharArray();
char[] hex = new char[hash.length * 2];
for (int i = 0; i < hash.length; i++) {
int v = hash[i] & 0xFF;
hex[i * 2] = digits[v >>> 4];
hex[i * 2 + 1] = digits[v & 0x0F];
}
return new String(hex);
}
public static String padLeft(int n, int width) {

String s = "" + n;
while (s.length() < width) {
s = "0" + s;
}
return s;
}
public static String timestamp() {
return "T" + System.currentTimeMillis();
}
}
