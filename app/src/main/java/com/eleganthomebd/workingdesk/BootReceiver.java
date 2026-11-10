package com.eleganthomebd.workingdesk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** After a restart / app update, keep the heartbeat running. */
public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) { Jobs.ensure(c); }
}
