package com.prompta.tg.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.prompta.tg.MainActivity;
import com.prompta.tg.PromptaApp;
import com.prompta.tg.R;
import com.prompta.tg.editor.AutoSaveHandler;
import com.prompta.tg.editor.FormatHelper;
import com.prompta.tg.editor.LineNumberWatcher;
import com.prompta.tg.editor.MarkdownStyler;
import com.prompta.tg.editor.UndoRedoManager;
import com.prompta.tg.model.Prompt;
import com.prompta.tg.storage.PromptRepository;
import com.prompta.tg.telegram.TelegramSyncService;

import io.noties.markwon.Markwon;
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin;
import io.noties.markwon.ext.tables.TablePlugin;
import io.noties.markwon.html.HtmlPlugin;

public class EditorFragment extends Fragment {

    // ── Views ────────────────────────────────────────────────────────────────
    private EditText etTitle, etContent;
    private TextView tvAutosave, tvWordCount, tvReadTime;
    private ImageView btnBack, btnPreview, btnMore;
    private ScrollView editorScroll, previewScroll;
    private TextView tvPreviewText;
    private LinearLayout lineNumberPanel;
    private CardView selectionToolbar, linkDialog;
    private EditText etLinkText, etLinkUrl;

    // Format toolbar buttons
    private TextView fmtH1, fmtH2, fmtH3, fmtH4, fmtH5, fmtH6;
    private TextView fmtBold, fmtItalic, fmtStrike, fmtCodeInline;
    private ImageView fmtHighlight, fmtLink, fmtImage;
    private ImageView fmtCodeBlock, fmtBullet, fmtNumbered, fmtChecklist;
    private ImageView fmtQuote, fmtTable, fmtHr, fmtLineNumbers;
    private ImageView fmtUndo, fmtRedo;

    // Selection toolbar
    private ImageView selBold, selItalic, selHighlight, selLink;
    private ImageView selCode, selCodeBlock, selImage, selH1, selMd;

    // ── Logic ────────────────────────────────────────────────────────────────
    private FormatHelper        formatHelper;
    private AutoSaveHandler     autoSave;
    private UndoRedoManager     undoRedo;
    private PromptRepository    repository;
    private Markwon             markwon;
    private MarkdownStyler      mdStyler;
    private TelegramSyncService telegramSync;

