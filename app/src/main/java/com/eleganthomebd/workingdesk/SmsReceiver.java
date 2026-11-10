package com.eleganthomebd.workingdesk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;

/** New SMS → (if allowed) queued for the server; long SMS parts are joined first. */
public class SmsReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) {
        if (!Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(i.getAction()) || !Store.configured(c)) return;
        SmsMessage[] parts = Telephony.Sms.Intents.getMessagesFromIntent(i);
        if (parts == null || parts.length == 0) return;
        StringBuilder body = new StringBuilder();
        String from = parts[0].getDisplayOriginatingAddress();
        long at = parts[0].getTimestampMillis();
        for (SmsMessage m : parts) if (m != null && m.getMessageBody() != null) body.append(m.getMessageBody());
        String msg = body.toString();
        if (Sms.allowed(from, msg)) Jobs.sendLater(c, from, msg, at > 0 ? at : System.currentTimeMillis());
    }
}
