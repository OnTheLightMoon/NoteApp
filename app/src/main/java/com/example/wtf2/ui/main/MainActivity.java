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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Главная активность приложения, управляющая вкладками заметок и папок, боковым меню и Google Drive.
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int TAB_NOTES = 0;
    private static final int TAB_FOLDERS = 1;

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
    private NavigationView navigationView;
    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> signInLauncher;
    private GoogleDriveHelper driveHelper;
    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        Log.d(TAG, "Initializing MainActivity");

        initializeViews();
        setupGoogleSignIn();
        setupNavigationDrawer();
        setupObservers();
        setupListeners();

        if (savedInstanceState == null) {
            replaceFragment(new NotesFragment());
        }

        // Асинхронная инициализация базы данных
        executor.execute(() -> {
            waitForDatabaseInitialization();
            runOnUiThread(() -> {
                viewModel.loadFolders();
                viewModel.loadNotes(null);
                Log.d(TAG, "Database initialized, loaded initial data");
            });
        });
    }

    /**
     * Инициализирует все UI-компоненты активности.
     */
    private void initializeViews() {
        FloatingActionButton fab = findViewById(R.id.add_btn);
        notesTab = findViewById(R.id.notesTab);
        foldersTab = findViewById(R.id.foldersTab);
        toggleIndicator = findViewById(R.id.toggleIndicator);
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
        driveHelper = new GoogleDriveHelper(this);
    }

    /**
     * Настраивает Google Sign-In для авторизации и работы с Google Drive.
     */
    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);
        signInLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK) {
                try {
                    GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(result.getData())
                            .getResult(ApiException.class);
                    handleSignInResult(account);
                    driveHelper.reinitializeDriveService(); // Переинициализация после входа
                } catch (ApiException e) {
                    Log.w(TAG, "Google Sign-In failed: " + e.getStatusCode(), e);
                    Toast.makeText(this, "Ошибка авторизации: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    updateNavigationMenu();
                }
            }
        });
    }

    /**
     * Настраивает боковое меню и его обработчики.
     */
    private void setupNavigationDrawer() {
        findViewById(R.id.menuButton).setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.END));
        updateNavigationMenu();
        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_sign_in) signIn();
            else if (itemId == R.id.nav_sign_out) signOut();
            else if (itemId == R.id.nav_upload_to_drive) syncNotesWithDrive();
            else if (itemId == R.id.nav_download_from_drive) downloadFromDrive();
            else if (itemId == R.id.nav_settings) Toast.makeText(this, "Настройки пока не реализованы", Toast.LENGTH_SHORT).show();
            drawerLayout.closeDrawer(GravityCompat.END);
            return true;
        });
    }

    /**
     * Настраивает наблюдателей за изменениями в ViewModel.
     */
    private void setupObservers() {
        viewModel.getCurrentTab().observe(this, tab -> {
            replaceFragment(tab == TAB_NOTES ? new NotesFragment() : new FoldersFragment());
            animateToggle(tab);
            updateTabStyles(tab);
            updateSearchAndFilterVisibility(tab);
        });

        viewModel.getIsInFolder().observe(this, isInFolder -> {
            findViewById(R.id.back_button_navigation).setVisibility(isInFolder ? View.VISIBLE : View.GONE);
            findViewById(R.id.toggleFrame).setVisibility(isInFolder ? View.GONE : View.VISIBLE);
            updateSearchAndFilterVisibility(viewModel.getCurrentTab().getValue());
        });

        viewModel.getIsSelectionMode().observe(this, isSelectionMode -> {
            tabsContainer.setVisibility(isSelectionMode ? View.GONE : View.VISIBLE);
            selectionHeader.setVisibility(isSelectionMode ? View.VISIBLE : View.GONE);
            selectionPanel.setVisibility(isSelectionMode ? View.VISIBLE : View.GONE);
            if (!isSelectionMode) refreshCurrentFragment();
        });

        viewModel.getSelectedCount().observe(this, count -> {
            selectionCount.setText("Выбрано: " + count);
            actionEditMoveButton.setImageResource(android.R.drawable.ic_menu_upload);
        });
    }

    /**
     * Настраивает слушатели событий для UI-элементов.
     */
    private void setupListeners() {
        findViewById(R.id.add_btn).setOnClickListener(v -> handleFabClick());
        notesTab.setOnClickListener(v -> switchTabIfNotSelected(TAB_NOTES));
        foldersTab.setOnClickListener(v -> switchTabIfNotSelected(TAB_FOLDERS));
        findViewById(R.id.back_button_navigation).setOnClickListener(v -> handleBackPress());
        selectionBackButton.setOnClickListener(v -> viewModel.exitSelectionMode());
        actionPinButton.setOnClickListener(v -> viewModel.pinSelectedItems());
        findViewById(R.id.action_share).setOnClickListener(v -> Toast.makeText(this, "Поделиться пока не реализовано", Toast.LENGTH_SHORT).show());
        findViewById(R.id.action_delete).setOnClickListener(v -> viewModel.deleteSelectedItems());
        actionEditMoveButton.setOnClickListener(v -> handleEditMoveAction());

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
            if (event.getAction() == MotionEvent.ACTION_UP && !searchInput.getText().toString().isEmpty()) {
                Drawable rightDrawable = searchInput.getCompoundDrawables()[2];
                if (rightDrawable != null && event.getRawX() >= (searchInput.getRight() - rightDrawable.getBounds().width())) {
                    searchInput.setText("");
                    return true;
                }
            }
            return false;
        });

        filterButton.setOnClickListener(v -> showSortDialog());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() { handleBackPress(); }
        });
    }

    /**
     * Обрабатывает нажатие на FAB в зависимости от текущего состояния.
     */
    private void handleFabClick() {
        if (!viewModel.getIsSelectionMode().getValue()) {
            int currentTab = viewModel.getCurrentTab().getValue();
            boolean isInFolder = viewModel.getIsInFolder().getValue() != null && viewModel.getIsInFolder().getValue();
            if (currentTab == TAB_NOTES || (currentTab == TAB_FOLDERS && isInFolder)) {
                Intent intent = new Intent(this, NoteEditActivity.class);
                if (isInFolder) intent.putExtra("FOLDER_ID", viewModel.getCurrentFolderId().getValue());
                startActivity(intent);
            } else {
                showFolderDialog(null);
            }
        }
    }

    /**
     * Переключает вкладку, если она еще не выбрана и не в режиме выбора.
     */
    private void switchTabIfNotSelected(int tab) {
        if (viewModel.getCurrentTab().getValue() != tab && !viewModel.getIsSelectionMode().getValue()) {
            viewModel.setCurrentTab(tab);
        }
    }

    /**
     * Обрабатывает действие редактирования или перемещения выбранных элементов.
     */
    private void handleEditMoveAction() {
        if (viewModel.getCurrentTab().getValue() == TAB_NOTES) {
            List<Note> selectedNotes = viewModel.getSelectedNotes();
            if (!selectedNotes.isEmpty()) {
                showMoveNotesDialog();
            }
        } else {
            List<Folder> selectedFolders = viewModel.getSelectedFolders();
            if (selectedFolders.size() == 1) {
                showFolderDialog(selectedFolders.get(0));
                viewModel.exitSelectionMode();
            } else {
                Toast.makeText(this, "Выберите только одну папку для редактирования", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Показывает диалог сортировки заметок.
     */
    private void showSortDialog() {
        String[] options = {"Без сортировки", "Дата (возрастание)", "Дата (убывание)", "Заголовок (A-Z)", "Заголовок (Z-A)"};
        new AlertDialog.Builder(this)
                .setTitle("Сортировка")
                .setItems(options, (dialog, which) -> viewModel.setSortOrder(which))
                .setNegativeButton("Отмена", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        int currentTab = viewModel.getCurrentTab().getValue() != null ? viewModel.getCurrentTab().getValue() : TAB_NOTES;
        boolean isInFolder = viewModel.getIsInFolder().getValue() != null && viewModel.getIsInFolder().getValue();
        if (currentTab == TAB_NOTES || (currentTab == TAB_FOLDERS && isInFolder)) {
            viewModel.loadNotes(isInFolder ? viewModel.getCurrentFolderId().getValue() : null);
        } else if (currentTab == TAB_FOLDERS) {
            viewModel.loadFolders();
        }
        updateNavigationMenu();
    }

    private void signIn() { signInLauncher.launch(googleSignInClient.getSignInIntent()); }
    private void signOut() {
        googleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Toast.makeText(this, "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show();
            driveHelper.reinitializeDriveService(); // Переинициализация после выхода
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
        navigationView.inflateMenu(account == null ? R.menu.nav_menu_unauthorized : R.menu.nav_menu_authorized);
    }

    /**
     * Обрабатывает нажатие кнопки "назад" с учетом текущего состояния.
     */
    private void handleBackPress() {
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END);
            return;
        }
        if (viewModel.getIsSelectionMode().getValue()) {
            viewModel.exitSelectionMode();
            return;
        }
        if (viewModel.getIsInFolder().getValue()) {
            viewModel.setIsInFolder(false);
            replaceFragment(viewModel.getCurrentTab().getValue() == TAB_NOTES ? new NotesFragment() : new FoldersFragment());
            return;
        }
        if (viewModel.getCurrentTab().getValue() == TAB_FOLDERS) {
            viewModel.setCurrentTab(TAB_NOTES);
            return;
        }
        finish();
    }

    private void animateToggle(int selectedTab) {
        ValueAnimator animator = ValueAnimator.ofFloat(toggleIndicator.getTranslationX(),
                selectedTab == TAB_NOTES ? notesTab.getLeft() : foldersTab.getLeft());
        animator.setDuration(200);
        animator.addUpdateListener(animation -> {
            toggleIndicator.setTranslationX((float) animation.getAnimatedValue());
            toggleIndicator.getLayoutParams().width = selectedTab == TAB_NOTES ? notesTab.getWidth() : foldersTab.getWidth();
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

            if (folderName.isEmpty() || folderName.equals("Неотсортированные") ||
                    (existingFolderNames.contains(folderName) && (folder == null || !folder.getName().equals(folderName)))) {
                Toast.makeText(this, folderName.isEmpty() ? "Введите имя папки" :
                        folderName.equals("Неотсортированные") ? "Имя 'Неотсортированные' зарезервировано" :
                                "Папка с именем '" + folderName + "' уже существует", Toast.LENGTH_SHORT).show();
                return;
            }

            Folder newFolder = folder != null ? folder : new Folder(folderName);
            newFolder.setName(folderName);
            newFolder.setColor(selectedColor);
            executor.execute(() -> {
                AppDatabase db = AppDatabase.getInstance(this);
                if (newFolder.getId() == 0) db.folderDao().insertFolder(newFolder);
                else db.folderDao().updateFolder(newFolder);
                viewModel.loadFolders();
                runOnUiThread(this::refreshCurrentFragment);
            });
            dialog.dismiss();
        });
    }

    private void showMoveNotesDialog() {
        List<Folder> folders = viewModel.getAllFolders();
        new AlertDialog.Builder(this)
                .setTitle("Переместить заметки")
                .setItems(folders.stream().map(Folder::getName).toArray(String[]::new),
                        (dialog, which) -> viewModel.moveNotes(viewModel.getSelectedNotes(), folders.get(which).getId()))
                .setNegativeButton("Отмена", null)
                .show();
    }

    /**
     * Синхронизирует заметки с Google Drive, проверяя авторизацию.
     */
    private void syncNotesWithDrive() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account == null) {
            Toast.makeText(this, "Пожалуйста, войдите в аккаунт Google", Toast.LENGTH_SHORT).show();
            signIn();
            return;
        }
        executor.execute(() -> {
            try {
                driveHelper.syncDatabaseToDrive();
                runOnUiThread(() -> Toast.makeText(this, "Данные загружены на Google Drive", Toast.LENGTH_SHORT).show());
            } catch (IOException e) {
                Log.e(TAG, "Failed to sync with Google Drive: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(this, "Ошибка загрузки: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    /**
     * Загружает данные с Google Drive, проверяя авторизацию.
     */
    private void downloadFromDrive() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account == null) {
            Toast.makeText(this, "Пожалуйста, войдите в аккаунт Google", Toast.LENGTH_SHORT).show();
            signIn();
            return;
        }
        executor.execute(() -> {
            try {
                driveHelper.downloadDatabaseFromDrive();
                runOnUiThread(() -> {
                    viewModel.loadFolders();
                    viewModel.loadNotes(null);
                    Toast.makeText(this, "Данные загружены с Google Drive", Toast.LENGTH_SHORT).show();
                });
            } catch (IOException e) {
                Log.e(TAG, "Failed to download from Google Drive: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(this, "Ошибка загрузки: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    /**
     * Обновляет текущий фрагмент в зависимости от текущей вкладки.
     */
    public void refreshCurrentFragment() {
        replaceFragment(viewModel.getCurrentTab().getValue() == TAB_NOTES ? new NotesFragment() : new FoldersFragment());
    }

    private void replaceFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    private void updateTabStyles(int tab) {
        notesTab.setSelected(tab == TAB_NOTES);
        foldersTab.setSelected(tab == TAB_FOLDERS);
        notesTab.setTextColor(tab == TAB_NOTES ? Color.BLACK : Color.GRAY);
        foldersTab.setTextColor(tab == TAB_FOLDERS ? Color.BLACK : Color.GRAY);
    }

    private void updateSearchAndFilterVisibility(int tab) {
        boolean isInFolder = viewModel.getIsInFolder().getValue() != null && viewModel.getIsInFolder().getValue();
        boolean shouldShow = (tab == TAB_NOTES) || (tab == TAB_FOLDERS && isInFolder);
        searchInput.setVisibility(shouldShow ? View.VISIBLE : View.GONE);
        filterButton.setVisibility(shouldShow ? View.VISIBLE : View.GONE);
    }

    private void waitForDatabaseInitialization() {
        while (!AppDatabase.isInitialized()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Log.e(TAG, "Interrupted while waiting for DB", e);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}