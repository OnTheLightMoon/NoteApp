package com.example.wtf2.viewmodel;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.wtf2.data.AppDatabase;
import com.example.wtf2.data.model.Folder;
import com.example.wtf2.data.model.Note;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * ViewModel для управления основным экраном приложения (заметки и папки).
 * Отвечает за загрузку данных, фильтрацию, сортировку и управление режимом выбора.
 */
public class MainViewModel extends AndroidViewModel {
    private static final String TAG = "MainViewModel";
    private final AppDatabase db;
    // Пул потоков для выполнения асинхронных операций
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    // LiveData для списка заметок
    private final MutableLiveData<List<Note>> notesLiveData = new MutableLiveData<>(new ArrayList<>());
    // LiveData для списка папок
    private final MutableLiveData<List<Folder>> foldersLiveData = new MutableLiveData<>(new ArrayList<>());
    // LiveData для цветов папок (имя -> цвет)
    private final MutableLiveData<Map<String, String>> folderColorsLiveData = new MutableLiveData<>(new HashMap<>());
    // Текущая вкладка (0 - заметки, 1 - папки)
    private final MutableLiveData<Integer> currentTab = new MutableLiveData<>(0);
    // Поисковый запрос
    private final MutableLiveData<String> searchQuery = new MutableLiveData<>("");
    // Порядок сортировки (0 - по умолчанию, 1 - дата ↑, 2 - дата ↓, 3 - A-Z, 4 - Z-A)
    private final MutableLiveData<Integer> sortOrder = new MutableLiveData<>(0);
    // Режим множественного выбора
    private final MutableLiveData<Boolean> isSelectionMode = new MutableLiveData<>(false);
    // Количество выбранных элементов
    private final MutableLiveData<Integer> selectedCount = new MutableLiveData<>(0);
    // Находимся ли внутри папки
    private final MutableLiveData<Boolean> isInFolder = new MutableLiveData<>(false);
    // ID текущей папки
    private final MutableLiveData<Long> currentFolderId = new MutableLiveData<>(0L);
    // Кэш всех заметок и папок
    private List<Note> allNotes = new ArrayList<>();
    private List<Folder> allFolders = new ArrayList<>();
    // Списки выбранных элементов
    private final List<Note> selectedNotes = new ArrayList<>();
    private final List<Folder> selectedFolders = new ArrayList<>();

    public MainViewModel(Application application) {
        super(application);
        db = AppDatabase.getInstance(application);
        Log.d(TAG, "MainViewModel initialized");
        loadFolders(); // Начальная загрузка папок
    }

    // Геттеры для LiveData
    public LiveData<List<Note>> getNotes() { return notesLiveData; }
    public LiveData<List<Folder>> getFolders() { return foldersLiveData; }
    public LiveData<Map<String, String>> getFolderColors() { return folderColorsLiveData; }
    public LiveData<Integer> getCurrentTab() { return currentTab; }
    public LiveData<String> getSearchQuery() { return searchQuery; }
    public LiveData<Integer> getSortOrder() { return sortOrder; }
    public LiveData<Boolean> getIsSelectionMode() { return isSelectionMode; }
    public LiveData<Integer> getSelectedCount() { return selectedCount; }
    public LiveData<Boolean> getIsInFolder() { return isInFolder; }
    public LiveData<Long> getCurrentFolderId() { return currentFolderId; }

    // Сеттеры для изменения состояния
    public void setCurrentTab(int tab) { currentTab.setValue(tab); }
    public void setSearchQuery(String query) {
        searchQuery.setValue(query);
        filterNotes(query);
    }
    public void setSortOrder(int order) {
        sortOrder.setValue(order);
        filterNotes(searchQuery.getValue());
    }
    public void setIsInFolder(boolean inFolder) { isInFolder.setValue(inFolder); }
    public void setCurrentFolderId(long folderId) { currentFolderId.setValue(folderId); }

