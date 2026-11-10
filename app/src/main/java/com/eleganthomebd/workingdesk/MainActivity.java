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
        new Thread(() -> Updater.check(getApplicationContext())).start();   // for next launch
        if (Updater.pending(this)) { Updater.show(this); return; }
        twa = new TwaLauncher(this);
        twa.launch(Uri.parse(Store.DESK_URL));
        launched = true;
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
