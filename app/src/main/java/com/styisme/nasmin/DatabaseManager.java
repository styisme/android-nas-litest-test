package com.styisme.nasmin;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.security.MessageDigest;
import java.security.SecureRandom;

public class DatabaseManager extends SQLiteOpenHelper {
    private static final String DB_NAME = "nasmin.db";
    private static final int DB_VERSION = 1;
    private static DatabaseManager instance;

    public static void init(Context context) {
        if (instance == null) instance = new DatabaseManager(context.getApplicationContext());
    }

    public static DatabaseManager getInstance() { return instance; }

    private DatabaseManager(Context context) { super(context, DB_NAME, null, DB_VERSION); }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE users (username TEXT PRIMARY KEY, password TEXT, salt TEXT, role INTEGER, root_dir TEXT)");
        String s1 = genSalt();
        db.execSQL("INSERT INTO users VALUES ('admin', ?, ?, 1, '')", new Object[]{hash("admin", s1), s1});
        String s2 = genSalt();
        db.execSQL("INSERT INTO users VALUES ('guest', ?, ?, 2, 'Public')", new Object[]{hash("guest", s2), s2});
    }

    @Override public void onUpgrade(SQLiteDatabase db, int o, int n) {}

    public static String genSalt() {
        byte[] b = new byte[16];
        new SecureRandom().nextBytes(b);
        StringBuilder sb = new StringBuilder();
        for (byte x : b) sb.append(String.format("%02x", x));
        return sb.toString();
    }

    public static String hash(String pwd, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest((pwd + salt).getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { return pwd; }
    }

    public int verifyUser(String user, String pass) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT role, password, salt FROM users WHERE username=?", new String[]{user});
        try {
            if (c.moveToFirst()) {
                String stored = c.getString(1);
                String salt = c.getString(2);
                if (salt == null) salt = "";
                if (stored != null && stored.equals(hash(pass, salt))) return c.getInt(0);
            }
            return 0;
        } finally { c.close(); }
    }

    public String getUserRoot(String user) {
        if (user == null) return "";
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT root_dir FROM users WHERE username=?", new String[]{user});
        try {
            if (c.moveToFirst()) {
                String r = c.getString(0);
                return r == null ? "" : r.replaceAll("^/+", "");
            }
            return "";
        } finally { c.close(); }
    }

    public boolean changePassword(String user, String newPass) {
        try {
            String salt = genSalt();
            ContentValues cv = new ContentValues();
            cv.put("password", hash(newPass, salt));
            cv.put("salt", salt);
            getWritableDatabase().update("users", cv, "username=?", new String[]{user});
            return true;
        } catch (Exception e) { return false; }
    }

    public static boolean isDefaultPassword() {
        try {
            DatabaseManager inst = getInstance();
            if (inst == null) return true;
            SQLiteDatabase db = inst.getReadableDatabase();
            Cursor c = db.rawQuery("SELECT password, salt FROM users WHERE username='admin'", null);
            try {
                if (c.moveToFirst()) {
                    String stored = c.getString(0);
                    String salt = c.getString(1);
                    if (salt == null) salt = "";
                    return stored != null && stored.equals(hash("admin", salt));
                }
                return true;
            } finally { c.close(); }
        } catch (Exception e) { return true; }
    }
}
