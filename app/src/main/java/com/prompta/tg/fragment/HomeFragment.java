package com.prompta.tg.fragment;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.prompta.tg.MainActivity;
import com.prompta.tg.R;
import com.prompta.tg.adapter.PromptAdapter;
import com.prompta.tg.model.Prompt;
import com.prompta.tg.storage.PromptRepository;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView rvPrompts;
    private LinearLayout emptyState;
    private ImageView btnSearch;
    private FrameLayout searchContainer;
    private EditText etSearch;
    private View tvCancelSearch;

    private PromptAdapter adapter;
    private PromptRepository repository;
    private List<Prompt> allPrompts = new ArrayList<>();

    private boolean searchVisible = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvPrompts       = view.findViewById(R.id.rv_prompts);
        emptyState      = view.findViewById(R.id.empty_state);
        btnSearch       = view.findViewById(R.id.btn_search);
        searchContainer = view.findViewById(R.id.search_container);
        etSearch        = view.findViewById(R.id.et_search);
        tvCancelSearch  = view.findViewById(R.id.btn_cancel_search);

        repository = new PromptRepository(requireContext());

        setupRecyclerView();
        setupSearch();
        loadPrompts();
    }

    private void setupRecyclerView() {
        adapter = new PromptAdapter(allPrompts, promptId -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openEditor(promptId);
            }
        });
        rvPrompts.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvPrompts.setAdapter(adapter);
        // Smooth scroll & item animator
        rvPrompts.setItemAnimator(new androidx.recyclerview.widget.DefaultItemAnimator());
    }

    private void setupSearch() {
        btnSearch.setOnClickListener(v -> toggleSearch(true));
        tvCancelSearch.setOnClickListener(v -> toggleSearch(false));

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int i1, int i2) {}
            @Override public void onTextChanged(CharSequence s, int i, int i1, int i2) {
                filterPrompts(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void toggleSearch(boolean show) {
        searchVisible = show;

        if (show) {
            searchContainer.setVisibility(View.VISIBLE);
            searchContainer.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(280)
                    .start();
            // Animate height from 0 to 56dp
            ViewGroup.LayoutParams lp = searchContainer.getLayoutParams();
            lp.height = dpToPx(56);
            searchContainer.setLayoutParams(lp);
            etSearch.requestFocus();
            btnSearch.animate().alpha(0f).setDuration(200).start();
        } else {
            searchContainer.animate()
                    .alpha(0f)
                    .translationY(-20f)
                    .setDuration(240)
                    .withEndAction(() -> {
                        searchContainer.setVisibility(View.GONE);
                        etSearch.setText("");
                        filterPrompts("");
                    })
                    .start();
            btnSearch.animate().alpha(1f).setDuration(200).start();
        }
    }

    private void filterPrompts(String query) {
        if (query.isEmpty()) {
            adapter.updateList(allPrompts);
            return;
        }
        List<Prompt> filtered = new ArrayList<>();
        String lower = query.toLowerCase();
        for (Prompt p : allPrompts) {
            if ((p.getTitle() != null && p.getTitle().toLowerCase().contains(lower)) ||
                (p.getContent() != null && p.getContent().toLowerCase().contains(lower))) {
                filtered.add(p);
            }
        }
        adapter.updateList(filtered);
    }

    private void loadPrompts() {
        allPrompts = repository.getAll();
        adapter.updateList(allPrompts);
        emptyState.setVisibility(allPrompts.isEmpty() ? View.VISIBLE : View.GONE);
        rvPrompts.setVisibility(allPrompts.isEmpty() ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadPrompts(); // Refresh after returning from editor
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
