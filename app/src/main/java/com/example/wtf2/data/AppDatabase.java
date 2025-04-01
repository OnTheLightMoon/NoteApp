package com.example.wtf2.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.wtf2.data.dao.FolderDao;
import com.example.wtf2.data.dao.NoteDao;
import com.example.wtf2.data.model.Folder;
import com.example.wtf2.data.model.Note;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.Executors;

@Database(entities = {Note.class, Folder.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCE;
    private static volatile boolean isInitialized = false;

    public abstract NoteDao noteDao();
    public abstract FolderDao folderDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "notes_database")
                            .fallbackToDestructiveMigration()
                            .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
                            .addCallback(new RoomDatabase.Callback() {
                                @Override
                                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                                    super.onCreate(db);
                                    Executors.newSingleThreadExecutor().execute(() -> {
                                        AppDatabase database = getInstance(context);
                                        populateTestData(database);
                                        isInitialized = true;
                                    });
                                }
                            })
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    public static boolean isInitialized() {
        return isInitialized;
    }

    private static void populateTestData(AppDatabase database) {
        Random random = new Random();
        String[] folderNames = {"Неотсортированные", "Работа", "Личное", "Идеи", "Покупки"};
        String[] colors = {"#FFFFFF", "#FFCDD2", "#C8E6C9", "#BBDEFB", "#FFECB3"};
        String[] noteTitles = {"Звонок клиенту", "Встреча с друзьями", "Искры гениальности", "Список продуктов"};
        String[] noteContents = {
                "Позвонить клиенту в 14:00 по поводу проекта.",
                "Договориться о встрече в кафе в субботу.",
                "Придумать новый дизайн приложения.",
                "Купить молоко, хлеб, яйца."
        };

        // Создаем папки и сохраняем их ID
        Folder[] folders = new Folder[folderNames.length];
        for (int i = 0; i < folderNames.length; i++) {
            Folder folder = new Folder(folderNames[i]);
            folder.setColor(colors[i]);
            folder.setPinned(i == 0 || random.nextBoolean());
            long folderId = database.folderDao().insertFolder(folder);
            folder.setId(folderId);
            folders[i] = folder;
        }

        // Создаем заметки с привязкой по folderId
        for (int i = 0; i < noteTitles.length; i++) {
            int folderIndex = random.nextInt(folders.length);
            Note note = new Note(
                    noteTitles[i],
                    noteContents[i],
                    getCurrentDate(),
                    folders[folderIndex].getId() // Теперь folderId — это long
            );
            note.setPinned(random.nextBoolean());
            database.noteDao().insert(note);
        }
    }

    private static String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        return sdf.format(new Date());
    }
}