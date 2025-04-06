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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Абстрактный класс базы данных Room для хранения заметок и папок.
 */
@Database(entities = {Note.class, Folder.class}, version = 3, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static final String DATABASE_NAME = "notes_database";
    public static final int DATABASE_VERSION = 2;
    private static volatile AppDatabase INSTANCE;
    private static volatile boolean isInitialized = false;
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * Предоставляет доступ к DAO для заметок.
     */
    @NonNull
    public abstract NoteDao noteDao();

    /**
     * Предоставляет доступ к DAO для папок.
     */
    @NonNull
    public abstract FolderDao folderDao();

    /**
     * Получает или создает экземпляр базы данных.
     * @param context Контекст приложения
     * @return Экземпляр базы данных
     */
    public static AppDatabase getInstance(@NonNull Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, DATABASE_NAME)
                            .fallbackToDestructiveMigration() // TODO: Убрать в продакшене
                            .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
                            .addCallback(new RoomDatabase.Callback() {
                                @Override
                                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                                    super.onCreate(db);
                                    executor.execute(() -> {
                                        AppDatabase database = getInstance(context);
                                        populateTestData(database); // Заполнение тестовыми данными
                                        isInitialized = true;
                                    });
                                }

                                @Override
                                public void onOpen(@NonNull SupportSQLiteDatabase db) {
                                    super.onOpen(db);
                                    isInitialized = true;
                                }
                            })
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Проверяет, инициализирована ли база данных.
     */
    public static boolean isInitialized() {
        return isInitialized;
    }

    /**
     * Сбрасывает экземпляр базы данных (для использования при синхронизации с Google Drive).
     */
    public static void resetInstance() {
        synchronized (AppDatabase.class) {
            INSTANCE = null;
            isInitialized = false;
        }
    }

    /**
     * Заполняет базу тестовыми данными при первом создании.
     * @param database Экземпляр базы данных
     */
    private static void populateTestData(@NonNull AppDatabase database) {
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

        Folder[] folders = new Folder[folderNames.length];
        for (int i = 0; i < folderNames.length; i++) {
            Folder folder = new Folder(folderNames[i]);
            folder.setColor(colors[i]);
            folder.setPinned(i == 0 || random.nextBoolean());
            long folderId = database.folderDao().insertFolder(folder);
            folder.setId(folderId);
            folders[i] = folder;
        }

        for (int i = 0; i < noteTitles.length; i++) {
            int folderIndex = random.nextInt(folders.length);
            Note note = new Note(
                    noteTitles[i],
                    noteContents[i],
                    getCurrentDate(),
                    folders[folderIndex].getId()
            );
            note.setPinned(random.nextBoolean());
            database.noteDao().insert(note);
        }
    }

    /**
     * Возвращает текущую дату и время в формате "dd.MM.yyyy HH:mm".
     */
    @NonNull
    private static String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        return sdf.format(new Date());
    }
}