    /**
     * Загружает заметки из базы данных для указанной папки или все заметки, если folderId null.
     * Выполняется асинхронно в пуле потоков.
     */
    public void loadNotes(Long folderId) {
        executor.execute(() -> {
            ensureDatabaseOpen();
            List<Note> notes = folderId != null
                    ? db.noteDao().getNotesByFolderId(folderId)
                    : db.noteDao().getAllNotes();
            sortNotes(notes); // Сортировка вынесена в отдельный метод
            allNotes = notes;
            filterNotes(searchQuery.getValue());
            Log.d(TAG, "Loaded " + notes.size() + " notes for folderId: " + (folderId != null ? folderId : "all"));
        });
    }

    /**
     * Загружает все папки из базы данных и обновляет их количество заметок.
     * Выполняется асинхронно.
     */
    public void loadFolders() {
        executor.execute(() -> {
            ensureDatabaseOpen();
            List<Folder> folders = db.folderDao().getAllFolders();
            for (Folder folder : folders) {
                folder.setNoteCount(db.noteDao().getNotesByFolderId(folder.getId()).size());
            }
            folders.sort(Comparator.comparing(Folder::isPinned).reversed()
                    .thenComparing(Folder::getName));
            allFolders = folders;
            foldersLiveData.postValue(folders);
            updateFolderColors(folders);
            Log.d(TAG, "Loaded " + folders.size() + " folders");
        });
    }

    /**
     * Фильтрует заметки по поисковому запросу и применяет сортировку.
     */
    private void filterNotes(String query) {
        if (query == null) query = "";
        String finalQuery = query.toLowerCase();
        List<Note> filteredNotes = allNotes.stream()
                .filter(note -> note.getTitle().toLowerCase().contains(finalQuery) ||
                        note.getContent().toLowerCase().contains(finalQuery))
                .collect(Collectors.toList());
        applySort(filteredNotes);
        notesLiveData.postValue(filteredNotes);
    }

    /**
     * Применяет сортировку к списку заметок в зависимости от текущего порядка.
     */
    private void applySort(List<Note> notes) {
        int order = sortOrder.getValue() != null ? sortOrder.getValue() : 0;
        Comparator<Note> pinnedComparator = Comparator.comparing(Note::isPinned).reversed();
        switch (order) {
            case 1: notes.sort(pinnedComparator.thenComparing(Note::getModifiedDate)); break;
            case 2: notes.sort(pinnedComparator.thenComparing(Note::getModifiedDate, Comparator.reverseOrder())); break;
            case 3: notes.sort(pinnedComparator.thenComparing(Note::getTitle)); break;
            case 4: notes.sort(pinnedComparator.thenComparing(Note::getTitle, Comparator.reverseOrder())); break;
            default: notes.sort(pinnedComparator.thenComparing(Note::getModifiedDate, Comparator.reverseOrder())); break;
        }
    }

    /**
     * Сортирует заметки по умолчанию (прикрепленные сверху, затем по дате убывания).
     */
    private void sortNotes(List<Note> notes) {
        notes.sort(Comparator.comparing(Note::isPinned).reversed()
                .thenComparing(Note::getModifiedDate, Comparator.reverseOrder()));
    }

    /**
     * Обновляет LiveData с цветами папок.
     */
    private void updateFolderColors(List<Folder> folders) {
        Map<String, String> folderColors = new HashMap<>();
        for (Folder folder : folders) {
            folderColors.put(folder.getName(), folder.getColor());
        }
        folderColorsLiveData.postValue(folderColors);
    }

    /**
     * Включает режим множественного выбора.
     */
    public void enterSelectionMode() {
        isSelectionMode.setValue(true);
        selectedCount.setValue(0);
    }

    /**
     * Выключает режим выбора и очищает списки выбранных элементов.
     */
    public void exitSelectionMode() {
        isSelectionMode.postValue(false);
        selectedNotes.clear();
        selectedFolders.clear();
        selectedCount.postValue(0);
    }

