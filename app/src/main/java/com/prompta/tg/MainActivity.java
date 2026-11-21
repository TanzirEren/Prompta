package com.prompta.tg;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.prompta.tg.fragment.EditorFragment;
import com.prompta.tg.fragment.HomeFragment;
import com.prompta.tg.fragment.SettingsFragment;

public class MainActivity extends AppCompatActivity {

    private LinearLayout navHome, navSettings;
    private FrameLayout navNew;
    private ImageView icHome, icSettings;
    private TextView tvHome, tvSettings;

    private static final int TAB_HOME     = 0;
    private static final int TAB_SETTINGS = 2;
    private int currentTab = TAB_HOME;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // MUST be called BEFORE super.onCreate() — splash screen install
        SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);

        // Edge-to-edge rendering
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        setContentView(R.layout.activity_main);

        initViews();
        applyInsets();
        setupNavigation();

        if (savedInstanceState == null) {
            loadFragment(new HomeFragment(), false);
        }
    }

    private void initViews() {
        navHome     = findViewById(R.id.nav_home);
        navNew      = findViewById(R.id.nav_new);
        navSettings = findViewById(R.id.nav_settings);
        icHome      = findViewById(R.id.ic_home);
        icSettings  = findViewById(R.id.ic_settings);
        tvHome      = findViewById(R.id.tv_home);
        tvSettings  = findViewById(R.id.tv_settings);
    }

    private void applyInsets() {
        // Bottom nav: pad by system navigation bar height
        View navCard = findViewById(R.id.nav_card);
        ViewCompat.setOnApplyWindowInsetsListener(navCard, (v, windowInsets) -> {
            Insets sys = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, 0, 0, sys.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void setupNavigation() {
        navHome.setOnClickListener(v -> {
            if (currentTab != TAB_HOME) {
                currentTab = TAB_HOME;
                loadFragment(new HomeFragment(), false);
                updateNavUI();
            }
        });

        navNew.setOnClickListener(v -> openEditor(null));

        navSettings.setOnClickListener(v -> {
            if (currentTab != TAB_SETTINGS) {
                currentTab = TAB_SETTINGS;
                loadFragment(new SettingsFragment(), false);
                updateNavUI();
            }
        });
    }

    private void loadFragment(Fragment fragment, boolean addToBack) {
        loadFragment(fragment, addToBack, false);
    }

    private void loadFragment(Fragment fragment, boolean addToBack, boolean isEditor) {
        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        if (isEditor) {
            tx.setCustomAnimations(
                    R.anim.slide_up,
                    R.anim.fade_scale_out,
                    R.anim.fade_scale_in,
                    R.anim.slide_down
            );
        } else {
            tx.setCustomAnimations(
                    android.R.anim.fade_in,
                    android.R.anim.fade_out,
                    android.R.anim.fade_in,
                    android.R.anim.fade_out
            );
        }
        tx.replace(R.id.fragment_container, fragment);
        if (addToBack) tx.addToBackStack(null);
        tx.commit();
    }

    public void openEditor(String promptId) {
        EditorFragment editor = new EditorFragment();
        if (promptId != null) {
            Bundle args = new Bundle();
            args.putString("prompt_id", promptId);
            editor.setArguments(args);
        }
        loadFragment(editor, true, true);
        setNavVisible(false);
    }

    public void closeEditor() {
        getSupportFragmentManager().popBackStack();
        setNavVisible(true);
        updateNavUI();
    }

    public void setNavVisible(boolean visible) {
        View nav = findViewById(R.id.bottom_nav_container);
        if (nav != null) {
            nav.animate()
               .alpha(visible ? 1f : 0f)
               .translationY(visible ? 0f : 200f)
               .setDuration(260)
               .withEndAction(() -> nav.setVisibility(visible ? View.VISIBLE : View.GONE))
               .start();
        }
    }

    private void updateNavUI() {
        icHome.setAlpha(currentTab == TAB_HOME ? 1f : 0.4f);
        tvHome.setAlpha(currentTab == TAB_HOME ? 1f : 0.4f);
        icSettings.setAlpha(currentTab == TAB_SETTINGS ? 1f : 0.4f);
        tvSettings.setAlpha(currentTab == TAB_SETTINGS ? 1f : 0.4f);
    }
}
