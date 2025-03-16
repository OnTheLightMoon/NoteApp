package com.example.wtf2;

import android.content.Intent;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class NoteEditor extends AppCompatActivity {

    private EditText noteTitle, noteContent;
    private TextView noteDate, noteStatistics;
    private Spinner folderSpinner;
    private AppDatabase db;
    private Note currentNote;
    private List<String> folderNames;
    private int noteId = -1; // Отдельное поле для ID
    private String currentDate;
    private static final String DEFAULT_FOLDER = "Неотсортированные";

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
        ImageButton backButton = findViewById(R.id.back_button);
        ImageButton saveButton = findViewById(R.id.save_note_btn);

        db = AppDatabase.getInstance(this);
        folderNames = new ArrayList<>();

        // Загружаем данные заметки из Intent, если они есть
        Intent intent = getIntent();
        if (intent.hasExtra("NOTE_ID")) {
            noteId = intent.getIntExtra("NOTE_ID", -1);
            String title = intent.getStringExtra("NOTE_TITLE");
            String content = intent.getStringExtra("NOTE_CONTENT");
            String date = intent.getStringExtra("NOTE_DATE");
            String folder = intent.getStringExtra("NOTE_FOLDER");
            currentNote = new Note(title, content, date, folder); // Используем конструктор

            noteTitle.setText(currentNote.getTitle());
            noteContent.setText(currentNote.getContent());
            noteDate.setText("Дата: " + currentNote.getDate());
        } else {
            currentNote = null;
            noteDate.setText("Дата: " + getCurrentDate());
        }

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
        saveButton.setOnClickListener(v -> saveNote());

        // Кнопка возврата
        backButton.setOnClickListener(v -> finish());
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
     * Сохраняет заметку
     */
    private void saveNote() {
        String title = noteTitle.getText().toString().trim();
        String content = noteContent.getText().toString().trim();
        String folder = folderSpinner.getSelectedItem() != null ? folderSpinner.getSelectedItem().toString() : DEFAULT_FOLDER;

        if (content.isEmpty() && title.isEmpty()) {
            Toast.makeText(this, "Заметка не может быть пустой", Toast.LENGTH_SHORT).show();
            return;
        }

        // Создаем объект Note вне лямбда-выражения
        Note noteToSave;
        if (currentNote == null) {
            // Новая заметка
            noteToSave = new Note(title, content, getCurrentDate(), folder); // Используем @Ignore конструктор
        } else {
            // Обновление существующей заметки
            noteToSave = new Note(noteId, title, content, getCurrentDate(), folder, currentNote.isPinned()); // Полный конструктор
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            if (currentNote == null) {
                db.noteDao().insert(noteToSave);
            } else {
                db.noteDao().update(noteToSave);
            }

            runOnUiThread(() -> {
                Toast.makeText(this, "Заметка сохранена", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            });
        });
    }

    private void loadFolders() {
        Executors.newSingleThreadExecutor().execute(() -> { // Запускаем в фоновом потоке
            List<Folder> folders = db.folderDao().getAllFolders();
            folderNames.clear();
            // Не добавляем "Без папки", только реальные папки
            for (Folder folder : folders) {
                folderNames.add(folder.getName());
            }

            // Убеждаемся, что "Неотсортированные" есть в списке
            if (!folderNames.contains(DEFAULT_FOLDER)) {
                folderNames.add(DEFAULT_FOLDER);
            }

            runOnUiThread(() -> { // Обновляем UI в главном потоке
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, folderNames);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                folderSpinner.setAdapter(adapter);

                // Устанавливаем текущую папку или "Неотсортированные" по умолчанию
                if (currentNote != null && currentNote.getFolder() != null) {
                    int position = folderNames.indexOf(currentNote.getFolder());
                    if (position >= 0) {
                        folderSpinner.setSelection(position);
                    } else {
                        folderSpinner.setSelection(folderNames.indexOf(DEFAULT_FOLDER));
                    }
                } else {
                    folderSpinner.setSelection(folderNames.indexOf(DEFAULT_FOLDER));
                }
            });
        });
    }
}