    public void updateSelectionCount(int count) { selectedCount.setValue(count); }
    public void toggleNoteSelection(Note note) {
        if (selectedNotes.contains(note)) selectedNotes.remove(note);
        else selectedNotes.add(note);
        selectedCount.setValue(selectedNotes.size());
    }
    public void toggleFolderSelection(Folder folder) {
        if (selectedFolders.contains(folder)) selectedFolders.remove(folder);
        else selectedFolders.add(folder);
        selectedCount.setValue(selectedFolders.size());
    }
    public boolean isNoteSelected(Note note) { return selectedNotes.contains(note); }
    public boolean isFolderSelected(Folder folder) { return selectedFolders.contains(folder); }
    public List<Note> getSelectedNotes() { return new ArrayList<>(selectedNotes); }
    public List<Folder> getSelectedFolders() { return new ArrayList<>(selectedFolders); }

    /**
     * Закрепляет или открепляет выбранные элементы (заметки или папки).
     */
    public void pinSelectedItems() {
        executor.execute(() -> {
            if (currentTab.getValue() == 0) {
                boolean allPinned = selectedNotes.stream().allMatch(Note::isPinned);
                db.noteDao().updatePinnedStatus(
                        selectedNotes.stream().map(Note::getId).collect(Collectors.toList()), !allPinned);
                loadNotes(currentFolderId.getValue());
            } else {
                boolean allPinned = selectedFolders.stream().allMatch(Folder::isPinned);
                db.folderDao().updatePinnedStatus(
                        selectedFolders.stream().map(Folder::getId).collect(Collectors.toList()), !allPinned);
                loadFolders();
            }
            exitSelectionMode();
        });
    }

    /**
     * Удаляет выбранные элементы (заметки или папки) из базы данных.
     */
    public void deleteSelectedItems() {
        executor.execute(() -> {
            ensureDatabaseOpen();
            if (currentTab.getValue() == 0) {
                db.noteDao().deleteNotes(selectedNotes);
                loadNotes(currentFolderId.getValue());
            } else {
                List<Folder> foldersToDelete = selectedFolders.stream()
                        .filter(folder -> !folder.getName().equals("Неотсортированные"))
                        .collect(Collectors.toList());
                for (Folder folder : foldersToDelete) {
                    db.noteDao().deleteNotes(db.noteDao().getNotesByFolderId(folder.getId()));
                }
                if (!foldersToDelete.isEmpty()) {
                    db.folderDao().deleteFolders(foldersToDelete);
                }
                loadFolders();
            }
            new Handler(Looper.getMainLooper()).post(this::exitSelectionMode);
        });
    }

    /**
     * Перемещает заметки в указанную папку.
     */
    public void moveNotes(List<Note> notes, long folderId) {
        executor.execute(() -> {
            for (Note note : notes) {
                note.setFolderId(folderId);
                db.noteDao().updateNote(note);
            }
            loadNotes(currentFolderId.getValue());
            loadFolders(); // Обновляем папки после перемещения заметок
            new Handler(Looper.getMainLooper()).post(this::exitSelectionMode);
        });
    }

    public List<String> getFolderNames() {
        return allFolders.stream().map(Folder::getName).collect(Collectors.toList());
    }
    public List<Folder> getAllFolders() { return new ArrayList<>(allFolders); }
    public Note getNoteById(int id) {
        try {
            return db.noteDao().getNoteById(id);
        } catch (Exception e) {
            Log.e(TAG, "Error fetching note by ID: " + id, e);
            return null;
        }
    }

    /**
     * Убеждается, что база данных открыта перед выполнением операций.
     */
    private void ensureDatabaseOpen() {
        if (!db.isOpen()) {
            db.getOpenHelper().getWritableDatabase();
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown(); // Очищаем пул потоков при уничтожении ViewModel
    }
}