package com.prompta.tg.sync;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.prompta.tg.model.Prompt;
import com.prompta.tg.storage.PromptRepository;
import com.prompta.tg.telegram.TelegramSyncService;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * SyncWorker — Background এ unsynced prompts Telegram এ push করে।
 * WorkManager schedule করে periodic বা on-demand run।
 */
public class SyncWorker extends Worker {

    public static final String TAG = "prompta_sync";

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context ctx        = getApplicationContext();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        PromptRepository repo   = new PromptRepository(ctx);
        TelegramSyncService tg  = new TelegramSyncService(prefs, repo);

        if (!tg.isConfigured()) return Result.success(); // Nothing to do

        List<Prompt> all = repo.getAll();
        boolean anyFailed = false;

        for (Prompt p : all) {
            if (p.isSynced()) continue; // Already synced

            // Synchronous wait (Worker runs on background thread)
            CountDownLatch latch   = new CountDownLatch(1);
            AtomicBoolean  success = new AtomicBoolean(false);

            tg.editPrompt(p, new TelegramSyncService.SyncCallback() {
                @Override public void onSuccess(long msgId) {
                    success.set(true);
                    latch.countDown();
                }
                @Override public void onError(String error) {
                    latch.countDown();
                }
            });

            try { latch.await(10, java.util.concurrent.TimeUnit.SECONDS); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }

            if (!success.get()) anyFailed = true;
        }

        return anyFailed ? Result.retry() : Result.success();
    }
}
