package com.example.wtf2;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "notes")
public class Note {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String title;
    private String content;
    private String date;
    private String folder;
    private boolean isPinned;

    // Основной конструктор для Room (содержит все поля)
    public Note(int id, String title, String content, String date, String folder, boolean isPinned) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.date = date;
        this.folder = folder;
        this.isPinned = isPinned;
    }

    // Дополнительный конструктор для создания новых заметок (игнорируется Room)
    @Ignore
    public Note(String title, String content, String date, String folder) {
        this(0, title, content, date, folder, false); // id = 0 (автогенерация), isPinned = false
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getFolder() { return folder; }
    public void setFolder(String folder) { this.folder = folder; }
    public boolean isPinned() { return isPinned; }
    public void setPinned(boolean pinned) { isPinned = pinned; }
}