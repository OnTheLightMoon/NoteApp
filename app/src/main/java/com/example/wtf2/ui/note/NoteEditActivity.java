package com.example.wtf2.ui.note;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.wtf2.R;
import com.example.wtf2.data.model.Folder;
import com.example.wtf2.data.model.Note;
import com.example.wtf2.viewmodel.MainViewModel;
import com.example.wtf2.viewmodel.NoteViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Активность для создания или редактирования заметки.
 */
public class NoteEditActivity extends AppCompatActivity {
    private static final String TAG = "NoteEditActivity";
    private static final String DEFAULT_FOLDER_NAME = "Неотсортированные";
    private NoteViewModel noteViewModel;
    private MainViewModel mainViewModel;
    private ArrayAdapter<String> folderAdapter;
    private List<Folder> folderList = new ArrayList<>();
    private long initialFolderId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_editor);

        initializeViewModels();
        initializeViews();

        initialFolderId = getIntent().getLongExtra("FOLDER_ID", -1);
        int noteId = getIntent().getIntExtra("NOTE_ID", -1);
        Log.d(TAG, "Started with NOTE_ID: " + noteId + ", FOLDER_ID: " + initialFolderId);

        noteViewModel.loadNote(noteId, initialFolderId);
    }

    /**
     * Инициализирует ViewModel'ы для активности.
     */
    private void initializeViewModels() {
        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);
        noteViewModel = new ViewModelProvider(this, new ViewModelProvider.Factory() {
            @NonNull
            @Override
            public <T extends androidx.lifecycle.ViewModel> T create(@NonNull Class<T> modelClass) {
                return modelClass.cast(new NoteViewModel(getApplication(), mainViewModel));
            }
        }).get(NoteViewModel.class);
    }

    /**
     * Инициализирует UI-компоненты и настраивает их поведение.
     */
    private void initializeViews() {
        EditText titleEdit = findViewById(R.id.note_title);
        EditText contentEdit = findViewById(R.id.note_content);
        TextView dateText = findViewById(R.id.note_date);
        TextView statsText = findViewById(R.id.note_statistics);
        Spinner folderSpinner = findViewById(R.id.folder_spinner);
        ImageButton saveButton = findViewById(R.id.save_note_btn);
        ImageButton backButton = findViewById(R.id.back_button);

        folderAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>());
        folderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        folderSpinner.setAdapter(folderAdapter);

        noteViewModel.getFolders().observe(this, folders -> {
            if (folders != null && !folders.isEmpty()) {
                folderList = folders;
                folderAdapter.clear();
                folderAdapter.addAll(folders.stream().map(Folder::getName).toList());
                folderAdapter.notifyDataSetChanged();
                setInitialFolderSelection(folderSpinner, folders);
            }
        });

        noteViewModel.getCurrentNote().observe(this, note -> {
            if (note != null) {
                titleEdit.setText(note.getTitle());
                contentEdit.setText(note.getContent());
                dateText.setText("Дата: " + (note.getModifiedDate() != null ? note.getModifiedDate() : ""));
                Log.d(TAG, "Loaded note: ID=" + note.getId() + ", FolderId=" + note.getFolderId());
            }
        });

        noteViewModel.getStatistics().observe(this, statsText::setText);

        contentEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                noteViewModel.updateStatistics(s.toString());
            }
        });

        saveButton.setOnClickListener(v -> {
            String title = titleEdit.getText().toString().trim();
            String content = contentEdit.getText().toString().trim();
            if (title.isEmpty() && content.isEmpty()) {
                Toast.makeText(this, "Заметка должна содержать заголовок или текст", Toast.LENGTH_SHORT).show();
                return;
            }
            long folderId = folderList.get(folderSpinner.getSelectedItemPosition()).getId();
            noteViewModel.saveNote(title, content, folderId);
            finish();
        });

        backButton.setOnClickListener(v -> finish());
    }

    /**
     * Устанавливает начальное значение спиннера папок.
     */
    private void setInitialFolderSelection(Spinner folderSpinner, List<Folder> folders) {
        int defaultPosition = getFolderPositionByName(folders, DEFAULT_FOLDER_NAME);
        if (initialFolderId != -1) {
            int position = getFolderPositionById(folders, initialFolderId);
            folderSpinner.setSelection(position != -1 ? position : defaultPosition);
        } else {
            Note currentNote = noteViewModel.getCurrentNote().getValue();
            if (currentNote != null && currentNote.getId() != 0) {
                int position = getFolderPositionById(folders, currentNote.getFolderId());
                folderSpinner.setSelection(position != -1 ? position : defaultPosition);
            } else {
                folderSpinner.setSelection(defaultPosition);
            }
        }
        Log.d(TAG, "Selected folder position: " + folderSpinner.getSelectedItemPosition());
    }

    private int getFolderPositionById(List<Folder> folders, long id) {
        for (int i = 0; i < folders.size(); i++) {
            if (folders.get(i).getId() == id) return i;
        }
        return -1;
    }

    private int getFolderPositionByName(List<Folder> folders, String name) {
        for (int i = 0; i < folders.size(); i++) {
            if (folders.get(i).getName().equals(name)) return i;
        }
        return 0;
    }
}