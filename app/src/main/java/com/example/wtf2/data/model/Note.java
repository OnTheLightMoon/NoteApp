package com.example.wtf2.data.model;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "notes")
public class Note {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String title;
    private String content;
    private long folderId; // Изменяем с int на long
    private String modifiedDate;
    private boolean isPinned;

    public Note() {
        this.id = 0;
        this.title = "";
        this.content = "";
        this.folderId = 0;
        this.modifiedDate = "";
        this.isPinned = false;
    }

    @Ignore
    public Note(int id, String title, String content, long folderId, String modifiedDate, boolean isPinned) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.folderId = folderId;
        this.modifiedDate = modifiedDate;
        this.isPinned = isPinned;
    }

    @Ignore
    public Note(String title, String content, String modifiedDate, long folderId) {
        this.id = 0;
        this.title = title;
        this.content = content;
        this.modifiedDate = modifiedDate;
        this.folderId = folderId;
        this.isPinned = false;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public long getFolderId() { return folderId; } // Изменяем с int на long
    public void setFolderId(long folderId) { this.folderId = folderId; } // Изменяем с int на long
    public String getModifiedDate() { return modifiedDate; }
    public void setModifiedDate(String modifiedDate) { this.modifiedDate = modifiedDate; }
    public boolean isPinned() { return isPinned; }
    public void setPinned(boolean pinned) { isPinned = pinned; }
}