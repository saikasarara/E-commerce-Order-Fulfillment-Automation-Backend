import java.io.BufferedReader;
public class Admin {
public String username;
public String passHashHex;
public static boolean authenticate(DataPersistence dp, BufferedReader
console) throws Exception {
int attempts = 0;
while (attempts < 3) {
System.out.print("Username: ");
String u = console.readLine();
if (u == null) u = "";
u = u.trim();
System.out.print("Password: ");
String p = console.readLine();
if (p == null) p = "";
p = p.trim();
String hash = Utils.sha256Hex(p);
for (int i = 0; i < dp.adminCount; i++) {
Admin a = dp.admins[i];
if (a.username.equals(u) && a.passHashHex.equals(hash)) {
System.out.print(Utils.ANSI_BLUE + "Login successful." +
Utils.ANSI_RESET + "\n");
return true;
}
}
System.out.print(Utils.ANSI_PINK + "Invalid." + Utils.ANSI_RESET +
"\n");
attempts++;
}
return false;
}
}