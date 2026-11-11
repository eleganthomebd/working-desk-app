package com.eleganthomebd.workingdesk;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import com.google.androidbrowserhelper.trusted.TwaLauncher;

/** App icon → (setup if needed) → Working Desk opens full-screen in Chrome (push + fingerprint keep working). */
public class MainActivity extends Activity {
    private TwaLauncher twa;
    private boolean launched;

    static boolean smsOk(Activity a) {
        return a.checkSelfPermission(Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
            && a.checkSelfPermission(Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (!Store.configured(this) || !smsOk(this)) {
            startActivity(new Intent(this, SetupActivity.class));
            finish();
            return;
        }
        Jobs.ensure(this);
        new Thread(() -> { Updater.check(getApplicationContext()); Jobs.flush(getApplicationContext(), 60000); }).start();   // update check + মিস হওয়া SMS এখনই
        if (Updater.pending(this)) { Updater.show(this); return; }
        // 🔔 Android 13+: নোটিফিকেশন অ্যাপের নামে আসার জন্য অ্যাপের নিজের অনুমতি লাগে — না থাকলে একবার চাই, তারপর ডেস্ক খোলে
        if (android.os.Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                && !Store.p(this).getBoolean("ntf_asked", false)) {
            Store.p(this).edit().putBoolean("ntf_asked", true).apply();
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 7);
            return;
        }
        openDesk(getIntent());
    }

    @Override public void onRequestPermissionsResult(int code, String[] perms, int[] res) {
        super.onRequestPermissionsResult(code, perms, res);
        if (code == 7 && !launched) openDesk(getIntent());
    }

    /** Opens the desk — the exact page if a foysal. link brought us here. */
    private void openDesk(Intent i) {
        Uri u = i != null ? i.getData() : null;
        if (u == null || !"foysal.eleganthomebd.com".equals(u.getHost())) u = Uri.parse(Store.DESK_URL);
        if (twa != null) twa.destroy();
        twa = new TwaLauncher(this);
        twa.launch(u);
        launched = true;
    }

    @Override protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        if (Store.configured(this) && smsOk(this) && i != null && i.getData() != null) openDesk(i);
    }

    @Override protected void onRestart() {
        super.onRestart();
        if (launched) finish();   // came back from the desk → close the launcher screen
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (twa != null) twa.destroy();
    }
}
