package com.example.wtf2;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private Button notesTab;
    private Button foldersTab;
    private View toggleIndicator;
    private EditText searchInput;
    private ImageButton filterButton;
    private ImageButton menuButton;
    private static final int TAB_NOTES = 0;
    private static final int TAB_FOLDERS = 1;
    private int currentTab = TAB_NOTES;
    private ExecutorService executorService;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Инициализация элементов интерфейса
        FloatingActionButton fab = findViewById(R.id.add_btn);
        notesTab = findViewById(R.id.notesTab);
        foldersTab = findViewById(R.id.foldersTab);
        toggleIndicator = findViewById(R.id.toggleIndicator);
        searchInput = findViewById(R.id.searchInput);
        filterButton = findViewById(R.id.imageButton);
        menuButton = findViewById(R.id.menuButton);

        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        // Начальная загрузка фрагмента "Все"
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new NotesListFragment())
                    .commit();
        }

        // Обработчик кнопки FAB
        fab.setOnClickListener(v -> {
            if (currentTab == TAB_NOTES) {
                Intent intent = new Intent(MainActivity.this, NoteEditor.class);
                startActivity(intent);
            } else if (currentTab == TAB_FOLDERS) {
                showPopupDialog();
            }
        });

        // Обработчики переключения вкладок
        notesTab.setOnClickListener(v -> {
            if (currentTab != TAB_NOTES) {
                switchTab(TAB_NOTES);
            }
        });

        foldersTab.setOnClickListener(v -> {
            if (currentTab != TAB_FOLDERS) {
                switchTab(TAB_FOLDERS);
            }
        });

        // Инициализация переключателя
        updateTabSelection(TAB_NOTES);
        animateToggle(TAB_NOTES);

        // Настройка поиска
        setupSearch();

        // Обработчик кнопки меню
        menuButton.setOnClickListener(v -> Toast.makeText(this, "Menu clicked", Toast.LENGTH_SHORT).show());
    }

    private void switchTab(int tab) {
        currentTab = tab;
        Fragment selectedFragment = (tab == TAB_NOTES) ? new NotesListFragment() : new FoldersListFragment();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, selectedFragment)
                .commit();
        animateToggle(tab);
        updateToolbarButtonsVisibility();
    }

    private void animateToggle(int selectedTab) {
        int startX = selectedTab == TAB_NOTES ? foldersTab.getWidth() : 0;
        int endX = selectedTab == TAB_NOTES ? 0 : foldersTab.getWidth();
        ValueAnimator animator = ValueAnimator.ofInt(startX, endX);
        animator.setDuration(200);
        animator.addUpdateListener(animation -> {
            int value = (int) animation.getAnimatedValue();
            toggleIndicator.setTranslationX(value);
        });
        animator.start();
        updateTabSelection(selectedTab);
    }

    private void updateTabSelection(int selectedTab) {
        notesTab.setSelected(selectedTab == TAB_NOTES);
        foldersTab.setSelected(selectedTab == TAB_FOLDERS);
    }

    private void setupSearch() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().toLowerCase();
                NotesListFragment notesFragment = (NotesListFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.fragment_container);
                if (notesFragment != null && currentTab == TAB_NOTES) {
                    notesFragment.filterNotes(query);
                }
                if (s.length() > 0) {
                    Drawable drawable = getResources().getDrawable(R.drawable.ic_clear_modern, null);
                    int size = dpToPx(8);
                    drawable.setBounds(0, 0, size, size);
                    searchInput.setCompoundDrawablesWithIntrinsicBounds(null, null, drawable, null);
                    searchInput.setCompoundDrawablePadding(dpToPx(8));
                } else {
                    searchInput.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        searchInput.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                Drawable drawable = searchInput.getCompoundDrawables()[2];
                if (drawable != null && event.getRawX() >= (searchInput.getRight() - drawable.getBounds().width() - dpToPx(8))) {
                    searchInput.setText("");
                    return true;
                }
            }
            return false;
        });

        filterButton.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Sort Notes")
                    .setItems(new String[]{
                            "Date Created (Asc)", "Date Created (Desc)",
                            "Date Modified (Asc)", "Date Modified (Desc)",
                            "Title (Asc)", "Title (Desc)"
                    }, (dialog, which) -> {
                        NotesListFragment notesFragment = (NotesListFragment) getSupportFragmentManager()
                                .findFragmentById(R.id.fragment_container);
                        if (notesFragment != null && currentTab == TAB_NOTES) {
                            notesFragment.sortNotes(which);
                        }
                    })
                    .show();
        });
    }

    private void updateToolbarButtonsVisibility() {
        if (currentTab == TAB_FOLDERS) {
            searchInput.setVisibility(View.GONE);
            filterButton.setVisibility(View.GONE);
        } else {
            searchInput.setVisibility(View.VISIBLE);
            filterButton.setVisibility(View.VISIBLE);
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void showPopupDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Создать новую папку");
        builder.setMessage("Введите название новой папки:");

        EditText input = new EditText(this);
        builder.setView(input);

        builder.setPositiveButton("Создать", (dialog, which) -> {
            String folderName = input.getText().toString().trim();
            if (folderName.isEmpty()) {
                Toast.makeText(this, "Название не может быть пустым", Toast.LENGTH_SHORT).show();
                return;
            }
            FoldersListFragment foldersFragment = (FoldersListFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.fragment_container);
            if (foldersFragment != null) {
                foldersFragment.addNewFolder(folderName);
            } else {
                switchToFoldersListFragment(folderName);
            }
        });

        builder.setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void switchToFoldersListFragment(String folderName) {
        currentTab = TAB_FOLDERS;
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new FoldersListFragment())
                .commit();
        animateToggle(TAB_FOLDERS);
        updateToolbarButtonsVisibility();

        new Handler().postDelayed(() -> {
            FoldersListFragment foldersFragment = (FoldersListFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.fragment_container);
            if (foldersFragment != null) {
                foldersFragment.addNewFolder(folderName);
            }
        }, 300);
    }
}