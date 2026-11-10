package com.eleganthomebd.workingdesk;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Settings + last status, kept only on this phone. */
final class Store {
    static final String DESK_URL = "https://foysal.eleganthomebd.com/";
    /** 5 Nov 2026 00:00 Dhaka — SMS stopped reaching the server from here. */
    static final long BACKFILL_FROM = 1793815200000L;

    private Store() {}

    static SharedPreferences p(Context c) {
        return c.getSharedPreferences("wd", Context.MODE_PRIVATE);
    }

    /** Endpoint like https://host/sms (no key). */
    static String endpoint(Context c) { return p(c).getString("ep", ""); }
    static String key(Context c) { return p(c).getString("k", ""); }
    static boolean configured(Context c) { return !endpoint(c).isEmpty() && !key(c).isEmpty(); }

    /**
     * Accepts the URL copied from MacroDroid's HTTP request (…/sms?k=KEY&…).
     * Returns null when OK, otherwise a Bangla error message.
     */
    static String saveFromPastedUrl(Context c, String raw) {
        if (raw == null) return "লিংক দিন";
        String s = raw.trim();
        Uri u;
        try { u = Uri.parse(s); } catch (Exception e) { return "লিংকটা ঠিক নেই"; }
        if (u.getScheme() == null || u.getHost() == null) return "লিংকটা ঠিক নেই — পুরো লিংক কপি করুন";
        String k = u.getQueryParameter("k");
        if (k == null || k.trim().isEmpty() || k.contains("{") || k.contains("%") || k.contains("•")) return "লিংকে k= চাবিটা পাওয়া যায়নি";
        String ep = "https://" + u.getHost() + "/sms";
        p(c).edit().putString("ep", ep).putString("k", k.trim()).apply();
        return null;
    }

    static void status(Context c, String key, String val) {
        p(c).edit().putString(key, now() + "  " + val).apply();
    }

    static String get(Context c, String key) { return p(c).getString(key, "—"); }

    static void addSent(Context c) {
        SharedPreferences sp = p(c);
        sp.edit().putInt("sent", sp.getInt("sent", 0) + 1).apply();
    }

    static String now() {
        SimpleDateFormat f = new SimpleDateFormat("d MMM, h:mm a", Locale.ENGLISH);
        f.setTimeZone(TimeZone.getTimeZone("Asia/Dhaka"));
        return f.format(new Date());
    }
}
