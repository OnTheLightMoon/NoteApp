package com.example.wtf2;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "folders")
public class Folder {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String name;
    private int noteCount;
    private boolean isPinned;
    @NonNull // Добавляем аннотацию, чтобы поле было NOT NULL
    private String color;

    public Folder(String name) {
        this.name = name;
        this.isPinned = false;
        this.color = "#FFFFFF"; // Значение по умолчанию
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getNoteCount() { return noteCount; }
    public void setNoteCount(int noteCount) { this.noteCount = noteCount; }
    public boolean isPinned() { return isPinned; }
    public void setPinned(boolean pinned) { isPinned = pinned; }
    @NonNull
    public String getColor() { return color; }
    public void setColor(@NonNull String color) { this.color = color; }
}