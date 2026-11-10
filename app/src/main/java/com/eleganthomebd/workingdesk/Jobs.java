package com.eleganthomebd.workingdesk;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.concurrent.TimeUnit;

/** Background jobs: send one SMS, heartbeat, one-time catch-up of old SMS. */
public final class Jobs {
    private Jobs() {}

    private static Constraints net() {
        return new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
    }

    static void sendLater(Context c, String from, String msg, long at) {
        Data d = new Data.Builder().putString("from", from).putString("msg", msg).putLong("at", at).build();
        OneTimeWorkRequest w = new OneTimeWorkRequest.Builder(SendWorker.class)
                .setInputData(d).setConstraints(net())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build();
        WorkManager.getInstance(c).enqueue(w);
    }

    /** Heartbeat every 30 min + the one-time catch-up (runs only until it has finished once). */
    static void ensure(Context c) {
        if (!Store.configured(c)) return;
        WorkManager wm = WorkManager.getInstance(c);
        wm.enqueueUniquePeriodicWork("hb", ExistingPeriodicWorkPolicy.KEEP,
                new PeriodicWorkRequest.Builder(HeartbeatWorker.class, 30, TimeUnit.MINUTES).setConstraints(net()).build());
        if (!Store.p(c).getBoolean("backfill_done", false)) {
            wm.enqueueUniqueWork("backfill", ExistingWorkPolicy.KEEP,
                    new OneTimeWorkRequest.Builder(BackfillWorker.class).setConstraints(net())
                            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES).build());
        }
    }

    public static class SendWorker extends Worker {
        public SendWorker(@NonNull Context c, @NonNull WorkerParameters p) { super(c, p); }

        @NonNull @Override public Result doWork() {
            Context c = getApplicationContext();
            String from = getInputData().getString("from"), msg = getInputData().getString("msg");
            int code = Sms.post(c, from, msg, getInputData().getLong("at", System.currentTimeMillis()), false);
            if (code >= 200 && code < 300) { Store.addSent(c); Store.status(c, "last_ok", "SMS পাঠানো হয়েছে"); return Result.success(); }
            if (code == 401) { Store.status(c, "last_err", "চাবি ভুল (401) — সেটআপে আবার লিংক দিন"); return Result.failure(); }
            Store.status(c, "last_err", "পাঠানো যায়নি (" + code + ") — আবার চেষ্টা হবে");
            return getRunAttemptCount() < 20 ? Result.retry() : Result.failure();
        }
    }

    public static class HeartbeatWorker extends Worker {
        public HeartbeatWorker(@NonNull Context c, @NonNull WorkerParameters p) { super(c, p); }

        @NonNull @Override public Result doWork() {
            Context c = getApplicationContext();
            if (!Store.configured(c)) return Result.success();
            int code = Sms.heartbeat(c);
            Updater.check(c);
            if (code >= 200 && code < 300) Store.status(c, "last_hb", "সার্ভার সংযোগ ঠিক আছে");
            else Store.status(c, "last_err", "সার্ভারে সাড়া যায়নি (" + code + ")");
            return Result.success();
        }
    }

    /** Sends the inbox SMS from 5 Nov 2026 onward once (server ignores duplicates). */
    public static class BackfillWorker extends Worker {
        public BackfillWorker(@NonNull Context c, @NonNull WorkerParameters p) { super(c, p); }

        @NonNull @Override public Result doWork() {
            Context c = getApplicationContext();
            if (!Store.configured(c)) return Result.success();
            long from = Store.p(c).getLong("bf_pos", Store.BACKFILL_FROM);
            int sent = 0, skipped = 0;
            try (Cursor q = c.getContentResolver().query(Uri.parse("content://sms/inbox"),
                    new String[]{"address", "body", "date"}, "date >= ?", new String[]{String.valueOf(from)}, "date ASC")) {
                if (q == null) { Store.status(c, "backfill", "SMS পড়া যায়নি — অনুমতি দেখুন"); return Result.retry(); }
                while (q.moveToNext()) {
                    String a = q.getString(0), b = q.getString(1);
                    long at = q.getLong(2);
                    if (Sms.allowed(a, b)) {
                        int code = Sms.post(c, a, b, at, true);
                        if (code == 401) { Store.status(c, "backfill", "চাবি ভুল — থেমে আছে"); return Result.failure(); }
                        if (code < 200 || code >= 300) {
                            Store.p(c).edit().putLong("bf_pos", at).apply();   // resume from here
                            Store.status(c, "backfill", sent + "টা গেছে, বাকিগুলো আবার চেষ্টা হবে");
                            return Result.retry();
                        }
                        sent++;
                    } else skipped++;
                }
            } catch (SecurityException e) {
                Store.status(c, "backfill", "SMS পড়ার অনুমতি নেই");
                return Result.retry();
            }
            Store.p(c).edit().putBoolean("backfill_done", true).apply();
            Store.status(c, "backfill", "শেষ — " + sent + "টা পাঠানো হয়েছে (" + skipped + "টা ব্যক্তিগত/OTP বাদ)");
            return Result.success();
        }
    }
}
