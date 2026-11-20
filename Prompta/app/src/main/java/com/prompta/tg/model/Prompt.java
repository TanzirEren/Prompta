package com.prompta.tg.model;

import java.io.Serializable;

public class Prompt implements Serializable {

    private String id;           // Unique local ID (UUID)
    private String title;        // Prompt title
    private String content;      // Raw content (Markdown/HTML)
    private long createdAt;      // Unix timestamp
    private long updatedAt;
    private long telegramMsgId;  // Message ID in TG channel (0 = not synced)
    private boolean synced;

    public Prompt() {}

    public Prompt(String id, String title, String content) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.telegramMsgId = 0;
        this.synced = false;
    }

    // ── Getters & Setters ───────────────────────────────────────────────────

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public long getTelegramMsgId() { return telegramMsgId; }
    public void setTelegramMsgId(long telegramMsgId) { this.telegramMsgId = telegramMsgId; }

    public boolean isSynced() { return synced; }
    public void setSynced(boolean synced) { this.synced = synced; }

    /** Short preview text (first 120 chars, stripped of markdown) */
    public String getPreview() {
        if (content == null || content.isEmpty()) return "";
        // Strip basic markdown symbols
        String plain = content
                .replaceAll("#{1,6}\\s", "")
                .replaceAll("\\*{1,2}([^*]+)\\*{1,2}", "$1")
                .replaceAll("`[^`]+`", "")
                .replaceAll("\\n", " ")
                .trim();
        return plain.length() > 120 ? plain.substring(0, 120) + "…" : plain;
    }
}
