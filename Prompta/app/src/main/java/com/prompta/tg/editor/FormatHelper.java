package com.prompta.tg.editor;

import android.widget.EditText;

/**
 * FormatHelper — সব markdown formatting operations handle করে।
 * wrap/toggle pattern ব্যবহার করে।
 */
public class FormatHelper {

    private final EditText editor;

    public FormatHelper(EditText editor) {
        this.editor = editor;
    }

    // ── Inline wrappers ──────────────────────────────────────────────────────

    public void bold()          { wrapSelection("**", "**"); }
    public void italic()        { wrapSelection("*", "*"); }
    public void strikethrough() { wrapSelection("~~", "~~"); }
    public void inlineCode()    { wrapSelection("`", "`"); }
    public void highlight()     { wrapSelection("==", "=="); }

    // ── Headings ─────────────────────────────────────────────────────────────

    public void heading(int level) {
        String prefix = "#".repeat(level) + " ";
        linePrefix(prefix);
    }

    // ── Block level ──────────────────────────────────────────────────────────

    public void bulletList()    { linePrefix("- "); }
    public void numberedList()  { linePrefix("1. "); }
    public void checklist()     { linePrefix("- [ ] "); }
    public void quote()         { linePrefix("> "); }

    public void codeBlock() {
        int start = editor.getSelectionStart();
        int end   = editor.getSelectionEnd();
        String selected = editor.getText().subSequence(start, end).toString();
        String wrapped = "\n```\n" + selected + "\n```\n";
        editor.getText().replace(start, end, wrapped);
        editor.setSelection(start + 5, start + 5 + selected.length());
    }

    public void horizontalRule() {
        insertAtNewLine("\n---\n");
    }

    public void table() {
        String tbl = "\n| Header 1 | Header 2 | Header 3 |\n" +
                     "| -------- | -------- | -------- |\n" +
                     "| Cell     | Cell     | Cell     |\n";
        insertAtNewLine(tbl);
    }

    // ── Link ─────────────────────────────────────────────────────────────────

    public void insertLink(String text, String url) {
        int start = editor.getSelectionStart();
        int end   = editor.getSelectionEnd();
        String formatted = "[" + text + "](" + url + ")";
        editor.getText().replace(start, end, formatted);
        editor.setSelection(start + formatted.length());
    }

    // ── Image ─────────────────────────────────────────────────────────────────

    public void insertImage(String alt, String url) {
        int start = editor.getSelectionStart();
        String formatted = "![" + alt + "](" + url + ")";
        editor.getText().insert(start, formatted);
        editor.setSelection(start + formatted.length());
    }

    // ── Markdown export ───────────────────────────────────────────────────────

    /** Wraps currently selected text with prefix and suffix */
    private void wrapSelection(String prefix, String suffix) {
        int start = editor.getSelectionStart();
        int end   = editor.getSelectionEnd();
        if (start < 0) start = editor.length();
        if (end < start) end = start;

        String selected = editor.getText().subSequence(start, end).toString();

        // Toggle: if already wrapped, unwrap
        if (selected.startsWith(prefix) && selected.endsWith(suffix)) {
            String inner = selected.substring(prefix.length(),
                    selected.length() - suffix.length());
            editor.getText().replace(start, end, inner);
            editor.setSelection(start, start + inner.length());
        } else {
            String wrapped = prefix + selected + suffix;
            editor.getText().replace(start, end, wrapped);
            editor.setSelection(start + prefix.length(),
                    start + prefix.length() + selected.length());
        }
    }

    /** Adds prefix to current line */
    private void linePrefix(String prefix) {
        int cursor = editor.getSelectionStart();
        int lineStart = cursor;
        String text = editor.getText().toString();
        while (lineStart > 0 && text.charAt(lineStart - 1) != '\n') lineStart--;

        // Toggle: remove if already there
        if (text.startsWith(prefix, lineStart)) {
            editor.getText().delete(lineStart, lineStart + prefix.length());
        } else {
            editor.getText().insert(lineStart, prefix);
            editor.setSelection(cursor + prefix.length());
        }
    }

    private void insertAtNewLine(String text) {
        int cursor = editor.getSelectionStart();
        // Ensure we insert after a newline
        String current = editor.getText().toString();
        if (cursor > 0 && current.charAt(cursor - 1) != '\n') {
            editor.getText().insert(cursor, "\n");
            cursor++;
        }
        editor.getText().insert(cursor, text);
        editor.setSelection(cursor + text.length());
    }

    /** Word count from raw text */
    public static int wordCount(String text) {
        if (text == null || text.trim().isEmpty()) return 0;
        return text.trim().split("\\s+").length;
    }

    /** Estimated read time (200 wpm) */
    public static int readTimeMinutes(String text) {
        int wc = wordCount(text);
        return Math.max(1, Math.round(wc / 200f));
    }
}
