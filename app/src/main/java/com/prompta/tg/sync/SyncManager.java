package com.prompta.tg.sync;

import android.content.Context;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

/**
 * SyncManager — WorkManager schedule করে।
 * App start হলে periodic sync চালু করে।
 * Prompt save/edit/delete হলে immediate sync trigger করে।
 */
public class SyncManager {

    private static final String PERIODIC_WORK = "prompta_periodic_sync";
    private static final String IMMEDIATE_WORK = "prompta_immediate_sync";

    private final WorkManager workManager;

    public SyncManager(Context ctx) {
        this.workManager = WorkManager.getInstance(ctx);
    }

    /** App launch এ call করুন — 15 min periodic sync */
    public void startPeriodicSync() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest periodic = new PeriodicWorkRequest.Builder(
                SyncWorker.class, 15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .addTag(SyncWorker.TAG)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build();

        workManager.enqueueUniquePeriodicWork(
                PERIODIC_WORK,
                ExistingPeriodicWorkPolicy.KEEP,
                periodic
        );
    }

    /** Prompt save/edit/delete হলে সাথে সাথে sync */
    public void syncNow() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest immediate = new OneTimeWorkRequest.Builder(SyncWorker.class)
                .setConstraints(constraints)
                .addTag(SyncWorker.TAG)
                .build();

        workManager.enqueueUniqueWork(
                IMMEDIATE_WORK,
                ExistingWorkPolicy.REPLACE,
                immediate
        );
    }

    /** Sync বন্ধ করুন */
    public void cancelAll() {
        workManager.cancelAllWorkByTag(SyncWorker.TAG);
    }

    /** Static convenience — Context থেকে সরাসরি call করুন */
    public static void syncNow(Context ctx) {
        new SyncManager(ctx).syncNow();
    }

    /** Static convenience — Context থেকে সরাসরি periodic sync চালু করুন */
    public static void startPeriodicSync(Context ctx) {
        new SyncManager(ctx).startPeriodicSync();
    }
}
