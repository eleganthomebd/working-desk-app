package com.eleganthomebd.workingdesk;

import android.Manifest;
import android.app.Activity;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** One-time setup: paste the MacroDroid link, give SMS permission, see status. */
public class SetupActivity extends Activity {
    private static final int REQ = 7;
    private TextView status;
    private EditText link;
    private LinearLayout box;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(Color.parseColor("#F6F8FB"));
        box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int p = dp(18);
        box.setPadding(p, dp(28), p, p);
        sv.addView(box);

        TextView t = text("Working Desk · SMS সেটআপ", 20, true, "#1F2937");
        box.addView(t);
        box.addView(text("লেনদেনের SMS নিজে থেকেই সার্ভারে যাবে। OTP/পাসওয়ার্ডের SMS কখনো যাবে না।", 14, false, "#6B7280"));

        box.addView(label("১. MacroDroid-এর লিংক পেস্ট করুন"));
        link = new EditText(this);
        link.setHint("https://…/sms?k=…");
        link.setTextSize(14);
        link.setSingleLine(false);
        box.addView(link, full());
        LinearLayout row = new LinearLayout(this);
        row.addView(button("ক্লিপবোর্ড থেকে", "#5F6368", v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm.hasPrimaryClip() && cm.getPrimaryClip().getItemCount() > 0)
                link.setText(String.valueOf(cm.getPrimaryClip().getItemAt(0).coerceToText(this)));
        }), weight());
        row.addView(button("সেভ", "#1A73E8", v -> {
            String err = Store.saveFromPastedUrl(this, link.getText().toString());
            toast(err == null ? "সেভ হয়েছে ✅" : err);
            refresh();
        }), weight());
        box.addView(row, full());

        box.addView(label("২. SMS পড়ার অনুমতি"));
        box.addView(button("SMS অনুমতি দিন", "#34A853", v ->
                requestPermissions(new String[]{Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS}, REQ)), full());
        box.addView(text("“Restricted setting” লেখা এলে: নিচের বোতাম → ⋮ (উপরে ডানে) → Allow restricted settings → ফিরে এসে আবার অনুমতি দিন।", 13, false, "#B45309"));
        box.addView(button("App info খুলুন", "#5F6368", v ->
                startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName())))), full());

        box.addView(label("৩. ব্যাটারি সীমা বন্ধ (যাতে ফোন অ্যাপ ঘুম পাড়িয়ে না দেয়)"));
        box.addView(button("ব্যাটারি সীমা বন্ধ করুন", "#FBBC05", v -> {
            Intent i = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + getPackageName()));
            try { startActivity(i); } catch (Exception e) { startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)); }
        }), full());

        box.addView(label("অবস্থা"));
        status = text("", 14, false, "#1F2937");
        box.addView(status);

        box.addView(button("Working Desk খুলুন", "#EA4335", v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }), full());

        setContentView(sv);
        if (Store.configured(this)) link.setText(Store.endpoint(this) + "?k=••••••");
        refresh();
    }

    @Override protected void onResume() { super.onResume(); refresh(); }

    @Override public void onRequestPermissionsResult(int r, String[] perms, int[] res) {
        super.onRequestPermissionsResult(r, perms, res);
        toast(MainActivity.smsOk(this) ? "অনুমতি পাওয়া গেছে ✅" : "অনুমতি পাওয়া যায়নি — উপরের লেখাটা দেখুন");
        refresh();
    }

    private void refresh() {
        if (status == null) return;
        if (Store.configured(this) && MainActivity.smsOk(this)) Jobs.ensure(this);
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        String s = "লিংক: " + (Store.configured(this) ? "✅ দেওয়া আছে" : "❌ দেওয়া হয়নি")
                + "\nSMS অনুমতি: " + (MainActivity.smsOk(this) ? "✅" : "❌")
                + "\nব্যাটারি সীমা: " + (pm.isIgnoringBatteryOptimizations(getPackageName()) ? "✅ বন্ধ" : "⚠ চালু")
                + "\nমোট পাঠানো: " + Store.p(this).getInt("sent", 0)
                + "\nশেষ পাঠানো: " + Store.get(this, "last_ok")
                + "\nসার্ভার সাড়া: " + Store.get(this, "last_hb")
                + "\n৫ নভেম্বর থেকের SMS: " + Store.get(this, "backfill")
                + "\nশেষ সমস্যা: " + Store.get(this, "last_err");
        status.setText(s);
    }

    private TextView text(String s, int sp, boolean bold, String color) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextSize(sp);
        v.setTextColor(Color.parseColor(color));
        if (bold) v.setTypeface(Typeface.DEFAULT_BOLD);
        v.setPadding(0, dp(4), 0, dp(4));
        return v;
    }

    private TextView label(String s) {
        TextView v = text(s, 15, true, "#1F2937");
        v.setPadding(0, dp(18), 0, dp(4));
        return v;
    }

    private Button button(String s, String color, View.OnClickListener l) {
        Button btn = new Button(this);
        btn.setText(s);
        btn.setAllCaps(false);
        btn.setTextSize(14);
        btn.setTextColor(Color.WHITE);
        btn.setGravity(Gravity.CENTER);
        GradientDrawable g = new GradientDrawable();
        g.setColor(Color.parseColor(color));
        g.setCornerRadius(dp(8));
        btn.setBackground(g);
        btn.setMinHeight(dp(40));
        btn.setOnClickListener(l);
        return btn;
    }

    private LinearLayout.LayoutParams full() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(4), 0, dp(4));
        return lp;
    }

    private LinearLayout.LayoutParams weight() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(0, 0, dp(6), 0);
        return lp;
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }
}
