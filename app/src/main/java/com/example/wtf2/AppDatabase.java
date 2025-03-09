package com.example.wtf2;


import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.concurrent.Executors;

@Database(entities = {Note.class, Folder.class}, version = 12, exportSchema = false)
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
                            .setJournalMode(JournalMode.TRUNCATE) // ✅ SQLite принудительно сохраняет изменения
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
                                        database.folderDao().insertFolder("Неотсортированное");
                                        Log.d("DEBUG", "Папка 'Неотсортированное' добавлена при создании базы!");
                                    });
                                }
                            })
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}

