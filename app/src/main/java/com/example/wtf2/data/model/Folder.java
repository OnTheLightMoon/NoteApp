package com.example.wtf2.data.model;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "folders")
public class Folder {
    @PrimaryKey(autoGenerate = true)
    private long id; // Изменяем с int на long
    private String name;
    private String color;
    private boolean isPinned;
    private int noteCount;

    public Folder() {
        this.id = 0;
        this.name = "";
        this.color = "#FFFFFF";
        this.isPinned = false;
        this.noteCount = 0;
    }

    @Ignore
    public Folder(String name) {
        this.id = 0;
        this.name = name;
        this.color = "#FFFFFF";
        this.isPinned = false;
        this.noteCount = 0;
    }

    public long getId() { return id; } // Изменяем с int на long
    public void setId(long id) { this.id = id; } // Изменяем с int на long
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public boolean isPinned() { return isPinned; }
    public void setPinned(boolean pinned) { isPinned = pinned; }
    public int getNoteCount() { return noteCount; }
    public void setNoteCount(int noteCount) { this.noteCount = noteCount; }
}