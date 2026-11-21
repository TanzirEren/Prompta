package com.prompta.tg;

import android.app.Application;
import android.util.Log;

import com.prompta.tg.sync.SyncManager;

public class PromptaApp extends Application {

    private static final String TAG = "PromptaApp";

    private static PromptaApp instance;
    private SyncManager syncManager;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // WorkManager init is handled by androidx.startup via Manifest provider.
        // We just schedule periodic sync here with a safety guard.
        try {
            syncManager = new SyncManager(this);
            syncManager.startPeriodicSync();
        } catch (Exception e) {
            Log.e(TAG, "SyncManager init failed: " + e.getMessage(), e);
        }
    }

    public static PromptaApp get() { return instance; }

    public SyncManager getSyncManager() { return syncManager; }
}
