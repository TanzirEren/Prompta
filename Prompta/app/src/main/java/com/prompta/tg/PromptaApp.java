package com.prompta.tg;

import android.app.Application;

import com.prompta.tg.sync.SyncManager;

public class PromptaApp extends Application {

    private static PromptaApp instance;
    private SyncManager syncManager;

    @Override
    public void onCreate() {
        super.onCreate();
        instance    = this;
        syncManager = new SyncManager(this);
        syncManager.startPeriodicSync();
    }

    public static PromptaApp get() { return instance; }

    public SyncManager getSyncManager() { return syncManager; }
}
