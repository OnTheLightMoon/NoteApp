package com.example.wtf2;


import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class NoteEditor extends AppCompatActivity {

    private EditText noteTitle, noteContent;
    private TextView noteDate, noteStatistics;
    private Spinner folderSpinner;
    private ImageButton saveNoteBtn, backButton;

    private String currentDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_editor);

        // Инициализация UI-элементов
        noteTitle = findViewById(R.id.note_title);
        noteContent = findViewById(R.id.note_content);
        noteDate = findViewById(R.id.note_date);
        noteStatistics = findViewById(R.id.note_statistics);
        folderSpinner = findViewById(R.id.folder_spinner);
        saveNoteBtn = findViewById(R.id.save_note_btn);
        backButton = findViewById(R.id.back_button);

        loadFolders();

        // Установка текущей даты
        currentDate = getCurrentDate();
        noteDate.setText("Дата: " + currentDate);

        // Обновление статистики при изменении текста заметки
        noteContent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateStatistics(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Кнопка сохранения заметки
        saveNoteBtn.setOnClickListener(view -> saveNote());

        // Кнопка возврата
        backButton.setOnClickListener(view -> finish());

    }

    /**
     * Получает текущую дату в формате "dd.MM.yyyy HH:mm"
     */
    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        return sdf.format(new Date());
    }

    /**
     * Обновляет статистику заметки (символы, слова, строки)
     */
    private void updateStatistics(String text) {
        int charCount = text.length();
        int wordCount = text.trim().isEmpty() ? 0 : text.trim().split("\\s+").length;
        int lineCount = text.split("\n").length;

        noteStatistics.setText("Символов: " + charCount + ", Слов: " + wordCount + ", Строк: " + lineCount);
    }

    /**
     * Сохраняет заметку (здесь можно добавить логику для сохранения в БД)
     */
    private void saveNote() {
        String title = noteTitle.getText().toString().trim();
        String content = noteContent.getText().toString().trim();
        String folder = folderSpinner.getSelectedItem().toString(); // Получаем выбранную папку

        if (title.isEmpty() && content.isEmpty()) {
            finish(); // Если заметка пустая — просто закрываем
            return;
        }

        Note note = new Note(title, content, currentDate, folder);


        Executors.newSingleThreadExecutor().execute(() -> { // ✅ Выполняем в `background thread`
            AppDatabase db = AppDatabase.getInstance(this);
            db.noteDao().insert(note);
            Log.d("DEBUG", "Заметка сохранена в папку: " + folder);
            runOnUiThread(() -> {
                Toast.makeText(this, "Заметка сохранена!", Toast.LENGTH_SHORT).show();
                finish(); // ✅ Закрываем `NoteEditor` после сохранения
            });
        });
    }



    private void loadFolders() {
        Executors.newSingleThreadExecutor().execute(() -> { // ✅ Запускаем в фоновом потоке
            List<Folder> folders = AppDatabase.getInstance(this).folderDao().getAllFolders();

            runOnUiThread(() -> { // ✅ Обновляем UI в главном потоке
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item);
                for (Folder folder : folders) {
                    adapter.add(folder.getName());
                }
                folderSpinner.setAdapter(adapter);
                Log.d("DEBUG", "Папки загружены в NoteEditor: " + folders.size());
            });
        });
    }




}

