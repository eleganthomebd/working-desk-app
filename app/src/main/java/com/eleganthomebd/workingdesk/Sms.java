package com.eleganthomebd.workingdesk;

import android.content.Context;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/** Which SMS may leave the phone, and how they are sent to the server. */
final class Sms {
    private Sms() {}

    /** An actual one-time code / password inside the message (not just "never share your PIN"). */
    private static final Pattern CODE = Pattern.compile(
            "(otp|one[ -]?time|pass ?code|password|\\bpin\\b|verification|verify|code|কোড|ওটিপি|পাসওয়ার্ড|পিন)[^0-9]{0,30}\\b\\d{4,8}\\b"
          + "|\\b\\d{4,8}\\b[^0-9]{0,25}(is your|otp|code|কোড|ওটিপি)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    /** Money / bill words — lets a bill SMS through even from an ordinary number. */
    private static final Pattern MONEY = Pattern.compile(
            "(tk|bdt|taka|৳|টাকা|bill|বিল|due|payment|cash ?in|cash ?out|send money|received|debited|credited)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    /** Personal mobile number like +8801XXXXXXXXX / 01XXXXXXXXX. */
    private static final Pattern PERSON = Pattern.compile("^(\\+?88)?01\\d{9}$");

    /** true = may go to the server. OTP / password SMS never do. */
    static boolean allowed(String from, String msg) {
        if (msg == null || msg.trim().isEmpty()) return false;
        if (CODE.matcher(msg).find()) return false;
        String f = from == null ? "" : from.replaceAll("[\\s-]", "");
        if (PERSON.matcher(f).matches() && !MONEY.matcher(msg).find()) return false;   // personal chat
        return true;
    }

    /** POST one SMS. Returns HTTP code (or -1 on network error). */
    static int post(Context c, String from, String msg, long at, boolean old) {
        try {
            String body = "k=" + enc(Store.key(c)) + "&from=" + enc(from) + "&msg=" + enc(msg)
                    + "&at=" + at + "&via=app" + (old ? "&imp=1" : "");
            HttpURLConnection h = (HttpURLConnection) new URL(Store.endpoint(c)).openConnection();
            h.setConnectTimeout(20000);
            h.setReadTimeout(30000);
            h.setRequestMethod("POST");
            h.setDoOutput(true);
            h.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
            try (OutputStream o = h.getOutputStream()) { o.write(body.getBytes(StandardCharsets.UTF_8)); }
            int code = h.getResponseCode();
            drain(h);
            return code;
        } catch (Exception e) {
            return -1;
        }
    }

    /** "I'm alive" ping (server shows a warning when the phone goes quiet). */
    static int heartbeat(Context c) {
        try {
            HttpURLConnection h = (HttpURLConnection) new URL(Store.endpoint(c) + "?hb=1&via=app&perm=" + (permOk(c) ? 1 : 0) + "&ver=" + Updater.current(c) + "&pend=" + Store.p(c).getInt("pend", 0) + "&k=" + enc(Store.key(c))).openConnection();
            h.setConnectTimeout(20000);
            h.setReadTimeout(20000);
            int code = h.getResponseCode();
            drain(h);
            return code;
        } catch (Exception e) {
            return -1;
        }
    }

    static boolean permOk(Context c) {
        return c.checkSelfPermission(android.Manifest.permission.RECEIVE_SMS) == android.content.pm.PackageManager.PERMISSION_GRANTED
            && c.checkSelfPermission(android.Manifest.permission.READ_SMS) == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    private static void drain(HttpURLConnection h) {
        try {
            InputStream in = h.getResponseCode() < 400 ? h.getInputStream() : h.getErrorStream();
            if (in != null) { byte[] b = new byte[1024]; while (in.read(b) > 0) { } in.close(); }
        } catch (Exception ignored) { }
        h.disconnect();
    }

    private static String enc(String s) {
        try { return URLEncoder.encode(s == null ? "" : s, "UTF-8"); } catch (Exception e) { return ""; }
    }
}
