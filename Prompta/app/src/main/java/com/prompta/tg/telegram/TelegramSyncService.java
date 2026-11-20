package com.prompta.tg.telegram;

import android.content.SharedPreferences;

import com.prompta.tg.model.Prompt;
import com.prompta.tg.storage.PromptRepository;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * TelegramSyncService — Prompt এর add / edit / delete
 * Telegram channel এ sync করে।
 *
 * Flow:
 *   save prompt → format text → sendMessage → store messageId
 *   edit prompt → formatText → editMessageText (messageId দিয়ে)
 *   delete prompt → deleteMessage → local delete
 */
public class TelegramSyncService {

    public interface SyncCallback {
        void onSuccess(long messageId);
        void onError(String error);
    }

    private final String token;
    private final String channelId;
    private final PromptRepository repository;

    public TelegramSyncService(SharedPreferences prefs, PromptRepository repository) {
        this.token      = prefs.getString("bot_token", "");
        this.channelId  = prefs.getString("channel_id", "");
        this.repository = repository;
    }

    public boolean isConfigured() {
        return !token.isEmpty() && !channelId.isEmpty();
    }

    // ── Send new prompt ────────────────────────────────────────────────────

    public void sendPrompt(Prompt prompt, SyncCallback cb) {
        if (!isConfigured()) { cb.onError("Telegram not configured"); return; }

        String text = formatPrompt(prompt);
        SendMessageRequest req = new SendMessageRequest(channelId, text);

        TelegramClient.get().sendMessage(token, req).enqueue(new Callback<TelegramResponse>() {
            @Override
            public void onResponse(Call<TelegramResponse> call, Response<TelegramResponse> resp) {
                if (resp.isSuccessful() && resp.body() != null && resp.body().ok) {
                    long msgId = resp.body().result.messageId;
                    repository.markSynced(prompt.getId(), msgId);
                    cb.onSuccess(msgId);
                } else {
                    String err = resp.body() != null ? resp.body().description : "Unknown error";
                    cb.onError(err);
                }
            }
            @Override
            public void onFailure(Call<TelegramResponse> call, Throwable t) {
                cb.onError(t.getMessage());
            }
        });
    }

    // ── Edit existing prompt ───────────────────────────────────────────────

    public void editPrompt(Prompt prompt, SyncCallback cb) {
        if (!isConfigured()) { cb.onError("Telegram not configured"); return; }
        if (prompt.getTelegramMsgId() == 0) {
            // Never sent before → send as new
            sendPrompt(prompt, cb);
            return;
        }

        String text = formatPrompt(prompt);
        EditMessageRequest req = new EditMessageRequest(channelId, prompt.getTelegramMsgId(), text);

        TelegramClient.get().editMessage(token, req).enqueue(new Callback<TelegramResponse>() {
            @Override
            public void onResponse(Call<TelegramResponse> call, Response<TelegramResponse> resp) {
                if (resp.isSuccessful() && resp.body() != null && resp.body().ok) {
                    repository.markSynced(prompt.getId(), prompt.getTelegramMsgId());
                    cb.onSuccess(prompt.getTelegramMsgId());
                } else {
                    String err = resp.body() != null ? resp.body().description : "Unknown error";
                    // If message not found (deleted from channel), re-send
                    if (err != null && err.contains("message to edit not found")) {
                        sendPrompt(prompt, cb);
                    } else {
                        cb.onError(err);
                    }
                }
            }
            @Override
            public void onFailure(Call<TelegramResponse> call, Throwable t) {
                cb.onError(t.getMessage());
            }
        });
    }

    // ── Delete prompt ──────────────────────────────────────────────────────

    public void deletePrompt(Prompt prompt, SyncCallback cb) {
        if (!isConfigured() || prompt.getTelegramMsgId() == 0) {
            cb.onSuccess(0); // Local-only delete, still OK
            return;
        }

        DeleteMessageRequest req = new DeleteMessageRequest(channelId, prompt.getTelegramMsgId());

        TelegramClient.get().deleteMessage(token, req).enqueue(new Callback<TelegramResponse>() {
            @Override
            public void onResponse(Call<TelegramResponse> call, Response<TelegramResponse> resp) {
                // Even if TG delete fails, we delete locally
                cb.onSuccess(0);
            }
            @Override
            public void onFailure(Call<TelegramResponse> call, Throwable t) {
                cb.onSuccess(0); // Still delete locally
            }
        });
    }

    // ── Test connection ────────────────────────────────────────────────────

    public void testConnection(SyncCallback cb) {
        if (token.isEmpty()) { cb.onError("Bot token is empty"); return; }

        TelegramClient.get().getMe(token).enqueue(new Callback<TelegramResponse>() {
            @Override
            public void onResponse(Call<TelegramResponse> call, Response<TelegramResponse> resp) {
                if (resp.isSuccessful() && resp.body() != null && resp.body().ok) {
                    cb.onSuccess(1);
                } else {
                    cb.onError("Invalid token");
                }
            }
            @Override
            public void onFailure(Call<TelegramResponse> call, Throwable t) {
                cb.onError("No internet: " + t.getMessage());
            }
        });
    }

    // ── Format ────────────────────────────────────────────────────────────

    /**
     * Prompt কে Telegram message format এ convert করে।
     * Channel এ এভাবে দেখাবে:
     *
     * 📌 *Title*
     * ──────────
     * content...
     * ──────────
     * 🆔 promptId | 🕐 date
     */
    private String formatPrompt(Prompt prompt) {
        StringBuilder sb = new StringBuilder();

        // Title
        String title = prompt.getTitle();
        if (title == null || title.isEmpty()) title = "Untitled";
        sb.append("📌 *").append(escapeMarkdown(title)).append("*\n");
        sb.append("──────────────\n");

        // Content (max 3800 chars — Telegram limit is 4096)
        String content = prompt.getContent();
        if (content == null) content = "";
        if (content.length() > 3800) {
            content = content.substring(0, 3800) + "\n…*(truncated)*";
        }
        sb.append(content).append("\n");
        sb.append("──────────────\n");

        // Metadata footer
        sb.append("🆔 `").append(prompt.getId(), 0, Math.min(8, prompt.getId().length())).append("…`");

        return sb.toString();
    }

    /** Telegram Markdown special chars escape */
    private String escapeMarkdown(String text) {
        return text
                .replace("_", "\\_")
                .replace("*", "\\*")
                .replace("[", "\\[")
                .replace("`", "\\`");
    }
}
