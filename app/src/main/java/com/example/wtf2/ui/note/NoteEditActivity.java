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

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.wtf2.R;
import com.example.wtf2.data.model.Folder;
import com.example.wtf2.data.model.Note;
import com.example.wtf2.viewmodel.MainViewModel;
import com.example.wtf2.viewmodel.NoteViewModel;

import java.util.ArrayList;
import java.util.List;

public class NoteEditActivity extends AppCompatActivity {
    private NoteViewModel noteViewModel;
    private MainViewModel mainViewModel;
    private ArrayAdapter<String> folderAdapter;
    private List<Folder> folderList;
    private long initialFolderId = -1; // Для хранения folderId из Intent

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_editor);

        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);
        noteViewModel = new ViewModelProvider(this, new ViewModelProvider.AndroidViewModelFactory(getApplication()) {
            @Override
            public <T extends androidx.lifecycle.ViewModel> T create(Class<T> modelClass) {
                return (T) new NoteViewModel(getApplication(), mainViewModel);
            }
        }).get(NoteViewModel.class);

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

        // Получаем folderId из Intent, если он передан
        initialFolderId = getIntent().getLongExtra("FOLDER_ID", -1);
        Log.d("NoteEditActivity", "Received FOLDER_ID: " + initialFolderId);

        noteViewModel.getFolders().observe(this, folders -> {
            if (folders != null && !folders.isEmpty()) {
                folderList = folders;
                List<String> folderNames = new ArrayList<>();
                for (Folder folder : folders) {
                    folderNames.add(folder.getName());
                }
                folderAdapter.clear();
                folderAdapter.addAll(folderNames);
                folderAdapter.notifyDataSetChanged();
                Log.d("NoteEditActivity", "Folder spinner updated with " + folderNames.size() + " items");

                // Устанавливаем начальное значение спиннера
                if (initialFolderId != -1) {
                    // Если передан folderId из Intent, выбираем эту папку
                    int position = -1;
                    for (int i = 0; i < folders.size(); i++) {
                        if (folders.get(i).getId() == initialFolderId) {
                            position = i;
                            break;
                        }
                    }
                    if (position >= 0) {
                        folderSpinner.setSelection(position);
                        Log.d("NoteEditActivity", "Selected folder from FOLDER_ID: " + initialFolderId + " at position " + position);
                    } else {
                        setDefaultFolderSelection(folders, folderSpinner);
                    }
                } else {
                    // Если редактируем существующую заметку, выбираем её папку
                    Note currentNote = noteViewModel.getCurrentNote().getValue();
                    if (currentNote != null && currentNote.getId() != 0) {
                        int position = -1;
                        for (int i = 0; i < folders.size(); i++) {
                            if (folders.get(i).getId() == currentNote.getFolderId()) {
                                position = i;
                                break;
                            }
                        }
                        if (position >= 0) {
                            folderSpinner.setSelection(position);
                            Log.d("NoteEditActivity", "Selected folder from note: " + currentNote.getFolderId() + " at position " + position);
                        } else {
                            setDefaultFolderSelection(folders, folderSpinner);
                        }
                    } else {
                        // Иначе выбираем "Неотсортированные"
                        setDefaultFolderSelection(folders, folderSpinner);
                    }
                }
            } else {
                Log.w("NoteEditActivity", "Folder list is empty or null");
            }
        });

        int noteId = getIntent().getIntExtra("NOTE_ID", -1);
        Log.d("NoteEditActivity", "Received NOTE_ID: " + noteId);
        noteViewModel.loadNote(noteId);

        noteViewModel.getCurrentNote().observe(this, note -> {
            if (note != null) {
                titleEdit.setText(note.getTitle());
                contentEdit.setText(note.getContent());
                dateText.setText("Дата: " + (note.getModifiedDate() != null ? note.getModifiedDate() : ""));
                Log.d("NoteEditActivity", "Displaying note: ID=" + note.getId() + ", FolderId=" + note.getFolderId() + ", Date=" + note.getModifiedDate());
            } else {
                Log.w("NoteEditActivity", "Current note is null");
            }
        });

        noteViewModel.getStatistics().observe(this, statsText::setText);

        TextWatcher statsWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                noteViewModel.updateStatistics(contentEdit.getText().toString());
            }
        };
        contentEdit.addTextChangedListener(statsWatcher);

        saveButton.setOnClickListener(v -> {
            String title = titleEdit.getText().toString().trim();
            String content = contentEdit.getText().toString().trim();
            int selectedPosition = folderSpinner.getSelectedItemPosition();
            long folderId = selectedPosition >= 0 && folderList != null
                    ? folderList.get(selectedPosition).getId()
                    : 1L;
            if (title.isEmpty() && content.isEmpty()) {
                Toast.makeText(this, "Заметка должна содержать заголовок или текст", Toast.LENGTH_SHORT).show();
                return;
            }
            noteViewModel.saveNote(title, content, folderId);
            finish();
        });

        backButton.setOnClickListener(v -> finish());
    }

    private void setDefaultFolderSelection(List<Folder> folders, Spinner folderSpinner) {
        for (int i = 0; i < folders.size(); i++) {
            if (folders.get(i).getName().equals("Неотсортированные")) {
                folderSpinner.setSelection(i);
                Log.d("NoteEditActivity", "Selected default folder 'Неотсортированные' at position " + i);
                break;
            }
        }
    }
}