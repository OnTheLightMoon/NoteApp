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
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.example.wtf2.ColorSpinnerAdapter;
import com.example.wtf2.R;
import com.example.wtf2.data.AppDatabase;
import com.example.wtf2.data.model.Folder;
import com.example.wtf2.data.model.Note;
import com.example.wtf2.ui.note.NoteEditActivity;
import com.example.wtf2.util.GoogleDriveHelper;
import com.example.wtf2.viewmodel.MainViewModel;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.api.services.drive.DriveScopes;

import java.io.IOException;
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
    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> signInLauncher;
    private NavigationView navigationView;
    private GoogleDriveHelper driveHelper;

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
        navigationView = findViewById(R.id.nav_view);
        // Инициализация GoogleDriveHelper
        driveHelper = new GoogleDriveHelper(this);

        // Инициализация Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        // Лаунчер для обработки результата авторизации
        signInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        try {
                            GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException.class);
                            handleSignInResult(account);
                        } catch (ApiException e) {
                            Log.w("MainActivity", "Google Sign-In failed", e);
                            Toast.makeText(this, "Ошибка авторизации", Toast.LENGTH_SHORT).show();
                            updateNavigationMenu();
                        }
                    }
                });

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        findViewById(R.id.menuButton).setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.END));

        // Обновляем меню при открытии
        updateNavigationMenu();
        // Обновляем обработчик меню (без изменений, просто для полноты)
        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_notes) {
                viewModel.setCurrentTab(0);
            } else if (itemId == R.id.nav_folders) {
                viewModel.setCurrentTab(1);
            } else if (itemId == R.id.nav_sign_in) {
                signIn();
            } else if (itemId == R.id.nav_sign_out) {
                signOut();
            } else if (itemId == R.id.nav_upload_to_drive) {
                syncNotesWithDrive();
            } else if (itemId == R.id.nav_download_from_drive) {
                downloadFromDrive();
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
            Log.d("MainActivity", "Forced fragment container update, isSelectionMode=" + isSelectionMode);
            if (!isSelectionMode) {
                refreshCurrentFragment();
            }
        });

        viewModel.getSelectedCount().observe(this, count -> {
            selectionCount.setText("Выбрано: " + count);
            // Убираем смену иконки, фиксируем иконку перемещения
            actionEditMoveButton.setImageResource(android.R.drawable.ic_menu_upload);
        });

        fab.setOnClickListener(v -> {
            if (!viewModel.getIsSelectionMode().getValue()) {
                int currentTab = viewModel.getCurrentTab().getValue();
                boolean isInFolder = viewModel.getIsInFolder().getValue() != null && viewModel.getIsInFolder().getValue();

                if (currentTab == 0 || (currentTab == 1 && isInFolder)) {
                    Intent intent = new Intent(this, NoteEditActivity.class);
                    if (isInFolder) {
                        intent.putExtra("FOLDER_ID", viewModel.getCurrentFolderId().getValue());
                    }
                    startActivity(intent);
                } else {
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
            if (viewModel.getCurrentTab().getValue() == 0) { // Заметки
                List<Note> selectedNotes = viewModel.getSelectedNotes();
                if (!selectedNotes.isEmpty()) {
                    showMoveNotesDialog();
                }
            } else { // Папки
                List<Folder> selectedFolders = viewModel.getSelectedFolders();
                if (selectedFolders.size() == 1) {
                    showFolderDialog(selectedFolders.get(0));
                    // Выходим из режима выбора после открытия диалога редактирования папки
                    viewModel.exitSelectionMode();
                } else {
                    Toast.makeText(this, "Выберите только одну папку для редактирования", Toast.LENGTH_SHORT).show();
                }
            }
            Log.d("MainActivity", "Fragment container visibility: " + findViewById(R.id.fragment_container).getVisibility());
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackPress();
            }
        });

        findViewById(R.id.action_delete).setOnClickListener(v -> {
            viewModel.deleteSelectedItems();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        int currentTab = viewModel.getCurrentTab().getValue() != null ? viewModel.getCurrentTab().getValue() : 0;
        boolean isInFolder = viewModel.getIsInFolder().getValue() != null && viewModel.getIsInFolder().getValue();

        if (currentTab == 0) {
            if (isInFolder) {
                viewModel.loadNotes(viewModel.getCurrentFolderId().getValue());
            } else {
                viewModel.loadNotes(null);
            }
        } else if (currentTab == 1 && isInFolder) {
            viewModel.loadNotes(viewModel.getCurrentFolderId().getValue());
        }
        if (currentTab == 1 && !isInFolder) {
            viewModel.loadFolders();
        }
        updateNavigationMenu();
    }

    private void signIn() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        signInLauncher.launch(signInIntent);
    }

    private void signOut() {
        googleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Toast.makeText(this, "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show();
            updateNavigationMenu();
        });
    }

    private void handleSignInResult(GoogleSignInAccount account) {
        if (account != null) {
            Toast.makeText(this, "Авторизация успешна: " + account.getEmail(), Toast.LENGTH_SHORT).show();
            updateNavigationMenu();
        }
    }

    private void updateNavigationMenu() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        navigationView.getMenu().clear();
        if (account == null) {
            navigationView.inflateMenu(R.menu.nav_menu_unauthorized);
        } else {
            navigationView.inflateMenu(R.menu.nav_menu_authorized);
        }
    }

    private void handleBackPress() {
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END);
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
                .setPositiveButton(folder != null ? "Сохранить" : "Создать", null)
                .setNegativeButton("Отмена", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String folderName = folderNameInput.getText().toString().trim();
            String selectedColor = colorValues.get(colorSpinner.getSelectedItemPosition());
            List<String> existingFolderNames = viewModel.getFolderNames();

            if (folderName.isEmpty()) {
                Toast.makeText(this, "Введите имя папки", Toast.LENGTH_SHORT).show();
                return;
            }
            if (folderName.equals("Неотсортированные")) {
                Toast.makeText(this, "Имя 'Неотсортированные' зарезервировано", Toast.LENGTH_SHORT).show();
                return;
            }
            if (existingFolderNames.contains(folderName) && (folder == null || !folder.getName().equals(folderName))) {
                Toast.makeText(this, "Папка с именем '" + folderName + "' уже существует", Toast.LENGTH_SHORT).show();
                return;
            }

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
                runOnUiThread(() -> {
                    // Обновляем фрагмент после сохранения
                    refreshCurrentFragment();
                });
            }).start();
            dialog.dismiss();
        });
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

    private void syncNotesWithDrive() {
        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(this);
                db.close();
                driveHelper.syncDatabaseToDrive();
                runOnUiThread(() -> Toast.makeText(this, "Данные загружены на Google Drive", Toast.LENGTH_SHORT).show());
                AppDatabase.getInstance(this); // Переоткрываем базу
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Ошибка загрузки: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void downloadFromDrive() {
        new Thread(() -> {
            try {
                driveHelper.downloadDatabaseFromDrive();
                runOnUiThread(() -> {
                    viewModel.loadFolders(); // Загружаем папки
                    viewModel.loadNotes(null); // Загружаем заметки
                    // Тост добавляем здесь, когда данные готовы
                    Toast.makeText(this, "Данные загружены с Google Drive", Toast.LENGTH_SHORT).show();
                });
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Ошибка загрузки: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    public void refreshCurrentFragment() {
        int currentTab = viewModel.getCurrentTab().getValue() != null ? viewModel.getCurrentTab().getValue() : 0;
        Fragment selectedFragment = currentTab == 0 ? new NotesFragment() : new FoldersFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, selectedFragment);
        transaction.commit();
        Log.d("MainActivity", "Refreshed current fragment: " + selectedFragment.getClass().getSimpleName());
    }
}