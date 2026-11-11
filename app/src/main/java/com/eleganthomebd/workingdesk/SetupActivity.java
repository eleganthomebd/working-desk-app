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
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** Setup: status banner, 4 numbered steps (each with its own button), live status, About. */
public class SetupActivity extends Activity {
    private static final int REQ = 7;
    private static final String C_TEXT = "#1F2937", C_SUB = "#6B7280", C_OK = "#137333", C_WARN = "#B45309";
    private TextView banner, status, s1, s2, s3, s4;
    private EditText link;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(Color.parseColor("#F6F8FB"));
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(18), dp(24), dp(18), dp(24));
        sv.addView(box);

        // ── হেডার: লোগো + নাম ──
        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.ic_desk_full);
        GradientDrawable lg = new GradientDrawable(); lg.setColor(Color.WHITE); lg.setCornerRadius(dp(12));
        logo.setBackground(lg); logo.setClipToOutline(true);
        head.addView(logo, new LinearLayout.LayoutParams(dp(48), dp(48)));
        LinearLayout ht = new LinearLayout(this); ht.setOrientation(LinearLayout.VERTICAL); ht.setPadding(dp(12), 0, 0, 0);
        ht.addView(text("Working Desk", 20, true, C_TEXT));
        ht.addView(text("Elegant Home BD · Mirror World BD", 12.5f, false, C_SUB));
        head.addView(ht);
        box.addView(head);

        banner = text("", 14, true, C_TEXT);
        banner.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams bl = full(); bl.setMargins(0, dp(16), 0, dp(4));
        box.addView(banner, bl);

        // ── ধাপ ১: লিংক ──
        s1 = step(box, "১. সার্ভারের লিংক");
        link = new EditText(this);
        link.setHint("https://…/sms?k=…");
        link.setTextSize(14);
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

        // ── ধাপ ২: SMS অনুমতি ──
        s2 = step(box, "২. SMS পড়ার অনুমতি");
        box.addView(button("SMS অনুমতি দিন", "#34A853", v ->
                requestPermissions(new String[]{Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS}, REQ)), full());
        box.addView(text("আটকে গেলে (\"App was denied access\"):\n"
                + "ক) নিচের বোতাম চাপুন > Permissions > SMS > Allow চাপুন > Close\n"
                + "খ) পিছনে ফিরে আবার এই অ্যাপের পাতা খুলুন > উপরে ডানে ⋮ > Allow restricted settings\n"
                + "গ) আবার Permissions > SMS > Allow", 13, false, C_WARN));
        box.addView(button("ফোনের Settings-এ Working Desk খুলুন", "#5F6368", v -> openAppInfo()), full());

        // ── ধাপ ৩: ব্যাটারি ──
        s3 = step(box, "৩. ব্যাটারি সীমা বন্ধ");
        box.addView(button("ব্যাটারি সীমা বন্ধ করুন", "#F9AB00", v -> {
            try { startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + getPackageName()))); }
            catch (Exception e) { startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)); }
        }), full());

        // ── ধাপ ৪: অব্যবহৃত হলে অনুমতি কেড়ে না নেওয়া ──
        s4 = step(box, "৪. \"Manage app if unused\" বন্ধ");
        box.addView(text("না করলে কিছুদিন অ্যাপ না খুললে ফোন নিজে SMS অনুমতি তুলে নেয়।", 13, false, C_SUB));
        box.addView(button("Manage app if unused বন্ধ করুন", "#EA4335", v -> {
            if (Build.VERSION.SDK_INT >= 30) {
                try { startActivity(new Intent(Intent.ACTION_AUTO_REVOKE_PERMISSIONS, Uri.parse("package:" + getPackageName()))); return; }
                catch (Exception ignored) { }
            }
            openAppInfo();
        }), full());

        // ── অবস্থা ──
        TextView st = text("অবস্থা", 15, true, C_TEXT); st.setPadding(0, dp(20), 0, dp(4));
        box.addView(st);
        status = text("", 13.5f, false, C_TEXT);
        box.addView(status);

        box.addView(button("Working Desk খুলুন", "#1A73E8", v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }), full());

        // ── About ──
        TextView ab = text("অ্যাপ সম্পর্কে", 15, true, C_TEXT); ab.setPadding(0, dp(22), 0, dp(4));
        box.addView(ab);
        box.addView(text("Working Desk · ভার্সন " + verName() + "\n"
                + "Elegant Home BD ও Mirror World BD-এর Owner-এর নিজের অ্যাপ।\n"
                + "লেনদেনের SMS (bKash, Nagad, Rocket, upay, ব্যাংক, বিল) শুধু কোম্পানির নিজের সার্ভারে যায়। "
                + "OTP, পাসওয়ার্ড আর ব্যক্তিগত SMS কখনো ফোনের বাইরে যায় না।\n"
                + "নতুন ভার্সন: foysal.eleganthomebd.com/app\n"
                + "প্যাকেজ: " + getPackageName(), 12.5f, false, C_SUB));

        setContentView(sv);
        if (Store.configured(this)) link.setText(Store.endpoint(this) + "?k=••••••");
        refresh();
    }

    @Override protected void onResume() { super.onResume(); refresh(); }

    @Override public void onRequestPermissionsResult(int r, String[] perms, int[] res) {
        super.onRequestPermissionsResult(r, perms, res);
        toast(MainActivity.smsOk(this) ? "অনুমতি পাওয়া গেছে ✅" : "অনুমতি আটকে গেছে — নিচের লেখা দেখে Settings থেকে দিন");
        refresh();
    }

    private void openAppInfo() {
        startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName())));
    }

    private boolean unusedOff() {
        if (Build.VERSION.SDK_INT < 30) return true;
        try { return getPackageManager().isAutoRevokeWhitelisted(); } catch (Exception e) { return false; }
    }

    private void refresh() {
        if (status == null) return;
        boolean ok1 = Store.configured(this), ok2 = MainActivity.smsOk(this);
        boolean ok3 = ((PowerManager) getSystemService(Context.POWER_SERVICE)).isIgnoringBatteryOptimizations(getPackageName());
        boolean ok4 = unusedOff();
        if (ok1 && ok2) Jobs.ensure(this);
        mark(s1, ok1); mark(s2, ok2); mark(s3, ok3); mark(s4, ok4);

        StringBuilder left = new StringBuilder();
        if (!ok1) left.append(" লিংক,");
        if (!ok2) left.append(" SMS অনুমতি,");
        if (!ok3) left.append(" ব্যাটারি,");
        if (!ok4) left.append(" Manage app if unused,");
        GradientDrawable g = new GradientDrawable(); g.setCornerRadius(dp(10));
        if (left.length() == 0) {
            banner.setText("✅ সব ঠিক আছে — লেনদেনের SMS নিজে থেকে সার্ভারে যাচ্ছে");
            banner.setTextColor(Color.parseColor(C_OK)); g.setColor(Color.parseColor("#E6F4EA"));
        } else {
            banner.setText("⚠ বাকি আছে:" + left.substring(0, left.length() - 1) + " — নিচের ধাপগুলো করুন");
            banner.setTextColor(Color.parseColor("#92400E")); g.setColor(Color.parseColor("#FEF3C7"));
        }
        banner.setBackground(g);

        status.setText("মোট পাঠানো: " + Store.p(this).getInt("sent", 0)
                + "\nশেষ পাঠানো: " + Store.get(this, "last_ok")
                + "\nসার্ভার সাড়া: " + Store.get(this, "last_hb")
                + "\n৫ নভেম্বর থেকের SMS: " + Store.get(this, "backfill")
                + "\nশেষ সমস্যা: " + Store.get(this, "last_err"));
    }

    private String verName() {
        try { return getPackageManager().getPackageInfo(getPackageName(), 0).versionName; } catch (Exception e) { return "?"; }
    }

    private TextView step(LinearLayout box, String title) {
        TextView v = text(title, 15, true, C_TEXT);
        v.setPadding(0, dp(18), 0, dp(4));
        v.setTag(title);
        box.addView(v);
        return v;
    }

    private void mark(TextView v, boolean ok) {
        v.setText((ok ? "✅ " : "⬜ ") + v.getTag());
        v.setTextColor(Color.parseColor(ok ? C_OK : C_TEXT));
    }

    private TextView text(String s, float sp, boolean bold, String color) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextSize(sp);
        v.setTextColor(Color.parseColor(color));
        v.setLineSpacing(0, 1.15f);
        if (bold) v.setTypeface(Typeface.DEFAULT_BOLD);
        v.setPadding(0, dp(3), 0, dp(3));
        return v;
    }

    private Button button(String s, String color, View.OnClickListener l) {
        Button btn = new Button(this);
        btn.setText(s);
        btn.setAllCaps(false);
        btn.setTextSize(14);
        btn.setTextColor(Color.WHITE);
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
