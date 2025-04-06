package com.example.wtf2.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * Модель данных для папки, хранимой в базе данных.
 */
@Entity(tableName = "folders")
public class Folder {
    private static final String DEFAULT_COLOR = "#FFFFFF";

    @PrimaryKey(autoGenerate = true)
    private long id;

    @NonNull
    private String name;

    @NonNull
    private String color;

    private boolean isPinned;

    private int noteCount;

    /**
     * Конструктор по умолчанию для Room.
     */
    public Folder() {
        this.name = "";
        this.color = DEFAULT_COLOR;
        this.isPinned = false;
        this.noteCount = 0;
    }

    /**
     * Конструктор для создания новой папки с именем.
     */
    @Ignore
    public Folder(@NonNull String name) {
        this.name = name;
        this.color = DEFAULT_COLOR;
        this.isPinned = false;
        this.noteCount = 0;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    @NonNull
    public String getName() { return name; }
    public void setName(@NonNull String name) { this.name = name != null ? name : ""; }

    @NonNull
    public String getColor() { return color; }
    public void setColor(@NonNull String color) { this.color = color != null ? color : DEFAULT_COLOR; }

    public boolean isPinned() { return isPinned; }
    public void setPinned(boolean pinned) { isPinned = pinned; }

    public int getNoteCount() { return noteCount; }
    public void setNoteCount(int noteCount) { this.noteCount = Math.max(0, noteCount); } // Предотвращаем отрицательное значение
}