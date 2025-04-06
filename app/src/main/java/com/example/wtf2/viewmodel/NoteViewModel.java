package com.example.wtf2.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.wtf2.data.AppDatabase;
import com.example.wtf2.data.model.Folder;
import com.example.wtf2.data.model.Note;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ViewModel для управления редактированием и созданием заметок.
 * Отвечает за загрузку заметки, сохранение изменений и статистику текста.
 */
public class NoteViewModel extends AndroidViewModel {
    private static final String TAG = "NoteViewModel";
    private final AppDatabase db;
    // Пул потоков для асинхронных операций
    private final ExecutorService executor = Executors.newFixedThreadPool(1);
    // Текущая редактируемая заметка
    private final MutableLiveData<Note> currentNote = new MutableLiveData<>();
    // Список доступных папок
    private final MutableLiveData<List<Folder>> folders = new MutableLiveData<>(new ArrayList<>());
    // Статистика текста заметки
    private final MutableLiveData<String> statistics = new MutableLiveData<>("Символов: 0, Слов: 0, Строк: 0");
    private static final String DEFAULT_FOLDER_NAME = "Неотсортированные";
    private static final long DEFAULT_FOLDER_ID = 1L;
    private final MainViewModel mainViewModel;

    public NoteViewModel(@NonNull Application application, MainViewModel mainViewModel) {
        super(application);
        this.db = AppDatabase.getInstance(application);
        this.mainViewModel = mainViewModel;
        loadFolders(); // Начальная загрузка папок для выбора
    }

    public void setCurrentNote(Note note) { currentNote.setValue(note); }
    public LiveData<Note> getCurrentNote() { return currentNote; }
    public LiveData<List<Folder>> getFolders() { return folders; }
    public LiveData<String> getStatistics() { return statistics; }

    /**
     * Загружает заметку по ID или создает новую с указанным folderId.
     */
    public void loadNote(int noteId, long initialFolderId) {
        executor.execute(() -> {
            Note note = (noteId != -1) ? db.noteDao().getNoteById(noteId) : null;
            if (note == null) {
                note = new Note("", "", getCurrentDate(), initialFolderId != -1 ? initialFolderId : getDefaultFolderId());
                Log.d(TAG, "Created new note with folderId: " + note.getFolderId());
            } else {
                Log.d(TAG, "Loaded note with ID: " + note.getId());
            }
            currentNote.postValue(note);
            updateStatistics(note.getContent());
            loadFolders();
        });
    }

    public void loadNote(int noteId) { loadNote(noteId, -1); }

    /**
     * Определяет ID папки по умолчанию для новой заметки.
     */
    private long getDefaultFolderId() {
        Boolean isInFolder = mainViewModel.getIsInFolder().getValue();
        if (isInFolder != null && isInFolder) {
            Long folderId = mainViewModel.getCurrentFolderId().getValue();
            return folderId != null ? folderId : DEFAULT_FOLDER_ID;
        }
        return DEFAULT_FOLDER_ID;
    }

    /**
     * Сохраняет заметку в базу данных, обновляя существующую или создавая новую.
     * После сохранения обновляет списки заметок и папок в MainViewModel.
     */
    public void saveNote(String title, String content, long folderId) {
        Note note = currentNote.getValue();
        if (note == null) {
            note = new Note("", "", getCurrentDate(), getDefaultFolderId());
            currentNote.setValue(note);
        }
        note.setTitle(title);
        note.setContent(content);
        note.setFolderId(folderId);
        note.setModifiedDate(getCurrentDate());
        Note finalNote = note;
        executor.execute(() -> {
            if (finalNote.getId() == 0) {
                long newId = db.noteDao().insert(finalNote);
                finalNote.setId((int) newId);
                Log.d(TAG, "Inserted new note with ID: " + newId);
            } else {
                db.noteDao().updateNote(finalNote);
                Log.d(TAG, "Updated note with ID: " + finalNote.getId());
            }
            Boolean isInFolder = mainViewModel.getIsInFolder().getValue();
            mainViewModel.loadNotes(isInFolder != null && isInFolder ? mainViewModel.getCurrentFolderId().getValue() : null);
            mainViewModel.loadFolders(); // Обновляем список папок после сохранения заметки
        });
    }

    /**
     * Обновляет статистику текста заметки (символы, слова, строки).
     */
    public void updateStatistics(String text) {
        if (text == null || text.trim().isEmpty()) {
            statistics.postValue("Строк: 0, Слов: 0, Символов: 0");
        } else {
            int charCount = text.length();
            int wordCount = text.trim().split("\\s+").length;
            int lineCount = text.split("\n").length;
            statistics.postValue("Строк: " + lineCount + ", Слов: " + wordCount + ", Символов: " + charCount);
        }
    }

    /**
     * Загружает список всех папок из базы данных.
     */
    private void loadFolders() {
        executor.execute(() -> {
            List<Folder> folderList = db.folderDao().getAllFolders();
            folders.postValue(folderList);
            Log.d(TAG, "Loaded " + folderList.size() + " folders");
        });
    }

    /**
     * Возвращает текущую дату и время в формате "dd.MM.yyyy HH:mm".
     */
    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        return sdf.format(new Date());
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown(); // Очищаем пул потоков
    }
}