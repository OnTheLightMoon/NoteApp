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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private Button notesTab;
    private Button foldersTab;
    private View toggleIndicator;
    private EditText searchInput;
    private ImageButton filterButton;
    private ImageButton menuButton;
    private LinearLayout tabsContainer;
    private LinearLayout selectionPanel;
    private LinearLayout selectionHeader;
    private ImageButton selectionBackButton;
    private TextView selectionCount;
    private ImageButton actionPinButton;
    private static final int TAB_NOTES = 0;
    private static final int TAB_FOLDERS = 1;
    private int currentTab = TAB_NOTES;
    private ExecutorService executorService;
    private Handler mainHandler;
    private static final int REQUEST_CODE_NOTE_EDITOR = 1;
    private boolean isSelectionMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FloatingActionButton fab = findViewById(R.id.add_btn);
        notesTab = findViewById(R.id.notesTab);
        foldersTab = findViewById(R.id.foldersTab);
        toggleIndicator = findViewById(R.id.toggleIndicator);
        searchInput = findViewById(R.id.searchInput);
        filterButton = findViewById(R.id.imageButton);
        menuButton = findViewById(R.id.menuButton);
        tabsContainer = findViewById(R.id.tabsContainer);
        selectionPanel = findViewById(R.id.selection_panel);
        selectionHeader = findViewById(R.id.selection_header);
        selectionBackButton = findViewById(R.id.selection_back_button);
        selectionCount = findViewById(R.id.selection_count);
        actionPinButton = findViewById(R.id.action_pin);

        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new NotesListFragment())
                    .commit();
        }

        fab.setOnClickListener(v -> {
            if (!isSelectionMode) {
                if (currentTab == TAB_NOTES) {
                    Intent intent = new Intent(MainActivity.this, NoteEditor.class);
                    startActivityForResult(intent, REQUEST_CODE_NOTE_EDITOR);
                } else if (currentTab == TAB_FOLDERS) {
                    showPopupDialog();
                }
            }
        });

        notesTab.setOnClickListener(v -> {
            if (currentTab != TAB_NOTES && !isSelectionMode) {
                switchTab(TAB_NOTES);
            }
        });

        foldersTab.setOnClickListener(v -> {
            if (currentTab != TAB_FOLDERS && !isSelectionMode) {
                switchTab(TAB_FOLDERS);
            }
        });

        updateTabSelection(TAB_NOTES);
        animateToggle(TAB_NOTES);

        setupSearch();
        setupSelectionPanel();

        menuButton.setOnClickListener(v -> Toast.makeText(this, "Menu clicked", Toast.LENGTH_SHORT).show());
        selectionBackButton.setOnClickListener(v -> exitSelectionMode());
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

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(16, 16, 16, 16);

        EditText input = new EditText(this);
        input.setHint("Введите название новой папки");
        layout.addView(input);

        Spinner colorSpinner = new Spinner(this);
        String[] colors = {"#FFFFFF", "#FFCDD2", "#C8E6C9", "#BBDEFB", "#FFF9C4"};
        String[] colorNames = {"Белый", "Красный", "Зелёный", "Синий", "Жёлтый"};
        ColorSpinnerAdapter adapter = new ColorSpinnerAdapter(this, Arrays.asList(colors), colorNames);
        colorSpinner.setAdapter(adapter);
        layout.addView(colorSpinner);

        builder.setView(layout);

        builder.setPositiveButton("Создать", (dialog, which) -> {
            String folderName = input.getText().toString().trim();
            if (folderName.isEmpty()) {
                Toast.makeText(this, "Название не может быть пустым", Toast.LENGTH_SHORT).show();
                return;
            }
            String selectedColor = colors[colorSpinner.getSelectedItemPosition()];
            FoldersListFragment foldersFragment = (FoldersListFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.fragment_container);
            if (foldersFragment != null) {
                foldersFragment.addNewFolder(folderName, selectedColor);
            } else {
                switchToFoldersListFragment(folderName, selectedColor);
            }
        });

        builder.setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void switchToFoldersListFragment(String folderName, String color) {
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
                foldersFragment.addNewFolder(folderName, color);
            }
        }, 300);
    }

    public void openNoteEditor(Note note) {
        Intent intent = new Intent(this, NoteEditor.class);
        intent.putExtra("NOTE_ID", note.getId());
        intent.putExtra("NOTE_TITLE", note.getTitle());
        intent.putExtra("NOTE_CONTENT", note.getContent());
        intent.putExtra("NOTE_DATE", note.getDate());
        intent.putExtra("NOTE_FOLDER", note.getFolder());
        startActivityForResult(intent, REQUEST_CODE_NOTE_EDITOR);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_NOTE_EDITOR && resultCode == RESULT_OK) {
            NotesListFragment notesFragment = (NotesListFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.fragment_container);
            if (notesFragment != null && currentTab == TAB_NOTES) {
                notesFragment.refreshNotes();
            }
        }
    }

    private void setupSelectionPanel() {
        AppDatabase db = AppDatabase.getInstance(this);

        actionPinButton.setOnClickListener(v -> {
            Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if (fragment instanceof NotesListFragment) {
                NotesListFragment notesFragment = (NotesListFragment) fragment;
                List<Note> selectedNotes = notesFragment.getNotesAdapter().getSelectedNotes();
                boolean allPinned = selectedNotes.stream().allMatch(Note::isPinned);

                List<Integer> idsToUpdate = new ArrayList<>();
                for (Note note : selectedNotes) {
                    if (!allPinned && !note.isPinned()) {
                        idsToUpdate.add(note.getId());
                    } else if (allPinned) {
                        idsToUpdate.add(note.getId());
                    }
                }

                executorService.execute(() -> {
                    db.noteDao().updatePinnedStatus(idsToUpdate, !allPinned);
                    runOnUiThread(() -> {
                        notesFragment.refreshNotes();
                        exitSelectionMode();
                        Toast.makeText(this, allPinned ? "Заметки откреплены" : "Заметки закреплены", Toast.LENGTH_SHORT).show();
                    });
                });
            } else if (fragment instanceof FoldersListFragment) {
                FoldersListFragment foldersFragment = (FoldersListFragment) fragment;
                List<Folder> selectedFolders = foldersFragment.getFolderAdapter().getSelectedFolders();
                boolean allPinned = selectedFolders.stream().allMatch(Folder::isPinned);

                List<Integer> idsToUpdate = new ArrayList<>();
                for (Folder folder : selectedFolders) {
                    if (!allPinned && !folder.isPinned()) {
                        idsToUpdate.add(folder.getId());
                    } else if (allPinned) {
                        idsToUpdate.add(folder.getId());
                    }
                }

                executorService.execute(() -> {
                    db.folderDao().updatePinnedStatus(idsToUpdate, !allPinned);
                    runOnUiThread(() -> {
                        foldersFragment.loadFolders();
                        exitSelectionMode();
                        Toast.makeText(this, allPinned ? "Папки откреплены" : "Папки закреплены", Toast.LENGTH_SHORT).show();
                    });
                });
            }
        });

        findViewById(R.id.action_edit).setOnClickListener(v -> {
            Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if (fragment instanceof NotesListFragment) {
                NotesListFragment notesFragment = (NotesListFragment) fragment;
                List<Note> selectedNotes = notesFragment.getNotesAdapter().getSelectedNotes();
                showMoveDialog(selectedNotes, notesFragment);
            } else if (fragment instanceof FoldersListFragment) {
                FoldersListFragment foldersFragment = (FoldersListFragment) fragment;
                List<Folder> selectedFolders = foldersFragment.getFolderAdapter().getSelectedFolders();
                if (selectedFolders.size() == 1) {
                    showEditFolderDialog(selectedFolders.get(0), foldersFragment);
                } else {
                    Toast.makeText(this, "Выберите только одну папку для редактирования", Toast.LENGTH_SHORT).show();
                }
            }
        });

        findViewById(R.id.action_share).setOnClickListener(v -> Toast.makeText(this, "Поделиться (заглушка)", Toast.LENGTH_SHORT).show());

        findViewById(R.id.action_delete).setOnClickListener(v -> {
            Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if (fragment instanceof NotesListFragment) {
                NotesListFragment notesFragment = (NotesListFragment) fragment;
                List<Note> selectedNotes = notesFragment.getNotesAdapter().getSelectedNotes();
                showDeleteDialog(selectedNotes, null, notesFragment);
            } else if (fragment instanceof FoldersListFragment) {
                FoldersListFragment foldersFragment = (FoldersListFragment) fragment;
                List<Folder> selectedFolders = foldersFragment.getFolderAdapter().getSelectedFolders();
                showDeleteDialog(null, selectedFolders, foldersFragment);
            }
        });
    }

    public void enterSelectionMode(int selectedCount) {
        isSelectionMode = true;
        tabsContainer.setVisibility(View.GONE);
        selectionHeader.setVisibility(View.VISIBLE);
        selectionPanel.setVisibility(View.VISIBLE);
        selectionCount.setText("Выбрано: " + selectedCount);
        findViewById(R.id.action_edit).setVisibility(View.VISIBLE); // Всегда видна
        updateButtonsState(selectedCount);
    }

    public void updateSelectionCount(int selectedCount) {
        selectionCount.setText("Выбрано: " + selectedCount);
        updateButtonsState(selectedCount);
    }

    private void updateButtonsState(int selectedCount) {
        boolean enabled = selectedCount > 0;
        actionPinButton.setEnabled(enabled);
        findViewById(R.id.action_share).setEnabled(enabled);
        findViewById(R.id.action_delete).setEnabled(enabled);
        ImageButton editButton = findViewById(R.id.action_edit);
        if (currentTab == TAB_FOLDERS && selectedCount != 1) {
            editButton.setEnabled(false); // Только одна папка для редактирования
        } else {
            editButton.setEnabled(enabled);
        }
        updatePinButton();
    }

    private void updatePinButton() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (fragment instanceof NotesListFragment) {
            List<Note> selectedNotes = ((NotesListFragment) fragment).getNotesAdapter().getSelectedNotes();
            boolean allPinned = selectedNotes.stream().allMatch(Note::isPinned);
            actionPinButton.setImageResource(allPinned ? android.R.drawable.ic_menu_close_clear_cancel : android.R.drawable.ic_lock_lock);
            actionPinButton.setContentDescription(allPinned ? "Unpin" : "Pin");
        } else if (fragment instanceof FoldersListFragment) {
            List<Folder> selectedFolders = ((FoldersListFragment) fragment).getFolderAdapter().getSelectedFolders();
            boolean allPinned = selectedFolders.stream().allMatch(Folder::isPinned);
            actionPinButton.setImageResource(allPinned ? android.R.drawable.ic_menu_close_clear_cancel : android.R.drawable.ic_lock_lock);
            actionPinButton.setContentDescription(allPinned ? "Unpin" : "Pin");
        }
    }

    public void exitSelectionMode() {
        isSelectionMode = false;
        tabsContainer.setVisibility(View.VISIBLE);
        selectionHeader.setVisibility(View.GONE);
        selectionPanel.setVisibility(View.GONE);
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (fragment instanceof NotesListFragment) {
            ((NotesListFragment) fragment).exitSelectionMode();
        } else if (fragment instanceof FoldersListFragment) {
            ((FoldersListFragment) fragment).exitSelectionMode();
        }
    }

    private void showMoveDialog(List<Note> selectedNotes, NotesListFragment notesFragment) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Переместить заметки");

        // Создаём Spinner для выбора папки
        Spinner folderSpinner = new Spinner(this);
        List<String> folderNames = new ArrayList<>();
        executorService.execute(() -> {
            List<Folder> folders = AppDatabase.getInstance(this).folderDao().getAllFolders();
            folderNames.clear();
            for (Folder folder : folders) {
                folderNames.add(folder.getName());
            }
            runOnUiThread(() -> {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, folderNames);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                folderSpinner.setAdapter(adapter);
            });
        });

        builder.setView(folderSpinner);

        builder.setPositiveButton("ОК", (dialog, which) -> {
            String selectedFolder = folderSpinner.getSelectedItem() != null ? folderSpinner.getSelectedItem().toString() : null;
            if (selectedFolder != null) {
                executorService.execute(() -> {
                    for (Note note : selectedNotes) {
                        note.setFolder(selectedFolder);
                        AppDatabase.getInstance(this).noteDao().update(note);
                    }
                    runOnUiThread(() -> {
                        notesFragment.refreshNotes();
                        exitSelectionMode();
                        Toast.makeText(this, "Заметки перемещены в '" + selectedFolder + "'", Toast.LENGTH_SHORT).show();
                    });
                });
            }
        });

        builder.setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showEditFolderDialog(Folder folder, FoldersListFragment foldersFragment) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Редактировать папку");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(16, 16, 16, 16);

        EditText input = new EditText(this);
        input.setText(folder.getName());
        layout.addView(input);

        Spinner colorSpinner = new Spinner(this);
        String[] colors = {"#FFFFFF", "#FFCDD2", "#C8E6C9", "#BBDEFB", "#FFF9C4"};
        String[] colorNames = {"Белый", "Красный", "Зелёный", "Синий", "Жёлтый"};
        ColorSpinnerAdapter adapter = new ColorSpinnerAdapter(this, Arrays.asList(colors), colorNames);
        colorSpinner.setAdapter(adapter);
        int colorPosition = Arrays.asList(colors).indexOf(folder.getColor());
        colorSpinner.setSelection(colorPosition != -1 ? colorPosition : 0);
        layout.addView(colorSpinner);

        builder.setView(layout);

        builder.setPositiveButton("ОК", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (newName.isEmpty()) {
                Toast.makeText(this, "Название не может быть пустым", Toast.LENGTH_SHORT).show();
                return;
            }
            String newColor = colors[colorSpinner.getSelectedItemPosition()];
            executorService.execute(() -> {
                AppDatabase db = AppDatabase.getInstance(this);
                String oldName = folder.getName();
                folder.setName(newName);
                folder.setColor(newColor);
                db.folderDao().update(folder);
                List<Note> notes = db.noteDao().getNotesByFolder(oldName);
                for (Note note : notes) {
                    note.setFolder(newName);
                    db.noteDao().update(note);
                }
                runOnUiThread(() -> {
                    foldersFragment.loadFolders();
                    exitSelectionMode();
                    Toast.makeText(this, "Папка переименована в '" + newName + "'", Toast.LENGTH_SHORT).show();
                });
            });
        });

        builder.setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showDeleteDialog(List<Note> notes, List<Folder> folders, Fragment fragment) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Точно удалить?");
        builder.setMessage("Вы уверены, что хотите удалить выбранные элементы?");

        builder.setPositiveButton("Да", (dialog, which) -> {
            executorService.execute(() -> {
                AppDatabase db = AppDatabase.getInstance(this);
                if (notes != null) {
                    List<Integer> noteIds = new ArrayList<>();
                    for (Note note : notes) {
                        noteIds.add(note.getId());
                    }
                    db.noteDao().deleteNotes(noteIds);
                } else if (folders != null) {
                    List<Integer> folderIds = new ArrayList<>();
                    for (Folder folder : folders) {
                        folderIds.add(folder.getId());
                        // Удаляем все заметки в этой папке
                        List<Note> notesInFolder = db.noteDao().getNotesByFolder(folder.getName());
                        List<Integer> noteIds = new ArrayList<>();
                        for (Note note : notesInFolder) {
                            noteIds.add(note.getId());
                        }
                        db.noteDao().deleteNotes(noteIds);
                    }
                    db.folderDao().deleteFolders(folderIds);
                }
                runOnUiThread(() -> {
                    if (fragment instanceof NotesListFragment) {
                        ((NotesListFragment) fragment).refreshNotes();
                    } else if (fragment instanceof FoldersListFragment) {
                        ((FoldersListFragment) fragment).loadFolders();
                    }
                    exitSelectionMode();
                    Toast.makeText(this, "Элементы удалены", Toast.LENGTH_SHORT).show();
                });
            });
        });

        builder.setNegativeButton("Нет", (dialog, which) -> dialog.dismiss());
        builder.show();
    }
}