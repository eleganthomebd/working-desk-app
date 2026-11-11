package com.eleganthomebd.workingdesk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;

/**
 * New SMS → (if allowed) sent to the server AT ONCE (v1.4, 11 Nov 2026: "super fast").
 * If that fails it is queued (WorkManager), and the 15-minute sweep catches anything still missed.
 */
public class SmsReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) {
        if (!Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(i.getAction()) || !Store.configured(c)) return;
        SmsMessage[] parts = Telephony.Sms.Intents.getMessagesFromIntent(i);
        if (parts == null || parts.length == 0) return;
        StringBuilder body = new StringBuilder();
        String from = parts[0].getDisplayOriginatingAddress();
        long t = parts[0].getTimestampMillis();
        for (SmsMessage m : parts) if (m != null && m.getMessageBody() != null) body.append(m.getMessageBody());
        final String msg = body.toString();
        if (!Sms.allowed(from, msg)) return;
        final long at = t > 0 ? t : System.currentTimeMillis();
        final Context app = c.getApplicationContext();
        final PendingResult pr = goAsync();   // keeps the app alive ~10 s for the direct send
        new Thread(() -> {
            try {
                int code = Sms.post(app, from, msg, at, false);
                if (code >= 200 && code < 300) {
                    Store.addSent(app);
                    Store.markSent(app, from, msg);
                    Store.status(app, "last_ok", "SMS পাঠানো হয়েছে");
                } else {
                    Jobs.sendLater(app, from, msg, at);   // retry in the background
                }
            } catch (Exception e) {
                Jobs.sendLater(app, from, msg, at);
            } finally {
                pr.finish();
            }
        }).start();
    }
}
