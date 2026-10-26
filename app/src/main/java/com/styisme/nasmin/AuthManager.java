package com.styisme.nasmin;

import android.content.Context;
import android.util.Base64;

public class AuthManager {
    private static final ThreadLocal<String> currentUser = new ThreadLocal<String>() {
        @Override protected String initialValue() { return "admin"; }
    };

    public static void init(Context context) {
        DatabaseManager.init(context);
    }

    public static String getCurrentUser() { return currentUser.get(); }
    public static void setCurrentUser(String u) { currentUser.set(u); }
    public static void clearCurrentUser() { currentUser.remove(); }

    public static int verifyRole(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Basic ")) return 0;
        try {
            String b64 = authHeader.substring(6).trim();
            byte[] dec = Base64.decode(b64, Base64.NO_WRAP);
            String s = new String(dec, "UTF-8");
            String[] parts = s.split(":", 2);
            if (parts.length == 2) {
                int role = DatabaseManager.getInstance().verifyUser(parts[0], parts[1]);
                if (role > 0) {
                    currentUser.set(parts[0]);
                    return role;
                }
            }
        } catch (Exception e) { return 0; }
        return 0;
    }
}
