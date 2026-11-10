package com.eleganthomebd.workingdesk;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Looks at foysal.eleganthomebd.com/app/version.json; a newer build → update popup. */
final class Updater {
    static final String APK_URL = Store.DESK_URL + "app/WorkingDesk.apk";
    private static final String VER_URL = Store.DESK_URL + "app/version.json";

    private Updater() {}

    static int current(Context c) {
        try {
            android.content.pm.PackageInfo i = c.getPackageManager().getPackageInfo(c.getPackageName(), 0);
            return android.os.Build.VERSION.SDK_INT >= 28 ? (int) i.getLongVersionCode() : i.versionCode;
        }
        catch (Exception e) { return 0; }
    }

    /** Network call — run off the main thread. Saves the newest versionCode seen. */
    static void check(Context c) {
        try {
            HttpURLConnection h = (HttpURLConnection) new URL(VER_URL + "?t=" + System.currentTimeMillis()).openConnection();
            h.setConnectTimeout(10000);
            h.setReadTimeout(10000);
            if (h.getResponseCode() != 200) { h.disconnect(); return; }
            InputStream in = h.getInputStream();
            java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[2048];
            for (int r; (r = in.read(buf)) > 0; ) bo.write(buf, 0, r);
            String s = new String(bo.toByteArray(), StandardCharsets.UTF_8);
            in.close();
            h.disconnect();
            Matcher m = Pattern.compile("\"versionCode\"\\s*:\\s*(\\d+)").matcher(s);
            if (m.find()) Store.p(c).edit().putInt("upd_ver", Integer.parseInt(m.group(1))).apply();
        } catch (Exception ignored) { }
    }

    static boolean pending(Context c) { return Store.p(c).getInt("upd_ver", 0) > current(c); }

    /** Must-update popup (only button: download the new APK). */
    static void show(Activity a) {
        new AlertDialog.Builder(a)
                .setTitle("নতুন আপডেট এসেছে")
                .setMessage("Working Desk অ্যাপের নতুন সংস্করণ এসেছে। আপডেট চাপুন, ডাউনলোড শেষ হলে ফাইলটা খুলে Install (Update) দিন। আপনার সেটআপ আর SMS অনুমতি আগের মতোই থাকবে।")
                .setCancelable(false)
                .setPositiveButton("আপডেট করুন", (d, w) -> {
                    a.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(APK_URL)));
                    a.finish();
                })
                .show();
    }
}
