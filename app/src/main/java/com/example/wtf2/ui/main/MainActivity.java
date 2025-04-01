package com.example.wtf2.ui.main;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.wtf2.ColorSpinnerAdapter;
import com.example.wtf2.R;
import com.example.wtf2.data.AppDatabase;
import com.example.wtf2.data.model.Folder;
import com.example.wtf2.data.model.Note;
import com.example.wtf2.ui.note.NoteEditActivity;
import com.example.wtf2.viewmodel.MainViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private MainViewModel viewModel;
    private Button notesTab;
    private Button foldersTab;
    private View toggleIndicator;
    private EditText searchInput;
    private ImageButton filterButton;
    private LinearLayout tabsContainer;
    private LinearLayout selectionPanel;
    private LinearLayout selectionHeader;
    private ImageButton selectionBackButton;
    private TextView selectionCount;
    private ImageButton actionPinButton;
    private ImageButton actionEditMoveButton;
    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        Log.d("MainActivity", "Initializing MainActivity");

        FloatingActionButton fab = findViewById(R.id.add_btn);
        notesTab = findViewById(R.id.notesTab);
        foldersTab = findViewById(R.id.foldersTab);
        toggleIndicator = findViewById(R.id.toggleIndicator);
        ImageButton backButton = findViewById(R.id.back_button_navigation);
        searchInput = findViewById(R.id.searchInput);
        filterButton = findViewById(R.id.imageButton);
        tabsContainer = findViewById(R.id.tabsContainer);
        selectionPanel = findViewById(R.id.selection_panel);
        selectionHeader = findViewById(R.id.selection_header);
        selectionBackButton = findViewById(R.id.selection_back_button);
        selectionCount = findViewById(R.id.selection_count);
        actionPinButton = findViewById(R.id.action_pin);
        actionEditMoveButton = findViewById(R.id.action_edit);
        drawerLayout = findViewById(R.id.drawer_layout);

        NavigationView navigationView = findViewById(R.id.nav_view);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        findViewById(R.id.menuButton).setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.END));
        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_notes) {
                viewModel.setCurrentTab(0);
            } else if (itemId == R.id.nav_folders) {
                viewModel.setCurrentTab(1);
            } else if (itemId == R.id.nav_settings) {
                Toast.makeText(this, "Настройки пока не реализованы", Toast.LENGTH_SHORT).show();
            }
            drawerLayout.closeDrawer(GravityCompat.END);
            return true;
        });

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new NotesFragment())
                    .commit();
        }

        new Thread(() -> {
            while (!AppDatabase.isInitialized()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Log.e("MainActivity", "Interrupted while waiting for DB initialization", e);
                }
            }
            runOnUiThread(() -> {
                Log.d("MainActivity", "Database initialized, loading folders and notes");
                viewModel.loadFolders();
                viewModel.loadNotes(null);
                Log.d("MainActivity", "Called loadFolders and loadNotes after DB initialization");
            });
        }).start();

        viewModel.getCurrentTab().observe(this, tab -> {
            Fragment selectedFragment = (tab == 0) ? new NotesFragment() : new FoldersFragment();
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, selectedFragment)
                    .commit();
            animateToggle(tab);
            notesTab.setSelected(tab == 0);
            foldersTab.setSelected(tab == 1);
            notesTab.setTextColor(tab == 0 ? Color.BLACK : Color.GRAY);
            foldersTab.setTextColor(tab == 1 ? Color.BLACK : Color.GRAY);
            boolean isInFolder = viewModel.getIsInFolder().getValue() != null && viewModel.getIsInFolder().getValue();
            boolean shouldShowSearchAndSort = (tab == 0) || (tab == 1 && isInFolder);
            searchInput.setVisibility(shouldShowSearchAndSort ? View.VISIBLE : View.GONE);
            filterButton.setVisibility(shouldShowSearchAndSort ? View.VISIBLE : View.GONE);
            Log.d("MainActivity", "CurrentTab changed: tab=" + tab + ", isInFolder=" + isInFolder + ", searchInput visibility=" + (searchInput.getVisibility() == View.VISIBLE));
        });

        viewModel.getIsInFolder().observe(this, isInFolder -> {
            ImageButton backBtn = findViewById(R.id.back_button_navigation);
            FrameLayout toggleFrame = findViewById(R.id.toggleFrame);
            backBtn.setVisibility(isInFolder ? View.VISIBLE : View.GONE);
            toggleFrame.setVisibility(isInFolder ? View.GONE : View.VISIBLE);
            int tab = viewModel.getCurrentTab().getValue() != null ? viewModel.getCurrentTab().getValue() : 0;
            boolean shouldShowSearchAndSort = (tab == 0) || (tab == 1 && isInFolder);
            searchInput.setVisibility(shouldShowSearchAndSort ? View.VISIBLE : View.GONE);
            filterButton.setVisibility(shouldShowSearchAndSort ? View.VISIBLE : View.GONE);
            Log.d("MainActivity", "IsInFolder changed: isInFolder=" + isInFolder + ", currentTab=" + tab + ", searchInput visibility=" + (searchInput.getVisibility() == View.VISIBLE));
        });

        viewModel.getIsSelectionMode().observe(this, isSelectionMode -> {
            tabsContainer.setVisibility(isSelectionMode ? View.GONE : View.VISIBLE);
            selectionHeader.setVisibility(isSelectionMode ? View.VISIBLE : View.GONE);
            selectionPanel.setVisibility(isSelectionMode ? View.VISIBLE : View.GONE);
        });

        viewModel.getSelectedCount().observe(this, count -> {
            selectionCount.setText("Выбрано: " + count);
            actionEditMoveButton.setImageResource(count > 1 ? android.R.drawable.ic_menu_upload : android.R.drawable.ic_menu_edit);
        });

        // Обновляем логику FAB
        fab.setOnClickListener(v -> {
            if (!viewModel.getIsSelectionMode().getValue()) {
                int currentTab = viewModel.getCurrentTab().getValue();
                boolean isInFolder = viewModel.getIsInFolder().getValue() != null && viewModel.getIsInFolder().getValue();

                if (currentTab == 0 || (currentTab == 1 && isInFolder)) {
                    // Создаём заметку
                    Intent intent = new Intent(this, NoteEditActivity.class);
                    if (isInFolder) {
                        // Передаём текущий folderId, чтобы спиннер выбрал эту папку
                        intent.putExtra("FOLDER_ID", viewModel.getCurrentFolderId().getValue());
                    }
                    startActivity(intent);
                } else {
                    // Создаём папку
                    showFolderDialog(null);
                }
            }
        });

        notesTab.setOnClickListener(v -> {
            if (viewModel.getCurrentTab().getValue() != 0 && !viewModel.getIsSelectionMode().getValue()) {
                viewModel.setCurrentTab(0);
            }
        });

        foldersTab.setOnClickListener(v -> {
            if (viewModel.getCurrentTab().getValue() != 1 && !viewModel.getIsSelectionMode().getValue()) {
                viewModel.setCurrentTab(1);
            }
        });

        backButton.setOnClickListener(v -> handleBackPress());

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().trim();
                viewModel.setSearchQuery(query);
                searchInput.setCompoundDrawablesWithIntrinsicBounds(0, 0, query.isEmpty() ? 0 : R.drawable.ic_clear, 0);
            }
        });

        searchInput.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                Drawable rightDrawable = searchInput.getCompoundDrawables()[2];
                if (rightDrawable != null && event.getRawX() >= (searchInput.getRight() - rightDrawable.getBounds().width())) {
                    searchInput.setText("");
                    return true;
                }
            }
            return false;
        });

        filterButton.setOnClickListener(v -> {
            String[] options = {"Без сортировки", "Дата (возрастание)", "Дата (убывание)", "Заголовок (A-Z)", "Заголовок (Z-A)"};
            new AlertDialog.Builder(this)
                    .setTitle("Сортировка")
                    .setItems(options, (dialog, which) -> viewModel.setSortOrder(which))
                    .setNegativeButton("Отмена", null)
                    .show();
        });

        selectionBackButton.setOnClickListener(v -> viewModel.exitSelectionMode());

        actionPinButton.setOnClickListener(v -> viewModel.pinSelectedItems());
        findViewById(R.id.action_share).setOnClickListener(v -> Toast.makeText(this, "Поделиться пока не реализовано", Toast.LENGTH_SHORT).show());
        findViewById(R.id.action_delete).setOnClickListener(v -> viewModel.deleteSelectedItems());
        actionEditMoveButton.setOnClickListener(v -> {
            if (viewModel.getCurrentTab().getValue() == 0) {
                List<Note> selectedNotes = viewModel.getSelectedNotes();
                if (selectedNotes.size() == 1) {
                    Intent intent = new Intent(this, NoteEditActivity.class);
                    intent.putExtra("NOTE_ID", selectedNotes.get(0).getId());
                    startActivity(intent);
                } else if (selectedNotes.size() > 1) {
                    showMoveNotesDialog();
                }
            } else {
                List<Folder> selectedFolders = viewModel.getSelectedFolders();
                if (selectedFolders.size() == 1) {
                    showFolderDialog(selectedFolders.get(0));
                } else {
                    Toast.makeText(this, "Выберите только одну папку для редактирования", Toast.LENGTH_SHORT).show();
                }
            }
            viewModel.exitSelectionMode();
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackPress();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        int currentTab = viewModel.getCurrentTab().getValue() != null ? viewModel.getCurrentTab().getValue() : 0;
        boolean isInFolder = viewModel.getIsInFolder().getValue() != null && viewModel.getIsInFolder().getValue();

        if (currentTab == 0) {
            // Вкладка "Заметки"
            if (isInFolder) {
                viewModel.loadNotes(viewModel.getCurrentFolderId().getValue());
            } else {
                viewModel.loadNotes(null);
            }
        } else if (currentTab == 1 && isInFolder) {
            // Вкладка "Папки" и мы внутри папки
            viewModel.loadNotes(viewModel.getCurrentFolderId().getValue());
        }
        // Если мы на вкладке "Папки" и не внутри папки, обновляем список папок
        if (currentTab == 1 && !isInFolder) {
            viewModel.loadFolders();
        }
    }

    private void handleBackPress() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
            Log.d("MainActivity", "Closed drawer on back press");
            return;
        }

        if (viewModel.getIsSelectionMode().getValue()) {
            viewModel.exitSelectionMode();
            Log.d("MainActivity", "Exited selection mode on back press");
            return;
        }

        if (viewModel.getIsInFolder().getValue()) {
            viewModel.setIsInFolder(false);
            Fragment selectedFragment = viewModel.getCurrentTab().getValue() == 0 ? new NotesFragment() : new FoldersFragment();
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, selectedFragment)
                    .commit();
            Log.d("MainActivity", "Exited folder, currentTab=" + viewModel.getCurrentTab().getValue() + ", navigated to " + (viewModel.getCurrentTab().getValue() == 0 ? "NotesFragment" : "FoldersFragment"));
            return;
        }

        if (viewModel.getCurrentTab().getValue() == 1) {
            viewModel.setCurrentTab(0);
            Log.d("MainActivity", "Switched from Folders tab to Notes tab on back press");
            return;
        }

        Log.d("MainActivity", "Finishing activity on back press");
        finish();
    }

    private void animateToggle(int selectedTab) {
        ValueAnimator animator = ValueAnimator.ofFloat(
                toggleIndicator.getTranslationX(),
                selectedTab == 0 ? notesTab.getLeft() : foldersTab.getLeft()
        );
        animator.setDuration(200);
        animator.addUpdateListener(animation -> {
            float translationX = (float) animation.getAnimatedValue();
            toggleIndicator.setTranslationX(translationX);
            toggleIndicator.getLayoutParams().width = selectedTab == 0 ? notesTab.getWidth() : foldersTab.getWidth();
            toggleIndicator.requestLayout();
        });
        animator.start();
    }

    private void showFolderDialog(Folder folder) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_new_folder, null);
        EditText folderNameInput = dialogView.findViewById(R.id.folder_name_input);
        Spinner colorSpinner = dialogView.findViewById(R.id.color_spinner);

        List<String> colorNames = Arrays.asList("Розовый", "Коралловый", "Голубой", "Зеленый", "Желтый", "Белый");
        List<String> colorValues = Arrays.asList("#FFD1DC", "#D4A5A5", "#B9D9EB", "#C1E1C1", "#F8E1B9", "#FFFFFF");
        ColorSpinnerAdapter colorAdapter = new ColorSpinnerAdapter(this, colorValues, colorNames.toArray(new String[0]));
        colorSpinner.setAdapter(colorAdapter);

        if (folder != null) {
            if (folder.getName().equals("Неотсортированные")) {
                Toast.makeText(this, "Папка 'Неотсортированные' защищена от изменений", Toast.LENGTH_SHORT).show();
                return;
            }
            folderNameInput.setText(folder.getName());
            int colorIndex = colorValues.indexOf(folder.getColor());
            colorSpinner.setSelection(colorIndex != -1 ? colorIndex : colorValues.indexOf("#FFFFFF"));
            builder.setTitle("Редактировать папку");
        } else {
            colorSpinner.setSelection(colorValues.indexOf("#FFFFFF"));
            builder.setTitle("Новая папка");
        }

        builder.setView(dialogView)
                .setPositiveButton(folder != null ? "Сохранить" : "Создать", (dialog, which) -> {
                    String folderName = folderNameInput.getText().toString().trim();
                    String selectedColor = colorValues.get(colorSpinner.getSelectedItemPosition());
                    if (!folderName.isEmpty() && !folderName.equals("Неотсортированные")) {
                        Folder newFolder = folder != null ? folder : new Folder(folderName);
                        newFolder.setName(folderName);
                        newFolder.setColor(selectedColor);
                        new Thread(() -> {
                            if (newFolder.getId() == 0) {
                                AppDatabase.getInstance(this).folderDao().insertFolder(newFolder);
                            } else {
                                AppDatabase.getInstance(this).folderDao().updateFolder(newFolder);
                            }
                            viewModel.loadFolders();
                        }).start();
                    } else if (folderName.equals("Неотсортированные")) {
                        Toast.makeText(this, "Имя 'Неотсортированные' зарезервировано", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showMoveNotesDialog() {
        List<Folder> folders = viewModel.getAllFolders();
        List<String> folderNames = new ArrayList<>();
        for (Folder folder : folders) {
            folderNames.add(folder.getName());
        }
        new AlertDialog.Builder(this)
                .setTitle("Переместить заметки")
                .setItems(folderNames.toArray(new String[0]), (dialog, which) -> {
                    long selectedFolderId = folders.get(which).getId();
                    viewModel.moveNotes(viewModel.getSelectedNotes(), selectedFolderId);
                })
                .setNegativeButton("Отмена", null)
                .show();
    }
}