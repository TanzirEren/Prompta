package com.prompta.tg.editor;

import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MarkdownStyler — EditText এ লেখার সময় live syntax highlight দেয়।
 * এটা একটা lightweight highlighter (Markwon editor plugin এর পাশে কাজ করে)।
 */
public class MarkdownStyler {

    // Heading পাওয়ার regex
    private static final Pattern HEADING = Pattern.compile("^(#{1,6})\\s(.+)$", Pattern.MULTILINE);
    // Bold **text**
    private static final Pattern BOLD    = Pattern.compile("\\*\\*(.+?)\\*\\*");
    // Italic *text*
    private static final Pattern ITALIC  = Pattern.compile("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)");
    // Inline code `code`
    private static final Pattern CODE    = Pattern.compile("`([^`]+)`");
    // Highlight ==text==
    private static final Pattern HIGHLIGHT = Pattern.compile("==(.+?)==");

    private final int colorHeading;
    private final int colorCode;
    private final int colorHighlight;
    private final int colorMuted;

    public MarkdownStyler(Context ctx) {
        // Off-white bg এ obsidian-based colors
        colorHeading   = 0xFF1A1A2E;
        colorCode      = 0xFF3D3D5C;
        colorHighlight = 0x44FFDD44; // translucent yellow
        colorMuted     = 0xFF6B6B85;
    }

    /**
     * Apply spans on a SpannableStringBuilder.
     * editText.addTextChangedListener এ afterTextChanged থেকে call করুন।
     * Note: এটা expensive for large text; debounce করা উচিত।
     */
    public void apply(SpannableStringBuilder sb) {
        clearSpans(sb);

        // Headings
        Matcher m = HEADING.matcher(sb);
        while (m.find()) {
            int hashes = m.group(1).length();
            float scale = 1.6f - (hashes - 1) * 0.1f; // H1=1.6, H6=1.1
            sb.setSpan(new RelativeSizeSpan(scale), m.start(), m.end(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.setSpan(new StyleSpan(Typeface.BOLD), m.start(), m.end(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.setSpan(new ForegroundColorSpan(colorHeading), m.start(), m.end(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        // Bold
        m = BOLD.matcher(sb);
        while (m.find()) {
            sb.setSpan(new StyleSpan(Typeface.BOLD), m.start(), m.end(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        // Italic
        m = ITALIC.matcher(sb);
        while (m.find()) {
            sb.setSpan(new StyleSpan(Typeface.ITALIC), m.start(), m.end(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        // Inline code
        m = CODE.matcher(sb);
        while (m.find()) {
            sb.setSpan(new TypefaceSpan("monospace"), m.start(), m.end(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.setSpan(new ForegroundColorSpan(colorCode), m.start(), m.end(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.setSpan(new BackgroundColorSpan(0x15000000), m.start(), m.end(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        // Highlight
        m = HIGHLIGHT.matcher(sb);
        while (m.find()) {
            sb.setSpan(new BackgroundColorSpan(colorHighlight), m.start(), m.end(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private void clearSpans(SpannableStringBuilder sb) {
        // Remove old custom spans
        Object[] spans = sb.getSpans(0, sb.length(), Object.class);
        for (Object span : spans) {
            if (span instanceof RelativeSizeSpan ||
                span instanceof StyleSpan ||
                span instanceof TypefaceSpan ||
                span instanceof BackgroundColorSpan ||
                span instanceof ForegroundColorSpan) {
                sb.removeSpan(span);
            }
        }
    }
}
