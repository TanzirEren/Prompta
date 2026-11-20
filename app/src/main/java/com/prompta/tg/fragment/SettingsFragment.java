package com.prompta.tg.fragment;

import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.prompta.tg.R;
import com.prompta.tg.storage.PromptRepository;
import com.prompta.tg.sync.SyncManager;
import com.prompta.tg.telegram.TelegramSyncService;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SettingsFragment extends Fragment {

    // Font families
    private static final String[] FONT_NAMES   = {"System Default", "Monospace", "Serif", "Sans-serif"};
    private static final String[] FONT_FAMILIES = {"default", "monospace", "serif", "sans-serif"};

    // Line spacing steps
    private static final float[] SPACING_VALUES = {1.0f, 1.2f, 1.4f, 1.5f, 1.6f, 1.8f, 2.0f};
    private static final String[] SPACING_LABELS = {"1.0×", "1.2×", "1.4×", "1.5×", "1.6×", "1.8×", "2.0×"};

    // Views
    private EditText      etBotToken, etChannelId;
    private TextView      tvFontSize, btnFontIncrease, btnFontDecrease;
    private TextView      tvFontFamily, tvLineSpacing, btnSpacingInc, btnSpacingDec;
    private TextView      tvFontPreview, tvSyncStatus, tvLastSync;
    private FrameLayout   btnSaveTg;
    private LinearLayout  rowFontFamily, rowSyncNow;
    private SwitchCompat  switchAutosave, switchSyntax;
    private View          syncDot;
    private ImageView     icSyncNow;

    private SharedPreferences prefs;

    // State
    private int   fontSize    = 15;
    private int   fontIdx     = 0;   // index into FONT_FAMILIES
    private int   spacingIdx  = 3;   // default 1.5×

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());

        bindViews(view);
        loadSavedSettings();
        setupListeners();
    }

    // ─── Bind ───────────────────────────────────────────────────────────────

    private void bindViews(View v) {
        etBotToken      = v.findViewById(R.id.et_bot_token);
        etChannelId     = v.findViewById(R.id.et_channel_id);
        tvFontSize      = v.findViewById(R.id.tv_font_size);
        btnFontIncrease = v.findViewById(R.id.btn_font_increase);
        btnFontDecrease = v.findViewById(R.id.btn_font_decrease);
        tvFontFamily    = v.findViewById(R.id.tv_font_family);
        tvLineSpacing   = v.findViewById(R.id.tv_line_spacing);
        btnSpacingInc   = v.findViewById(R.id.btn_spacing_inc);
        btnSpacingDec   = v.findViewById(R.id.btn_spacing_dec);
        tvFontPreview   = v.findViewById(R.id.tv_font_preview);
        tvSyncStatus    = v.findViewById(R.id.tv_sync_status);
        tvLastSync      = v.findViewById(R.id.tv_last_sync);
        btnSaveTg       = v.findViewById(R.id.btn_save_tg);
        syncDot         = v.findViewById(R.id.sync_dot);
        icSyncNow       = v.findViewById(R.id.ic_sync_now);
        rowFontFamily   = v.findViewById(R.id.row_font_family);
        rowSyncNow      = v.findViewById(R.id.row_sync_now);
        switchAutosave  = v.findViewById(R.id.switch_autosave);
        switchSyntax    = v.findViewById(R.id.switch_syntax);
    }

    // ─── Load ───────────────────────────────────────────────────────────────

    private void loadSavedSettings() {
        // Telegram
        etBotToken.setText(prefs.getString("bot_token", ""));
        etChannelId.setText(prefs.getString("channel_id", ""));

        // Font size
        fontSize = prefs.getInt("font_size", 15);
        tvFontSize.setText(String.valueOf(fontSize));

        // Font family
        String savedFamily = prefs.getString("font_family", "default");
        fontIdx = 0;
        for (int i = 0; i < FONT_FAMILIES.length; i++) {
            if (FONT_FAMILIES[i].equals(savedFamily)) { fontIdx = i; break; }
        }
        tvFontFamily.setText(FONT_NAMES[fontIdx]);

        // Line spacing
        float savedSpacing = prefs.getFloat("line_spacing", 1.5f);
        spacingIdx = 3;
        for (int i = 0; i < SPACING_VALUES.length; i++) {
            if (Math.abs(SPACING_VALUES[i] - savedSpacing) < 0.05f) { spacingIdx = i; break; }
        }
        tvLineSpacing.setText(SPACING_LABELS[spacingIdx]);

        // Toggles
        switchAutosave.setChecked(prefs.getBoolean("autosave_enabled", true));
        switchSyntax.setChecked(prefs.getBoolean("syntax_highlight", true));

        // Telegram status
        boolean connected = prefs.getBoolean("tg_connected", false);
        setSyncStatus(connected ? "✓ Connected" : "Not connected",
                connected ? 0xFF2ECC71 : 0xFF888888, false);

        // Last sync
        long lastSync = prefs.getLong("last_sync_time", 0);
        tvLastSync.setText(lastSync == 0 ? "Never" : formatTime(lastSync));

        // Update preview
        updateFontPreview();
    }

    // ─── Listeners ──────────────────────────────────────────────────────────

    private void setupListeners() {

        // Font family picker
        rowFontFamily.setOnClickListener(v -> showFontFamilyDialog());

        // Font size +/-
        btnFontIncrease.setOnClickListener(v -> {
            if (fontSize < 24) {
                fontSize++;
                tvFontSize.setText(String.valueOf(fontSize));
                prefs.edit().putInt("font_size", fontSize).apply();
                updateFontPreview();
            }
        });
        btnFontDecrease.setOnClickListener(v -> {
            if (fontSize > 10) {
                fontSize--;
                tvFontSize.setText(String.valueOf(fontSize));
                prefs.edit().putInt("font_size", fontSize).apply();
                updateFontPreview();
            }
        });

        // Line spacing +/-
        btnSpacingInc.setOnClickListener(v -> {
            if (spacingIdx < SPACING_VALUES.length - 1) {
                spacingIdx++;
                tvLineSpacing.setText(SPACING_LABELS[spacingIdx]);
                prefs.edit().putFloat("line_spacing", SPACING_VALUES[spacingIdx]).apply();
                updateFontPreview();
            }
        });
        btnSpacingDec.setOnClickListener(v -> {
            if (spacingIdx > 0) {
                spacingIdx--;
                tvLineSpacing.setText(SPACING_LABELS[spacingIdx]);
                prefs.edit().putFloat("line_spacing", SPACING_VALUES[spacingIdx]).apply();
                updateFontPreview();
            }
        });

        // Autosave toggle
        switchAutosave.setOnCheckedChangeListener((btn, checked) ->
                prefs.edit().putBoolean("autosave_enabled", checked).apply());

        // Syntax highlight toggle
        switchSyntax.setOnCheckedChangeListener((btn, checked) ->
                prefs.edit().putBoolean("syntax_highlight", checked).apply());

        // Save Telegram settings & test
        btnSaveTg.setOnClickListener(v -> saveTelegramSettings());

        // Sync now
        rowSyncNow.setOnClickListener(v -> triggerSyncNow());
    }

    // ─── Font Family Dialog ─────────────────────────────────────────────────

    private void showFontFamilyDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Choose Font")
                .setSingleChoiceItems(FONT_NAMES, fontIdx, (dialog, which) -> {
                    fontIdx = which;
                    tvFontFamily.setText(FONT_NAMES[fontIdx]);
                    prefs.edit().putString("font_family", FONT_FAMILIES[fontIdx]).apply();
                    updateFontPreview();
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ─── Preview ────────────────────────────────────────────────────────────

    private void updateFontPreview() {
        tvFontPreview.setTextSize(fontSize);
        tvFontPreview.setLineSpacing(0, SPACING_VALUES[spacingIdx]);

        switch (FONT_FAMILIES[fontIdx]) {
            case "monospace":  tvFontPreview.setTypeface(Typeface.MONOSPACE);    break;
            case "serif":      tvFontPreview.setTypeface(Typeface.SERIF);        break;
            case "sans-serif": tvFontPreview.setTypeface(Typeface.SANS_SERIF);   break;
            default:           tvFontPreview.setTypeface(Typeface.DEFAULT);      break;
        }
    }

    // ─── Telegram ───────────────────────────────────────────────────────────

    private void saveTelegramSettings() {
        String token     = etBotToken.getText().toString().trim();
        String channelId = etChannelId.getText().toString().trim();

        if (token.isEmpty() || channelId.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill both fields", Toast.LENGTH_SHORT).show();
            return;
        }

        prefs.edit()
             .putString("bot_token", token)
             .putString("channel_id", channelId)
             .putBoolean("tg_connected", false)
             .apply();

        setSyncStatus("Testing…", 0xFFD4AC0D, false);

        TelegramSyncService tg = new TelegramSyncService(
                prefs, new PromptRepository(requireContext()));

        tg.testConnection(new TelegramSyncService.SyncCallback() {
            @Override
            public void onSuccess(long msgId) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    prefs.edit().putBoolean("tg_connected", true).apply();
                    setSyncStatus("✓ Connected", 0xFF2ECC71, true);
                    Toast.makeText(requireContext(), "Bot connected!", Toast.LENGTH_SHORT).show();
                });
            }
            @Override
            public void onError(String error) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    prefs.edit().putBoolean("tg_connected", false).apply();
                    setSyncStatus("✗ " + truncate(error, 40), 0xFFE74C3C, false);
                    Toast.makeText(requireContext(), "Error: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    // ─── Sync Now ───────────────────────────────────────────────────────────

    private void triggerSyncNow() {
        icSyncNow.animate().rotation(360f).setDuration(600)
                .withEndAction(() -> icSyncNow.setRotation(0f)).start();

        SyncManager.syncNow(requireContext());

        long now = System.currentTimeMillis();
        prefs.edit().putLong("last_sync_time", now).apply();
        tvLastSync.setText(formatTime(now));

        Toast.makeText(requireContext(), "Sync triggered", Toast.LENGTH_SHORT).show();
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    private void setSyncStatus(String msg, int dotColor, boolean pulse) {
        tvSyncStatus.setText(msg);
        syncDot.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(dotColor));
        if (pulse) {
            syncDot.animate().scaleX(1.4f).scaleY(1.4f).setDuration(350)
                    .withEndAction(() ->
                            syncDot.animate().scaleX(1f).scaleY(1f).setDuration(350).start())
                    .start();
        }
    }

    private String formatTime(long millis) {
        return new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                .format(new Date(millis));
    }

    private String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }
}
