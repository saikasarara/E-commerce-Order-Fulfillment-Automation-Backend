public class Admin {
    public String username;
    public String passHash;  // stored password hash (hex string)

    public Admin(String username, String passHash) {
        this.username = username;
        this.passHash = passHash;
    }

    /** Securely authenticate admin login with SHA-256 hashed password (3 attempts max) */
    public static boolean authenticate(DataPersistence dp, BufferedReader console) throws Exception {
        System.out.print("\n=== Admin Login ===\n");
        int attempts = 0;
        while (attempts < 3) {
            // Prompt for credentials
            System.out.print("Username: ");
            String u = console.readLine();
            if (u == null) u = "";
            u = u.trim();
            System.out.print("Password: ");
            String p = console.readLine();
            if (p == null) p = "";
            p = p.trim();
            // Verify username and hashed password
            Admin admin = dp.admin;  // we assume a single admin account
            if (admin != null && admin.username.equals(u) && admin.passHash.equals(hashPassword(p))) {
                System.out.print("Login successful.\n");
                return true;
            } else {
                System.out.print("Invalid credentials. ");
                attempts++;
                if (attempts < 3) {
                    System.out.print("Try again.\n");
                }
            }
        }
        System.out.print("\nToo many failed attempts.\n");
        return false;
    }

    /** Compute SHA-256 hash of a plaintext password and return hex string */
    public static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");  // Allowed function
            byte[] hashBytes = md.digest(password.getBytes());        // Allowed function
            // Convert hash bytes to hex string
            String hexDigits = "0123456789abcdef";
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                int val = b & 0xFF;
                sb.append(hexDigits.charAt(val >>> 4));
                sb.append(hexDigits.charAt(val & 0x0F));
            }
            return sb.toString();
        } catch (Exception e) {
            // In case of error, fall back to plain password (should not happen in practice)
            return password;
        }
    }
}

