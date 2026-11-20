package com.prompta.tg.editor;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.content.Context;
import android.util.TypedValue;

/**
 * LineNumberWatcher — EditText এর text change শুনে
 * lineNumberPanel এ line numbers update করে।
 */
public class LineNumberWatcher implements TextWatcher {

    private final LinearLayout panel;
    private final Context context;
    private int lastLineCount = 0;

    public LineNumberWatcher(LinearLayout panel, Context context) {
        this.panel   = panel;
        this.context = context;
    }

    @Override
    public void afterTextChanged(Editable s) {
        String text = s.toString();
        int lineCount = text.isEmpty() ? 1 : text.split("\n", -1).length;

        if (lineCount == lastLineCount) return;
        lastLineCount = lineCount;

        panel.removeAllViews();
        for (int i = 1; i <= lineCount; i++) {
            TextView tv = new TextView(context);
            tv.setText(String.valueOf(i));
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
            tv.setTextColor(0xFF9E9EB0); // muted
            tv.setTextAlignment(TextView.TEXT_ALIGNMENT_TEXT_END);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (int) (24 * context.getResources().getDisplayMetrics().density)
            );
            tv.setLayoutParams(lp);
            panel.addView(tv);
        }
    }

    @Override public void beforeTextChanged(CharSequence s, int i, int i1, int i2) {}
    @Override public void onTextChanged(CharSequence s, int i, int i1, int i2) {}
}
