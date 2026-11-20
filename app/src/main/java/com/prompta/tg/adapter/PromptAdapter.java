package com.prompta.tg.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.prompta.tg.R;
import com.prompta.tg.model.Prompt;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PromptAdapter extends RecyclerView.Adapter<PromptAdapter.PromptViewHolder> {

    public interface OnPromptClickListener {
        void onClick(String promptId);
    }

    private List<Prompt> prompts;
    private final OnPromptClickListener listener;
    private static final SimpleDateFormat DATE_FMT =
            new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());

    public PromptAdapter(List<Prompt> prompts, OnPromptClickListener listener) {
        this.prompts = new ArrayList<>(prompts);
        this.listener = listener;
    }

    public void updateList(List<Prompt> newList) {
        List<Prompt> oldList = this.prompts;
        this.prompts = new ArrayList<>(newList);

        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override public int getOldListSize() { return oldList.size(); }
            @Override public int getNewListSize() { return newList.size(); }
            @Override public boolean areItemsTheSame(int o, int n) {
                return oldList.get(o).getId().equals(newList.get(n).getId());
            }
            @Override public boolean areContentsTheSame(int o, int n) {
                Prompt op = oldList.get(o), np = newList.get(n);
                return op.getTitle().equals(np.getTitle()) &&
                       op.getUpdatedAt() == np.getUpdatedAt();
            }
        });
        result.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public PromptViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                 .inflate(R.layout.item_prompt_card, parent, false);
        return new PromptViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull PromptViewHolder h, int pos) {
        Prompt p = prompts.get(pos);
        h.tvTitle.setText(p.getTitle() != null && !p.getTitle().isEmpty()
                ? p.getTitle() : "Untitled");
        h.tvPreview.setText(p.getPreview());
        h.tvDate.setText(DATE_FMT.format(new Date(p.getUpdatedAt())));
        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(p.getId());
        });
    }

    @Override
    public int getItemCount() { return prompts.size(); }

    static class PromptViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvPreview, tvDate;
        PromptViewHolder(@NonNull View v) {
            super(v);
            tvTitle   = v.findViewById(R.id.tv_title);
            tvPreview = v.findViewById(R.id.tv_preview);
            tvDate    = v.findViewById(R.id.tv_date);
        }
    }
}
