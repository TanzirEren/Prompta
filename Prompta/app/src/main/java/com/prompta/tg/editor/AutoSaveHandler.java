package com.prompta.tg.editor;

import android.os.Handler;
import android.os.Looper;

/**
 * AutoSaveHandler — debounce 2s, তারপর callback fire করে।
 * EditText text change হলে এটা call করতে হবে।
 */
public class AutoSaveHandler {

    public interface SaveCallback {
        void onSave();
    }

    private static final long DEBOUNCE_MS = 2000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final SaveCallback callback;
    private final Runnable saveRunnable;

    private boolean pending = false;

    public AutoSaveHandler(SaveCallback callback) {
        this.callback = callback;
        this.saveRunnable = () -> {
            pending = false;
            callback.onSave();
        };
    }

    /** Call এটা যখনই content change হয় */
    public void onContentChanged() {
        pending = true;
        handler.removeCallbacks(saveRunnable);
        handler.postDelayed(saveRunnable, DEBOUNCE_MS);
    }

    /** Force immediate save */
    public void saveNow() {
        handler.removeCallbacks(saveRunnable);
        pending = false;
        callback.onSave();
    }

    public boolean hasPendingChange() {
        return pending;
    }

    public void cancel() {
        handler.removeCallbacks(saveRunnable);
        pending = false;
    }
}