    private Prompt   currentPrompt;
    private String   promptId;
    private boolean  previewMode     = false;
    private boolean  lineNumVisible  = false;
    private boolean  isLoading       = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            promptId = getArguments().getString("prompt_id");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_editor, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindViews(view);
        setupDependencies();
        applyFontSize();
        loadPrompt();
        setupToolbar();
        setupSelectionToolbar();
        setupEditorWatchers();
        setupLinkDialog();
        setupTopBar();
    }

    // ────────────────────────────────────────────────────────────────────────
    // Setup
    // ────────────────────────────────────────────────────────────────────────

    private void bindViews(View v) {
        etTitle          = v.findViewById(R.id.et_title);
        etContent        = v.findViewById(R.id.et_content);
        tvAutosave       = v.findViewById(R.id.tv_autosave);
        tvWordCount      = v.findViewById(R.id.tv_word_count);
        tvReadTime       = v.findViewById(R.id.tv_read_time);
        btnBack          = v.findViewById(R.id.btn_back);
        btnPreview       = v.findViewById(R.id.btn_preview);
        btnMore          = v.findViewById(R.id.btn_more);
        editorScroll     = v.findViewById(R.id.editor_scroll);
        previewScroll    = v.findViewById(R.id.preview_scroll);
        tvPreviewText    = v.findViewById(R.id.tv_preview);
        lineNumberPanel  = v.findViewById(R.id.line_number_panel);
        selectionToolbar = v.findViewById(R.id.selection_toolbar);
        linkDialog       = v.findViewById(R.id.link_dialog);
        etLinkText       = v.findViewById(R.id.et_link_text);
        etLinkUrl        = v.findViewById(R.id.et_link_url);

        // Format toolbar
        fmtH1          = v.findViewById(R.id.fmt_h1);
        fmtH2          = v.findViewById(R.id.fmt_h2);
        fmtH3          = v.findViewById(R.id.fmt_h3);
        fmtH4          = v.findViewById(R.id.fmt_h4);
        fmtH5          = v.findViewById(R.id.fmt_h5);
        fmtH6          = v.findViewById(R.id.fmt_h6);
        fmtBold        = v.findViewById(R.id.fmt_bold);
        fmtItalic      = v.findViewById(R.id.fmt_italic);
        fmtStrike      = v.findViewById(R.id.fmt_strike);
        fmtHighlight   = v.findViewById(R.id.fmt_highlight);
        fmtLink        = v.findViewById(R.id.fmt_link);
        fmtImage       = v.findViewById(R.id.fmt_image);
        fmtCodeInline  = v.findViewById(R.id.fmt_code_inline);
        fmtCodeBlock   = v.findViewById(R.id.fmt_code_block);
        fmtBullet      = v.findViewById(R.id.fmt_bullet);
        fmtNumbered    = v.findViewById(R.id.fmt_numbered);
        fmtChecklist   = v.findViewById(R.id.fmt_checklist);
        fmtQuote       = v.findViewById(R.id.fmt_quote);
        fmtTable       = v.findViewById(R.id.fmt_table);
        fmtHr          = v.findViewById(R.id.fmt_hr);
        fmtLineNumbers = v.findViewById(R.id.fmt_line_numbers);
        fmtUndo        = v.findViewById(R.id.fmt_undo);
        fmtRedo        = v.findViewById(R.id.fmt_redo);

        // Selection toolbar
        selBold      = v.findViewById(R.id.sel_bold);
        selItalic    = v.findViewById(R.id.sel_italic);
        selHighlight = v.findViewById(R.id.sel_highlight);
        selLink      = v.findViewById(R.id.sel_link);
        selCode      = v.findViewById(R.id.sel_code);
        selCodeBlock = v.findViewById(R.id.sel_code_block);
        selImage     = v.findViewById(R.id.sel_image);
        selH1        = v.findViewById(R.id.sel_h1);
        selMd        = v.findViewById(R.id.sel_md);
    }

    private void setupDependencies() {
        repository   = new PromptRepository(requireContext());
        formatHelper = new FormatHelper(etContent);
        undoRedo     = new UndoRedoManager(etContent);
        markwon      = Markwon.builder(requireContext())
                .usePlugin(StrikethroughPlugin.create())
                .usePlugin(TablePlugin.create(requireContext()))
                .usePlugin(HtmlPlugin.create())
                .build();
        autoSave     = new AutoSaveHandler(this::performSave);
        mdStyler     = new MarkdownStyler(requireContext());
        telegramSync = new TelegramSyncService(
                androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext()),
                repository
        );
    }

    private void applyFontSize() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        int size = prefs.getInt("font_size", 15);
        etContent.setTextSize(size);
        tvPreviewText.setTextSize(size);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Prompt load/save
    // ────────────────────────────────────────────────────────────────────────

    private void loadPrompt() {
        isLoading = true;
        if (promptId != null) {
            currentPrompt = repository.getById(promptId);
            if (currentPrompt != null) {
                etTitle.setText(currentPrompt.getTitle());
                etContent.setText(currentPrompt.getContent());
                etContent.setSelection(etContent.length());
            }
        }
        if (currentPrompt == null) {
            currentPrompt = new Prompt(null, "", "");
        }
        updateStats();
        isLoading = false;
    }

    private void performSave() {
        currentPrompt.setTitle(etTitle.getText().toString().trim());
        currentPrompt.setContent(etContent.getText().toString());
        currentPrompt = repository.save(currentPrompt);
        promptId = currentPrompt.getId();
        showAutoSaveTick();

        // Telegram sync — background এ send/edit করো
        final Prompt promptToSync = currentPrompt;
        telegramSync.editPrompt(promptToSync, new TelegramSyncService.SyncCallback() {
            @Override
            public void onSuccess(long msgId) {
                // Sync done — SyncManager trigger করার দরকার নেই
            }
            @Override
            public void onError(String error) {
                // Offline বা error — SyncWorker পরে retry করবে
                PromptaApp.get().getSyncManager().syncNow();
            }
        });
    }

    private void showAutoSaveTick() {
        tvAutosave.setText("✓ Saved");
        tvAutosave.animate().alpha(1f).setDuration(200).withEndAction(() ->
            tvAutosave.animate().alpha(0f).setDuration(1500).setStartDelay(1000).start()
        ).start();
    }

    // ────────────────────────────────────────────────────────────────────────
    // Top bar
    // ────────────────────────────────────────────────────────────────────────

    private void setupTopBar() {
        btnBack.setOnClickListener(v -> {
            autoSave.saveNow();
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).closeEditor();
            }
        });

        btnPreview.setOnClickListener(v -> togglePreview());

        btnMore.setOnClickListener(v -> showMoreMenu());

        etTitle.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int i1, int i2) {}
            @Override public void onTextChanged(CharSequence s, int i, int i1, int i2) {}
            @Override public void afterTextChanged(Editable s) {
                if (!isLoading) autoSave.onContentChanged();
            }
        });
    }

    // ────────────────────────────────────────────────────────────────────────
    // Preview toggle (Feature 1)
    // ────────────────────────────────────────────────────────────────────────

    private void togglePreview() {
        previewMode = !previewMode;
        if (previewMode) {
            // Render markdown
            markwon.setMarkdown(tvPreviewText, etContent.getText().toString());
            editorScroll.animate().alpha(0f).setDuration(200).withEndAction(() -> {
                editorScroll.setVisibility(View.GONE);
                previewScroll.setVisibility(View.VISIBLE);
                previewScroll.setAlpha(0f);
                previewScroll.animate().alpha(1f).setDuration(200).start();
            }).start();
            btnPreview.setImageResource(R.drawable.ic_arrow_back); // repurpose as "edit" icon
        } else {
            previewScroll.animate().alpha(0f).setDuration(200).withEndAction(() -> {
                previewScroll.setVisibility(View.GONE);
                editorScroll.setVisibility(View.VISIBLE);
                editorScroll.setAlpha(0f);
                editorScroll.animate().alpha(1f).setDuration(200).start();
            }).start();
            btnPreview.setImageResource(R.drawable.ic_preview);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Format Toolbar (Features 2–22)
    // ────────────────────────────────────────────────────────────────────────

    private void setupToolbar() {
        // Headings
        fmtH1.setOnClickListener(v -> formatHelper.heading(1));
        fmtH2.setOnClickListener(v -> formatHelper.heading(2));
        fmtH3.setOnClickListener(v -> formatHelper.heading(3));
        fmtH4.setOnClickListener(v -> formatHelper.heading(4));
        fmtH5.setOnClickListener(v -> formatHelper.heading(5));
        fmtH6.setOnClickListener(v -> formatHelper.heading(6));

        // Inline
        fmtBold.setOnClickListener(v        -> formatHelper.bold());
        fmtItalic.setOnClickListener(v      -> formatHelper.italic());
        // Strikethrough visual on the button (paintFlags must be set in Java, not XML)
        fmtStrike.setPaintFlags(fmtStrike.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        fmtStrike.setOnClickListener(v      -> formatHelper.strikethrough());
        fmtHighlight.setOnClickListener(v   -> formatHelper.highlight());
        fmtCodeInline.setOnClickListener(v  -> formatHelper.inlineCode());

        // Link & Image
        fmtLink.setOnClickListener(v        -> showLinkDialog());
        fmtImage.setOnClickListener(v       -> pickImage());

        // Blocks
        fmtCodeBlock.setOnClickListener(v   -> formatHelper.codeBlock());
        fmtBullet.setOnClickListener(v      -> formatHelper.bulletList());
        fmtNumbered.setOnClickListener(v    -> formatHelper.numberedList());
        fmtChecklist.setOnClickListener(v   -> formatHelper.checklist());
        fmtQuote.setOnClickListener(v       -> formatHelper.quote());
        fmtTable.setOnClickListener(v       -> formatHelper.table());
        fmtHr.setOnClickListener(v          -> formatHelper.horizontalRule());

        // Line numbers toggle (Feature — line numbers enable/disable)
        fmtLineNumbers.setOnClickListener(v -> toggleLineNumbers());

        // Undo / Redo
        fmtUndo.setOnClickListener(v -> undoRedo.undo());
        fmtRedo.setOnClickListener(v -> undoRedo.redo());
    }

    // ────────────────────────────────────────────────────────────────────────
    // Editor text watchers
    // ────────────────────────────────────────────────────────────────────────

    private void setupEditorWatchers() {
        etContent.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int i1, int i2) {}
            @Override public void onTextChanged(CharSequence s, int i, int i1, int i2) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (isLoading) return;
                updateStats();
                autoSave.onContentChanged();
                // Live markdown syntax highlight (debounced via post)
                etContent.post(() -> {
                    if (etContent.getText() instanceof android.text.SpannableStringBuilder) {
                        mdStyler.apply((android.text.SpannableStringBuilder) etContent.getText());
                    }
                });
            }
        });

        // Line number watcher
        etContent.addTextChangedListener(new LineNumberWatcher(lineNumberPanel, requireContext()));

        // Selection change → show/hide floating toolbar
        etContent.setCustomSelectionActionModeCallback(new ActionMode.Callback() {
            @Override public boolean onCreateActionMode(ActionMode m, Menu menu) {
                showSelectionToolbar();
                return true;
            }
            @Override public boolean onPrepareActionMode(ActionMode m, Menu menu) { return false; }
            @Override public boolean onActionItemClicked(ActionMode m, MenuItem item) { return false; }
            @Override public void onDestroyActionMode(ActionMode m) {
                hideSelectionToolbar();
            }
        });
    }

    private void updateStats() {
        String text = etContent.getText().toString();
        int words = FormatHelper.wordCount(text);
        int mins  = FormatHelper.readTimeMinutes(text);
        tvWordCount.setText(words + " word" + (words == 1 ? "" : "s"));
        tvReadTime.setText("~" + mins + " min read");
    }

    // ────────────────────────────────────────────────────────────────────────
    // Selection floating toolbar (Feature — text selection actions)
    // ────────────────────────────────────────────────────────────────────────

    private void setupSelectionToolbar() {
        selBold.setOnClickListener(v      -> { formatHelper.bold();          hideSelectionToolbar(); });
        selItalic.setOnClickListener(v    -> { formatHelper.italic();        hideSelectionToolbar(); });
        selHighlight.setOnClickListener(v -> { formatHelper.highlight();     hideSelectionToolbar(); });
        selLink.setOnClickListener(v      -> { showLinkDialog();             hideSelectionToolbar(); });
        selCode.setOnClickListener(v      -> { formatHelper.inlineCode();    hideSelectionToolbar(); });
        selCodeBlock.setOnClickListener(v -> { formatHelper.codeBlock();     hideSelectionToolbar(); });
        selImage.setOnClickListener(v     -> { pickImage();                  hideSelectionToolbar(); });
        selH1.setOnClickListener(v        -> { formatHelper.heading(1);      hideSelectionToolbar(); });
        selMd.setOnClickListener(v        -> { togglePreview();              hideSelectionToolbar(); });
    }

    private void showSelectionToolbar() {
        selectionToolbar.setVisibility(View.VISIBLE);
        selectionToolbar.setAlpha(0f);
        selectionToolbar.setScaleX(0.85f);
        selectionToolbar.setScaleY(0.85f);
        selectionToolbar.animate()
                .alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(200).start();
    }

    private void hideSelectionToolbar() {
        selectionToolbar.animate()
                .alpha(0f).scaleX(0.9f).scaleY(0.9f)
                .setDuration(150)
                .withEndAction(() -> selectionToolbar.setVisibility(View.GONE))
                .start();
    }

    // ────────────────────────────────────────────────────────────────────────
    // Link dialog (Feature — link insert)
    // ────────────────────────────────────────────────────────────────────────

    private void setupLinkDialog() {
        // Pre-fill link text from selection
        linkDialog.setVisibility(View.GONE);

        view(R.id.btn_link_cancel).setOnClickListener(v -> hideLinkDialog());

        view(R.id.btn_link_insert).setOnClickListener(v -> {
            String text = etLinkText.getText().toString().trim();
            String url  = etLinkUrl.getText().toString().trim();
            if (url.isEmpty()) {
                Toast.makeText(requireContext(), "URL required", Toast.LENGTH_SHORT).show();
                return;
            }
            if (text.isEmpty()) text = url;
            formatHelper.insertLink(text, url);
            hideLinkDialog();
        });
    }

    private void showLinkDialog() {
        // Pre-fill selection as text
        int start = etContent.getSelectionStart();
        int end   = etContent.getSelectionEnd();
        if (start != end) {
            etLinkText.setText(etContent.getText().subSequence(start, end));
        }
        etLinkUrl.setText("");
        linkDialog.setVisibility(View.VISIBLE);
        linkDialog.setTranslationY(200f);
        linkDialog.animate().translationY(0f).setDuration(280).start();
        etLinkUrl.requestFocus();
    }

    private void hideLinkDialog() {
        linkDialog.animate().translationY(300f).setDuration(220)
                .withEndAction(() -> linkDialog.setVisibility(View.GONE)).start();
    }

    // ────────────────────────────────────────────────────────────────────────
    // Image picker (Feature — image with drag reposition)
    // Phase 3 এ full image drag আসবে; এখন URI-based insert
    // ────────────────────────────────────────────────────────────────────────

    private void pickImage() {
        // TODO Phase 3: ActivityResult launcher → pick from gallery → upload/insert
        Toast.makeText(requireContext(),
                "Image picker — Phase 3 এ আসবে", Toast.LENGTH_SHORT).show();
    }

    // ────────────────────────────────────────────────────────────────────────
    // Line numbers toggle (Feature)
    // ────────────────────────────────────────────────────────────────────────

    private void toggleLineNumbers() {
        lineNumVisible = !lineNumVisible;
        lineNumberPanel.setVisibility(lineNumVisible ? View.VISIBLE : View.GONE);
        fmtLineNumbers.setAlpha(lineNumVisible ? 1f : 0.4f);
        // Force redraw
        if (lineNumVisible) {
            etContent.getText().append(""); // triggers watcher
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // More menu
    // ────────────────────────────────────────────────────────────────────────

    private void showMoreMenu() {
        // Simple popup items
        android.widget.PopupMenu popup = new android.widget.PopupMenu(
                requireContext(), btnMore);
        popup.getMenu().add(0, 1, 0, "Word count");
        popup.getMenu().add(0, 2, 0, "Copy as Markdown");
        popup.getMenu().add(0, 3, 0, "Delete prompt");
        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1:
                    String text = etContent.getText().toString();
                    Toast.makeText(requireContext(),
                            FormatHelper.wordCount(text) + " words · " +
                            FormatHelper.readTimeMinutes(text) + " min read",
                            Toast.LENGTH_LONG).show();
                    return true;
                case 2:
                    android.content.ClipboardManager cm = (android.content.ClipboardManager)
                            requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                    cm.setPrimaryClip(android.content.ClipData.newPlainText(
                            "markdown", etContent.getText().toString()));
                    Toast.makeText(requireContext(), "Copied!", Toast.LENGTH_SHORT).show();
                    return true;
                case 3:
                    deletePrompt();
                    return true;
            }
            return false;
        });
        popup.show();
    }

    private void deletePrompt() {
        if (currentPrompt.getId() == null) {
            if (getActivity() instanceof MainActivity)
                ((MainActivity) getActivity()).closeEditor();
            return;
        }
        // TG message delete করো, তারপর local delete
        telegramSync.deletePrompt(currentPrompt, new TelegramSyncService.SyncCallback() {
            @Override
            public void onSuccess(long msgId) {
                repository.delete(currentPrompt.getId());
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (getActivity() instanceof MainActivity)
                            ((MainActivity) getActivity()).closeEditor();
                    });
                }
            }
            @Override
            public void onError(String error) {
                repository.delete(currentPrompt.getId());
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (getActivity() instanceof MainActivity)
                            ((MainActivity) getActivity()).closeEditor();
                    });
                }
            }
        });
    }

    // ────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ────────────────────────────────────────────────────────────────────────

    @Override
    public void onPause() {
        super.onPause();
        autoSave.saveNow();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        autoSave.cancel();
    }

    // ────────────────────────────────────────────────────────────────────────
    // Utility
    // ────────────────────────────────────────────────────────────────────────

    /** Convenience: find view by id from current view */
    private View view(int id) {
        return requireView().findViewById(id);
    }
}
