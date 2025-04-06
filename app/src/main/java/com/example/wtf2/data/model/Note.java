package com.example.wtf2.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * Модель данных для заметки, хранимой в базе данных.
 */
@Entity(tableName = "notes")
public class Note {
    @PrimaryKey(autoGenerate = true)
    private int id;

    @NonNull
    private String title;

    @NonNull
    private String content;

    private long folderId;

    @NonNull
    private String modifiedDate;

    private boolean isPinned;

    /**
     * Конструктор по умолчанию для Room.
     */
    public Note() {
        this.title = "";
        this.content = "";
        this.folderId = 0;
        this.modifiedDate = "";
        this.isPinned = false;
    }

    /**
     * Конструктор для создания новой заметки.
     */
    @Ignore
    public Note(@NonNull String title, @NonNull String content, @NonNull String modifiedDate, long folderId) {
        this.title = title;
        this.content = content;
        this.modifiedDate = modifiedDate;
        this.folderId = folderId;
        this.isPinned = false;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    @NonNull
    public String getTitle() { return title; }
    public void setTitle(@NonNull String title) { this.title = title != null ? title : ""; }

    @NonNull
    public String getContent() { return content; }
    public void setContent(@NonNull String content) { this.content = content != null ? content : ""; }

    public long getFolderId() { return folderId; }
    public void setFolderId(long folderId) { this.folderId = folderId; }

    @NonNull
    public String getModifiedDate() { return modifiedDate; }
    public void setModifiedDate(@NonNull String modifiedDate) { this.modifiedDate = modifiedDate != null ? modifiedDate : ""; }

    public boolean isPinned() { return isPinned; }
    public void setPinned(boolean pinned) { isPinned = pinned; }
}