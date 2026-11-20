package com.prompta.tg.storage;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.prompta.tg.model.Prompt;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Local storage for prompts using SharedPreferences + Gson.
 * Phase 3 এ Telegram sync layer এর সাথে সংযুক্ত হবে।
 */
public class PromptRepository {

    private static final String PREFS_NAME = "prompta_db";
    private static final String KEY_PROMPTS = "prompts";

    private final SharedPreferences prefs;
    private final Gson gson = new Gson();

    public PromptRepository(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // ── Read ────────────────────────────────────────────────────────────────

    public List<Prompt> getAll() {
        String json = prefs.getString(KEY_PROMPTS, "[]");
        Type type = new TypeToken<List<Prompt>>() {}.getType();
        List<Prompt> list = gson.fromJson(json, type);
        if (list == null) list = new ArrayList<>();
        // Sort by updatedAt descending
        list.sort((a, b) -> Long.compare(b.getUpdatedAt(), a.getUpdatedAt()));
        return list;
    }

    public Prompt getById(String id) {
        for (Prompt p : getAll()) {
            if (p.getId().equals(id)) return p;
        }
        return null;
    }

    // ── Write ───────────────────────────────────────────────────────────────

    public Prompt save(Prompt prompt) {
        List<Prompt> all = getAll();
        if (prompt.getId() == null || prompt.getId().isEmpty()) {
            // New prompt
            prompt.setId(UUID.randomUUID().toString());
            prompt.setCreatedAt(System.currentTimeMillis());
        }
        prompt.setUpdatedAt(System.currentTimeMillis());
        prompt.setSynced(false); // Mark as pending sync

        // Replace or add
        boolean found = false;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getId().equals(prompt.getId())) {
                all.set(i, prompt);
                found = true;
                break;
            }
        }
        if (!found) all.add(0, prompt);

        persist(all);
        return prompt;
    }

    public void delete(String id) {
        List<Prompt> all = getAll();
        all.removeIf(p -> p.getId().equals(id));
        persist(all);
    }

    public void markSynced(String id, long telegramMsgId) {
        Prompt p = getById(id);
        if (p != null) {
            p.setTelegramMsgId(telegramMsgId);
            p.setSynced(true);
            save(p);
        }
    }

    // ── Internal ────────────────────────────────────────────────────────────

    private void persist(List<Prompt> list) {
        prefs.edit().putString(KEY_PROMPTS, gson.toJson(list)).apply();
    }
}
