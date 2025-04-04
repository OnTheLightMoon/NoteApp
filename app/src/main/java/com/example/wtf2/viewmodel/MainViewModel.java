package com.example.wtf2.viewmodel;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.wtf2.R;
import com.example.wtf2.data.AppDatabase;
import com.example.wtf2.data.model.Folder;
import com.example.wtf2.data.model.Note;
import com.example.wtf2.ui.main.MainActivity;
import com.example.wtf2.ui.main.NotesFragment;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MainViewModel extends AndroidViewModel {
    private final AppDatabase db;
    private final MutableLiveData<List<Note>> notesLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Folder>> foldersLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Map<String, String>> folderColorsLiveData = new MutableLiveData<>(new HashMap<>());
    private final MutableLiveData<Integer> currentTab = new MutableLiveData<>(0);
    private final MutableLiveData<String> searchQuery = new MutableLiveData<>("");
    private final MutableLiveData<Integer> sortOrder = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> isSelectionMode = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> selectedCount = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> isInFolder = new MutableLiveData<>(false);
    private final MutableLiveData<Long> currentFolderId = new MutableLiveData<>(0L); // Изменяем с Integer на Long
    private List<Note> allNotes = new ArrayList<>();
    private List<Folder> allFolders = new ArrayList<>();
    private final List<Note> selectedNotes = new ArrayList<>();
    private final List<Folder> selectedFolders = new ArrayList<>();

    public MainViewModel(Application application) {
        super(application);
        db = AppDatabase.getInstance(application);
        Log.d("MainViewModel", "MainViewModel initialized");
    }

    public LiveData<List<Note>> getNotes() {
        return notesLiveData;
    }

    public LiveData<List<Folder>> getFolders() {
        return foldersLiveData;
    }

    public LiveData<Map<String, String>> getFolderColors() {
        return folderColorsLiveData;
    }

    public LiveData<Integer> getCurrentTab() {
        return currentTab;
    }

    public LiveData<String> getSearchQuery() {
        return searchQuery;
    }

    public LiveData<Integer> getSortOrder() {
        return sortOrder;
    }

    public LiveData<Boolean> getIsSelectionMode() {
        return isSelectionMode;
    }

    public LiveData<Integer> getSelectedCount() {
        return selectedCount;
    }

    public LiveData<Boolean> getIsInFolder() {
        return isInFolder;
    }

    public LiveData<Long> getCurrentFolderId() { // Изменяем с Integer на Long
        return currentFolderId;
    }

    public void setCurrentTab(int tab) {
        currentTab.setValue(tab);
    }

    public void setSearchQuery(String query) {
        searchQuery.setValue(query);
        filterNotes(query);
    }

    public void setSortOrder(int order) {
        sortOrder.setValue(order);
        filterNotes(searchQuery.getValue());
    }

    public void setIsInFolder(boolean inFolder) {
        isInFolder.setValue(inFolder);
    }

    public void setCurrentFolderId(long folderId) { // Изменяем с int на long
        currentFolderId.setValue(folderId);
    }

    public void loadNotes(Long folderId) {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplication());
            if (!db.isOpen()) {
                db.getOpenHelper().getWritableDatabase();
            }
            List<Note> notes = folderId != null
                    ? db.noteDao().getNotesByFolderId(folderId)
                    : db.noteDao().getAllNotes();
            notes.sort(Comparator.comparing(Note::isPinned).reversed()
                    .thenComparing(Note::getModifiedDate, Comparator.reverseOrder()));
            allNotes = notes;
            filterNotes(searchQuery.getValue());
            Log.d("MainViewModel", "Loaded " + notes.size() + " notes for folderId: " + (folderId != null ? folderId : "all"));
        }).start();
    }

    public void loadFolders() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplication());
            if (!db.isOpen()) {
                db.getOpenHelper().getWritableDatabase();
            }
            List<Folder> folders = db.folderDao().getAllFolders();
            for (Folder folder : folders) {
                folder.setNoteCount(db.noteDao().getNotesByFolderId(folder.getId()).size());
            }
            folders.sort(Comparator.comparing(Folder::isPinned).reversed()
                    .thenComparing(Folder::getName));
            allFolders = folders;
            foldersLiveData.postValue(folders);
            Map<String, String> folderColors = new HashMap<>();
            for (Folder folder : folders) {
                folderColors.put(folder.getName(), folder.getColor());
            }
            folderColorsLiveData.postValue(folderColors);
            Log.d("MainViewModel", "Loaded " + folders.size() + " folders, updated folderColors with " + folderColors.size() + " entries");
        }).start();
    }

    private void filterNotes(String query) {
        if (query == null) query = "";
        String finalQuery = query;
        List<Note> filteredNotes = allNotes.stream()
                .filter(note -> note.getTitle().toLowerCase().contains(finalQuery.toLowerCase()) ||
                        note.getContent().toLowerCase().contains(finalQuery.toLowerCase()))
                .collect(Collectors.toList());
        applySort(filteredNotes);
        notesLiveData.postValue(filteredNotes);
    }

    private void applySort(List<Note> notes) {
        Integer order = sortOrder.getValue();
        if (order == null) order = 0;
        Comparator<Note> pinnedComparator = Comparator.comparing(Note::isPinned).reversed();
        switch (order) {
            case 1: // Date ascending
                notes.sort(pinnedComparator.thenComparing(Note::getModifiedDate));
                break;
            case 2: // Date descending
                notes.sort(pinnedComparator.thenComparing(Note::getModifiedDate, Comparator.reverseOrder()));
                break;
            case 3: // Title A-Z
                notes.sort(pinnedComparator.thenComparing(Note::getTitle));
                break;
            case 4: // Title Z-A
                notes.sort(pinnedComparator.thenComparing(Note::getTitle, Comparator.reverseOrder()));
                break;
            default: // Default
                notes.sort(pinnedComparator.thenComparing(Note::getModifiedDate, Comparator.reverseOrder()));
                break;
        }
    }

    public void enterSelectionMode() {
        isSelectionMode.setValue(true);
        selectedCount.setValue(0);
    }

    public void exitSelectionMode() {
        isSelectionMode.postValue(false);
        selectedNotes.clear();
        selectedFolders.clear();
        selectedCount.postValue(0);
        // Не сбрасываем currentFolderId, чтобы сохранить контекст
    }

    public void updateSelectionCount(int count) {
        selectedCount.setValue(count);
    }

    public void toggleNoteSelection(Note note) {
        if (selectedNotes.contains(note)) {
            selectedNotes.remove(note);
        } else {
            selectedNotes.add(note);
        }
        selectedCount.setValue(selectedNotes.size());
    }

    public void toggleFolderSelection(Folder folder) {
        if (selectedFolders.contains(folder)) {
            selectedFolders.remove(folder);
        } else {
            selectedFolders.add(folder);
        }
        selectedCount.setValue(selectedFolders.size());
    }

    public boolean isNoteSelected(Note note) {
        return selectedNotes.contains(note);
    }

    public boolean isFolderSelected(Folder folder) {
        return selectedFolders.contains(folder);
    }

    public List<Note> getSelectedNotes() {
        return new ArrayList<>(selectedNotes);
    }

    public List<Folder> getSelectedFolders() {
        return new ArrayList<>(selectedFolders);
    }

    public void pinSelectedItems() {
        new Thread(() -> {
            if (currentTab.getValue() == 0) { // Notes
                boolean allPinned = selectedNotes.stream().allMatch(Note::isPinned);
                db.noteDao().updatePinnedStatus(
                        selectedNotes.stream().map(Note::getId).collect(Collectors.toList()),
                        !allPinned
                );
                loadNotes(currentFolderId.getValue());
            } else { // Folders
                boolean allPinned = selectedFolders.stream().allMatch(Folder::isPinned);
                db.folderDao().updatePinnedStatus(
                        selectedFolders.stream().map(Folder::getId).collect(Collectors.toList()),
                        !allPinned
                );
                loadFolders();
            }
            exitSelectionMode();
        }).start();
    }

    public void deleteSelectedItems() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplication());
            if (!db.isOpen()) {
                db.getOpenHelper().getWritableDatabase();
            }
            if (currentTab.getValue() == 0) { // Notes
                db.noteDao().deleteNotes(selectedNotes);
                List<Note> updatedNotes = currentFolderId.getValue() != null
                        ? db.noteDao().getNotesByFolderId(currentFolderId.getValue())
                        : db.noteDao().getAllNotes();
                updatedNotes.sort(Comparator.comparing(Note::isPinned).reversed()
                        .thenComparing(Note::getModifiedDate, Comparator.reverseOrder()));
                allNotes = updatedNotes;
                new Handler(Looper.getMainLooper()).post(() -> {
                    notesLiveData.setValue(allNotes);
                    Log.d("MainViewModel", "Deleted notes and updated list with " + updatedNotes.size() + " items");
                    exitSelectionMode();
                    // Убираем прямой вызов refreshCurrentFragment()
                });
            } else { // Folders
                for (Folder folder : selectedFolders) {
                    if (folder.getName().equals("Неотсортированные")) {
                        continue;
                    }
                    List<Note> notesInFolder = db.noteDao().getNotesByFolderId(folder.getId());
                    if (!notesInFolder.isEmpty()) {
                        db.noteDao().deleteNotes(notesInFolder);
                    }
                }
                List<Folder> foldersToDelete = selectedFolders.stream()
                        .filter(folder -> !folder.getName().equals("Неотсортированные"))
                        .collect(Collectors.toList());
                if (!foldersToDelete.isEmpty()) {
                    db.folderDao().deleteFolders(foldersToDelete);
                }
                new Handler(Looper.getMainLooper()).post(() -> {
                    loadFolders();
                    exitSelectionMode();
                    // Убираем прямой вызов refreshCurrentFragment()
                });
            }
        }).start();
    }

    public void moveNotes(List<Note> notes, long folderId) {
        new Thread(() -> {
            for (Note note : notes) {
                note.setFolderId(folderId);
                db.noteDao().updateNote(note);
            }
            // Загружаем заметки в зависимости от текущего состояния
            Long currentId = currentFolderId.getValue();
            List<Note> updatedNotes = currentId != null && currentId != 0
                    ? db.noteDao().getNotesByFolderId(currentId)
                    : db.noteDao().getAllNotes();
            updatedNotes.sort(Comparator.comparing(Note::isPinned).reversed()
                    .thenComparing(Note::getModifiedDate, Comparator.reverseOrder()));
            allNotes = updatedNotes;
            new Handler(Looper.getMainLooper()).post(() -> {
                notesLiveData.setValue(allNotes);
                Log.d("MainViewModel", "Moved notes and updated list with " + updatedNotes.size() + " items");
                // Выход из режима выбора после перемещения
                exitSelectionMode();
            });
        }).start();
    }

    public List<String> getFolderNames() {
        return allFolders.stream().map(Folder::getName).collect(Collectors.toList());
    }

    public List<Folder> getAllFolders() {
        return new ArrayList<>(allFolders);
    }

    public Note getNoteById(int id) {
        try {
            return db.noteDao().getNoteById(id);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}