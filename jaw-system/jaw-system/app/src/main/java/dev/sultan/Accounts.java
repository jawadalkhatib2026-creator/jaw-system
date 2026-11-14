package dev.sultan;

import android.content.Context;
import android.content.SharedPreferences;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

/** Jaw accounts live only on this TV (no server). Passwords are salted and hashed. */
final class Accounts {
    private static final String ADMIN_USER = "bc78e58d55cde1346e68f8e5fe588dedf62fa457aa646a500a53347faff6ee24";
    private static final String ADMIN_PASS = "5e965f2c2181cf56d74bf2bcbddd221712ef0435d839d6a2bab12317180001a1";
    static boolean adminOk = false;

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences("jaw_accounts", Context.MODE_PRIVATE);
    }

    static String sha(String s) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(s.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format(Locale.US, "%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    static boolean isAdmin(String user, String pass) {
        return sha(user.trim()).equals(ADMIN_USER) && sha(pass).equals(ADMIN_PASS);
    }

    static JSONArray all(Context c) {
        try { return new JSONArray(sp(c).getString("list", "[]")); }
        catch (Exception e) { return new JSONArray(); }
    }

    static int count(Context c) { return all(c).length(); }

    /** Returns null on success, otherwise a short error message. */
    static String create(Context c, String email, String pass) {
        String em = email == null ? "" : email.trim().toLowerCase(Locale.US);
        if (sha(email == null ? "" : email.trim()).equals(ADMIN_USER)) return "Reserved / محجوز";
        if (!em.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) return "Invalid email / بريد غير صالح";
        if (pass == null || pass.length() < 6) return "Password too short / كلمة المرور قصيرة";
        try {
            JSONArray a = all(c);
            for (int i = 0; i < a.length(); i++)
                if (em.equals(a.getJSONObject(i).optString("email"))) return "Already exists / موجود مسبقاً";
            byte[] sb = new byte[8];
            new SecureRandom().nextBytes(sb);
            String salt = sha(new String(sb, "ISO-8859-1")).substring(0, 16);
            JSONObject o = new JSONObject();
            o.put("email", em);
            o.put("salt", salt);
            o.put("hash", sha(salt + pass));
            a.put(o);
            sp(c).edit().putString("list", a.toString()).apply();
            return null;
        } catch (Exception e) {
            return "Error / خطأ";
        }
    }

    static boolean verify(Context c, String email, String pass) {
        try {
            String em = email.trim().toLowerCase(Locale.US);
            JSONArray a = all(c);
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                if (em.equals(o.optString("email")) && sha(o.optString("salt") + pass).equals(o.optString("hash"))) return true;
            }
        } catch (Exception e) { }
        return false;
    }

    static void delete(Context c, String email) {
        try {
            JSONArray a = all(c), out = new JSONArray();
            for (int i = 0; i < a.length(); i++)
                if (!email.equals(a.getJSONObject(i).optString("email"))) out.put(a.getJSONObject(i));
            sp(c).edit().putString("list", out.toString()).apply();
            if (email.equals(Store.user(c))) Store.setUser(c, null);
        } catch (Exception e) { }
    }

    static void clear(Context c) {
        sp(c).edit().clear().apply();
        Store.setUser(c, null);
    }
}
