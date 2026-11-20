package com.prompta.tg.editor;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * UndoRedoManager — EditText এর জন্য undo/redo stack।
 * Android এর built-in undo অনেক সময় কাজ করে না, তাই manual।
 */
public class UndoRedoManager {

    private static final int MAX_HISTORY = 100;

    private static class State {
        final String text;
        final int cursor;
        State(String text, int cursor) {
            this.text   = text;
            this.cursor = cursor;
        }
    }

    private final EditText editor;
    private final Deque<State> undoStack = new ArrayDeque<>();
    private final Deque<State> redoStack = new ArrayDeque<>();

    private boolean isRestoring = false;
    private String lastText = "";

    public UndoRedoManager(EditText editor) {
        this.editor = editor;
        // Push initial state
        undoStack.push(new State("", 0));
        setupWatcher();
    }

    private void setupWatcher() {
        editor.addTextChangedListener(new TextWatcher() {
            String beforeText;

            @Override
            public void beforeTextChanged(CharSequence s, int i, int i1, int i2) {
                if (!isRestoring) beforeText = s.toString();
            }

            @Override
            public void onTextChanged(CharSequence s, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isRestoring) return;
                String current = s.toString();
                if (!current.equals(lastText)) {
                    if (undoStack.size() >= MAX_HISTORY) {
                        // Remove oldest
                        State[] arr = undoStack.toArray(new State[0]);
                        undoStack.clear();
                        for (int x = 0; x < arr.length - 1; x++) undoStack.addLast(arr[x]);
                    }
                    undoStack.push(new State(current, editor.getSelectionStart()));
                    redoStack.clear();
                    lastText = current;
                }
            }
        });
    }

    public void undo() {
        if (undoStack.size() <= 1) return; // Nothing to undo
        State current = undoStack.pop();
        redoStack.push(current);
        State prev = undoStack.peek();
        if (prev != null) restore(prev);
    }

    public void redo() {
        if (redoStack.isEmpty()) return;
        State next = redoStack.pop();
        undoStack.push(next);
        restore(next);
    }

    private void restore(State s) {
        isRestoring = true;
        editor.setText(s.text);
        int cursor = Math.min(s.cursor, s.text.length());
        editor.setSelection(cursor);
        lastText = s.text;
        isRestoring = false;
    }

    public boolean canUndo() { return undoStack.size() > 1; }
    public boolean canRedo() { return !redoStack.isEmpty(); }
}
