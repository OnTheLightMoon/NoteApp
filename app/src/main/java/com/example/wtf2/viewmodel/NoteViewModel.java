package com.example.wtf2.viewmodel;

import android.app.Application;

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

public class NoteViewModel extends AndroidViewModel {
    private final AppDatabase db;
    private final MutableLiveData<Note> currentNote = new MutableLiveData<>();
    private final MutableLiveData<List<Folder>> folders = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> statistics = new MutableLiveData<>("Символов: 0, Слов: 0, Строк: 0");
    private static final String DEFAULT_FOLDER_NAME = "Неотсортированные";
    private static final long DEFAULT_FOLDER_ID = 1L;
    private final MainViewModel mainViewModel;

    public NoteViewModel(@NonNull Application application, MainViewModel mainViewModel) {
        super(application);
        this.db = AppDatabase.getInstance(application);
        this.mainViewModel = mainViewModel;
    }

    public void setCurrentNote(Note note) {
        currentNote.setValue(note);
    }

    public LiveData<Note> getCurrentNote() {
        return currentNote;
    }

    public LiveData<List<Folder>> getFolders() {
        return folders;
    }

    public LiveData<String> getStatistics() {
        return statistics;
    }

    public void loadNote(int noteId, long initialFolderId) {
        new Thread(() -> {
            Note note;
            if (noteId != -1) {
                note = db.noteDao().getNoteById(noteId);
                if (note == null) {
                    android.util.Log.e("NoteViewModel", "Note with ID " + noteId + " not found");
                    note = new Note("", "", getCurrentDate(), initialFolderId != -1 ? initialFolderId : getDefaultFolderId());
                } else {
                    android.util.Log.d("NoteViewModel", "Loaded note with ID: " + note.getId());
                }
            } else {
                note = new Note("", "", getCurrentDate(), initialFolderId != -1 ? initialFolderId : getDefaultFolderId());
            }
            currentNote.postValue(note);
            updateStatistics(note.getContent());
            loadFolders();
        }).start();
    }

    public void loadNote(int noteId) {
        loadNote(noteId, -1);
    }

    private long getDefaultFolderId() {
        if (mainViewModel.getIsInFolder().getValue() != null && mainViewModel.getIsInFolder().getValue()) {
            return mainViewModel.getCurrentFolderId().getValue() != null
                    ? mainViewModel.getCurrentFolderId().getValue()
                    : DEFAULT_FOLDER_ID;
        }
        return DEFAULT_FOLDER_ID;
    }

    public void saveNote(String title, String content, long folderId) {
        Note note = currentNote.getValue();
        if (note == null) {
            android.util.Log.e("NoteViewModel", "Current note is null");
            note = new Note("", "", getCurrentDate(), getDefaultFolderId());
            currentNote.setValue(note);
        }
        android.util.Log.d("NoteViewModel", "Saving note with ID before update: " + note.getId());
        note.setTitle(title);
        note.setContent(content);
        note.setFolderId(folderId);
        note.setModifiedDate(getCurrentDate());
        Note finalNote = note;
        new Thread(() -> {
            android.util.Log.d("NoteViewModel", "Saving note with ID: " + finalNote.getId());
            if (finalNote.getId() == 0) {
                long newId = db.noteDao().insert(finalNote);
                finalNote.setId((int) newId);
                android.util.Log.d("NoteViewModel", "Inserted new note with ID: " + newId);
            } else {
                db.noteDao().updateNote(finalNote);
                android.util.Log.d("NoteViewModel", "Updated existing note with ID: " + finalNote.getId());
            }
            if (mainViewModel.getIsInFolder().getValue() != null && mainViewModel.getIsInFolder().getValue()) {
                mainViewModel.loadNotes(mainViewModel.getCurrentFolderId().getValue());
            } else {
                mainViewModel.loadNotes(null);
            }
        }).start();
    }

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

    private void loadFolders() {
        new Thread(() -> {
            List<Folder> folderList = db.folderDao().getAllFolders();
            folders.postValue(folderList);
            android.util.Log.d("NoteViewModel", "Loaded " + folderList.size() + " folders");
        }).start();
    }

    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        return sdf.format(new Date());
    }
}