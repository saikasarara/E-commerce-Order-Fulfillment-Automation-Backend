import java.io.BufferedReader;

public class Admin {
    public String username;
    public String passHashHex;

    public static boolean authenticate(DataPersistence dp, BufferedReader console) throws Exception {
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
            int i = 0;
            while (i < dp.adminCount) {
                Admin a = dp.admins[i];
                if (a.username.equals(u) && a.passHashHex.equals(hash)) {
                    System.out.print("Login successful.\n");
                    return true;
                }
                i++;
            }
            System.out.print("Invalid.\n");
            attempts++;
        }
        return false;
    }
}
