package com.example.wtf2;


import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.concurrent.Executors;

@Database(entities = {Note.class, Folder.class}, version = 3, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCE;

    public abstract NoteDao noteDao();
    public abstract FolderDao folderDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "notes_database")
                            .fallbackToDestructiveMigration() // ✅ Удаляет старую базу и создаёт новую!
                            .setJournalMode(RoomDatabase.JournalMode.TRUNCATE) // ✅ SQLite принудительно сохраняет изменения
                            .addCallback(new RoomDatabase.Callback() {
                                @Override
                                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                                    super.onCreate(db);
                                    Log.d("DEBUG", "База данных создана!"); // ✅ Проверяем, создается ли база
                                    Cursor cursor = db.query("SELECT name FROM sqlite_master WHERE type='table'");
                                    while (cursor.moveToNext()) {
                                        Log.d("DEBUG", "Таблица в базе: " + cursor.getString(0));
                                    }
                                    cursor.close();
                                    Executors.newSingleThreadExecutor().execute(() -> {
                                        AppDatabase database = getInstance(context);
                                        Folder defaultFolder = new Folder("Неотсортированные"); // Создаём объект Folder
                                        defaultFolder.setColor("#FFFFFF"); // Устанавливаем цвет по умолчанию
                                        database.folderDao().insertFolder(defaultFolder); // Передаём объект Folder
                                        Log.d("DEBUG", "Папка 'Неотсортированное' добавлена при создании базы!");
                                    });
                                }
                            })
                            .addMigrations(MIGRATION_2_3) // Добавляем миграцию
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Добавляем столбец isPinned в таблицы notes и folders
            database.execSQL("ALTER TABLE folders ADD COLUMN color TEXT NOT NULL DEFAULT '#FFFFFF'");
        }
    };
}

